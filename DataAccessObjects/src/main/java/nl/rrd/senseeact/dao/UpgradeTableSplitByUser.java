package nl.rrd.senseeact.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.rrd.utils.exception.DatabaseException;
import org.slf4j.Logger;

import nl.rrd.utils.AppComponents;

/**
 * This class can be used to split a table into separate tables per user. It
 * can be called from {@link
 * DatabaseTableDef#upgradeTable(int, Database, String)
 * DatabaseTableDef.upgradeTable()} if the physical table name is the same as
 * the logical table name.
 * 
 * @author Dennis Hofs (RRD)
 */
public class UpgradeTableSplitByUser {
	private static final int BATCH_SIZE = 10000;
	
	/**
	 * Upgrades the specified logical table by splitting it into separate
	 * physical tables per user.
	 * 
	 * @param db the database
	 * @param tableDef the table definition
	 * @throws DatabaseException if a database error occurs
	 */
	public static void upgradeSplit(Database db, DatabaseTableDef<?> tableDef)
			throws DatabaseException {
		if (!db.selectDbTables().contains(tableDef.getName()))
			return;
		Logger logger = AppComponents.getLogger(Database.LOGTAG);
		logger.info(String.format("Start upgrade table %s: split by user",
				tableDef.getName()));
		try {
			// add index on user and utcTime to speed up delete by time
			db.createIndex(tableDef.getName(), new DatabaseIndex("userTime",
					"user", "utcTime"));
			logger.info("Created index \"userTime\"");
		} catch (DatabaseException ex) {
			logger.info("Can't create index \"userTime\", probably already exists: " +
					ex.getMessage());
		}
		// select without filter by user so the select query runs faster
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("utcTime", true)
		};
		while (true) {
			List<Map<String,?>> maps = db.selectMaps(tableDef.getName(), null,
					null, BATCH_SIZE, sort);
			if (maps.isEmpty()) {
				db.dropCachedDbTable(tableDef.getName());
				logger.info(String.format(
						"Finished upgrade table %s: split by user",
						tableDef.getName()));
				return;
			}
			logger.info(String.format(
					"Upgrade table %s: selected batch of %s records",
					tableDef.getName(), maps.size()));
			while (!maps.isEmpty()) {
				upgradeSplitUserBatch(db, tableDef, maps);
			}
		}
	}
	
	/**
	 * This method takes the first batch of records ordered by time from the
	 * main table. It gets the user of the first record and then gets all
	 * records for that user. They are moved from the main table to the user
	 * table. The moved records are removed from "batchMaps".
	 * 
	 * @param db the database
	 * @param tableDef the database definition
	 * @param batchMaps the batch of records
	 * @throws DatabaseException if a database error occurs
	 */
	private static void upgradeSplitUserBatch(Database db,
			DatabaseTableDef<?> tableDef, List<Map<String,?>> batchMaps)
			throws DatabaseException {
		Logger logger = AppComponents.getLogger(Database.LOGTAG);
		String user = (String)batchMaps.get(0).get("user");
		List<Map<String,Object>> userMaps = new ArrayList<>();
		Iterator<Map<String,?>> batchIt = batchMaps.iterator();
		while (batchIt.hasNext()) {
			Map<String,?> batchMap = batchIt.next();
			String batchUser = (String)batchMap.get("user");
			if (batchUser.equals(user)) {
				userMaps.add(new LinkedHashMap<>(batchMap));
				batchIt.remove();
			}
		}
		long startTime = (Long)userMaps.get(0).get("utcTime");
		long endTime = (Long)userMaps.get(userMaps.size() - 1).get("utcTime");
		String startLocal = (String)userMaps.get(0).get("localTime");
		String endLocal = (String)userMaps.get(userMaps.size() - 1).get(
				"localTime");
		List<String> startEndIds = new ArrayList<>();
		for (Map<String,?> map : userMaps) {
			long time = (Long)map.get("utcTime");
			if (time > startTime)
				break;
			startEndIds.add((String)map.get("id"));
		}
		if (startTime != endTime) {
			int endIdPos = startEndIds.size();
			for (int i = userMaps.size() - 1; i >= 0; i--) {
				Map<String,?> map = userMaps.get(i);
				long time = (Long)map.get("utcTime");
				if (time < endTime)
					break;
				startEndIds.add(endIdPos, (String)map.get("id"));
			}
		}
		String userTable = db.getSplitUserTable(tableDef, user);
		logger.info(String.format(
				"Upgrade table %s: move %s records to user table %s for user %s: %s - %s",
				tableDef.getName(), userMaps.size(), userTable, user,
				startLocal, endLocal));
		DatabaseCriteria deleteIdCriteria = null;
		if (!startEndIds.isEmpty()) {
			DatabaseCriteria[] ors =
					new DatabaseCriteria[startEndIds.size()];
			for (int i = 0; i < ors.length; i++) {
				ors[i] = new DatabaseCriteria.Equal(
						"id", startEndIds.get(i));
			}
			deleteIdCriteria = new DatabaseCriteria.Or(ors);
		}
		DatabaseCriteria deleteTimeCriteriaWithoutUser = null;
		DatabaseCriteria deleteTimeCriteriaWithUser = null;
		if (startTime != endTime) {
			deleteTimeCriteriaWithoutUser = new DatabaseCriteria.And(
				new DatabaseCriteria.GreaterThan("utcTime", startTime),
				new DatabaseCriteria.LessThan("utcTime", endTime)
			);
			deleteTimeCriteriaWithUser = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("user", user),
				new DatabaseCriteria.GreaterThan("utcTime", startTime),
				new DatabaseCriteria.LessThan("utcTime", endTime)
			);
		}
		if (deleteIdCriteria != null)
			db.delete(userTable, null, deleteIdCriteria);
		if (deleteTimeCriteriaWithoutUser != null)
			db.delete(userTable, null, deleteTimeCriteriaWithoutUser);
		db.insertMaps(userTable, userMaps);
		if (deleteIdCriteria != null)
			db.delete(tableDef.getName(), null, deleteIdCriteria);
		if (deleteTimeCriteriaWithUser != null)
			db.delete(tableDef.getName(), null, deleteTimeCriteriaWithUser);
	}
}
