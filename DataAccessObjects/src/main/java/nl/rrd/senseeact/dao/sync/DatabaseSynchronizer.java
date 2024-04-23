package nl.rrd.senseeact.dao.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rrd.senseeact.dao.*;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.CurrentIterator;
import nl.rrd.utils.exception.DatabaseException;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * This class can synchronize two databases. It can read database actions that
 * should be written to a remote database, and it can write database actions
 * from a remote database. By default it synchronizes all data for the specified
 * user and resource data (that does not belong to a user) in the database
 * except reserved tables (whose names start with an underscore). You can limit
 * this by table name and time range when you set properties on this class. When
 * reading database actions, you will get only the actions that match the
 * properties. When writing database actions, the synchronizer will throw an
 * exception if an action does not match the properties.
 *
 * <p>Writing database actions for resource tables is by default not allowed.
 * This can be controlled with {@link #setAllowWriteResourceTables(boolean)
 * setAllowWriteResourceTables()} and {@link
 * #setIncludeWriteResourceTables(List) setIncludeWriteResourceTables()}.</p>
 *
 * @author Dennis Hofs (RRD)
 */
public class DatabaseSynchronizer {
	private List<String> includeTables = new ArrayList<>();
	private List<String> excludeTables = new ArrayList<>();
	private List<SyncTimeRangeRestriction> timeRangeRestrictions =
			new ArrayList<>();
	private boolean allowWriteResourceTables = false;
	private List<String> includeWriteResourceTables = new ArrayList<>();
	private String user;

	public DatabaseSynchronizer(String user) {
		this.user = user;
	}

	/**
	 * Returns the names of the tables that should be synchronised. If this is
	 * null or an empty list, it will synchronise all data tables, so this
	 * property limits that set. Reserved tables (whose name start with an
	 * underscore) will never be synchronised.
	 * 
	 * <p>If any exclude tables are set, they will never be synchronised, even
	 * if they appear in the include tables.</p>
	 *
	 * @return the table names or null or an empty list (default)
	 */
	public List<String> getIncludeTables() {
		return includeTables;
	}

	/**
	 * Sets the names of the tables that should be synchronised. If this is null
	 * or an empty list, it will synchronise all data tables, so this property
	 * limits that set. Reserved tables (whose name start with an underscore)
	 * will never be synchronised.
	 * 
	 * <p>If any exclude tables are set, they will never be synchronised, even
	 * if they appear in the include tables.</p>
	 *
	 * @param tables the table names or null or an empty list (default)
	 */
	public void setIncludeTables(List<String> tables) {
		this.includeTables = tables;
	}
	
	/**
	 * Returns the names of the tables that should not be synchronised. If this
	 * is null or an empty list, it will synchronise all data tables or all
	 * include tables. Reserved tables (whose name start with an underscore)
	 * will never be synchronised.
	 * 
	 * <p>Exclude tables are never synchronised, even if they appear in the
	 * include tables.</p>
	 * 
	 * @return the table names or null or an empty list (default)
	 */
	public List<String> getExcludeTables() {
		return excludeTables;
	}
	
	/**
	 * Sets the names of the tables that should not be synchronised. If this is
	 * null or an empty list, it will synchronise all data tables or all
	 * include tables. Reserved tables (whose name start with an underscore)
	 * will never be synchronised.
	 * 
	 * <p>Exclude tables are never synchronised, even if they appear in the
	 * include tables.</p>
	 * 
	 * @param tables the table names or null or an empty list (default)
	 */
	public void setExcludeTables(List<String> tables) {
		this.excludeTables = tables;
	}
	
	/**
	 * Returns the time range restrictions. For more information see {@link
	 * #setTimeRangeRestrictions(List) setTimeRangeRestrictions()}.
	 * 
	 * @return the time range restrictions or null or an empty list (default)
	 */
	public List<SyncTimeRangeRestriction> getTimeRangeRestrictions() {
		return timeRangeRestrictions;
	}

	/**
	 * Sets the time range restrictions. This is meant to limit database actions
	 * that are read. It has no influence on writing actions. You can write
	 * database actions that are outside the range.
	 * 
	 * <p>Be aware of interference with the sync progress if in an earlier case
	 * you synchronized a later time range. For example one time you
	 * synchronize actions with time range [2016-03-02 00:00:00, 
	 * 2016-03-03 00:00:00]. The sync progress could then be
	 * 2016-03-02 18:30:00, but data before 2016-03-02 was not synchronized.
	 * If next time you synchronize actions with time range
	 * [2016-03-01 00:00:00, 2016-03-02 00:00:00], you won't receive anything,
	 * because the sync progress has a later time.</p>
	 * 
	 * @param timeRangeRestrictions the time range restrictions or null or an
	 * empty list (default)
	 */
	public void setTimeRangeRestrictions(
			List<SyncTimeRangeRestriction> timeRangeRestrictions) {
		this.timeRangeRestrictions = timeRangeRestrictions;
	}

	/**
	 * Returns whether the user is allowed to write to any resource tables. If
	 * this is allowed, the actual tables can still be restricted by {@link
	 * #getIncludeWriteResourceTables() getIncludeWriteResourceTables()}. The
	 * default is false.
	 *
	 * @return true if the user is allowed to write to (some) resource tables,
	 * false if the user is not allowed to write to any resource table
	 */
	public boolean isAllowWriteResourceTables() {
		return allowWriteResourceTables;
	}

	/**
	 * Sets whether the user is allowed to write to any resource tables. If this
	 * is allowed, the actual tables can still be restricted with {@link
	 * #setIncludeWriteResourceTables(List) setIncludeWriteResourceTables()}.
	 * The default is false.
	 *
	 * @param allowWriteResourceTables true if the user is allowed to write to
	 * (some) resource tables, false if the user is not allowed to write to any
	 * resource table
	 */
	public void setAllowWriteResourceTables(boolean allowWriteResourceTables) {
		this.allowWriteResourceTables = allowWriteResourceTables;
	}

	/**
	 * If the user is allowed to write to resource tables (see {@link
	 * #isAllowWriteResourceTables() isAllowWriteResourceTables()}), then this
	 * method may restrict the tables that the user can write to. If this method
	 * returns an empty list, the user can write to any resource table.
	 *
	 * @return the resource tables that the user can write to or an empty list
	 */
	public List<String> getIncludeWriteResourceTables() {
		return includeWriteResourceTables;
	}

	/**
	 * If the user is allowed to write to resource tables (see {@link
	 * #setAllowWriteResourceTables(boolean) setAllowWriteResourceTables()}),
	 * then this method may restrict the tables that the user can write to. If
	 * this method returns an empty list, the user can write to any resource
	 * table.
	 *
	 * @param includeWriteResourceTables the resource tables that the user can
	 * write to or an empty list
	 */
	public void setIncludeWriteResourceTables(
			List<String> includeWriteResourceTables) {
		this.includeWriteResourceTables = includeWriteResourceTables;
	}

	private boolean isWriteResourceTableAllowed(String table) {
		return allowWriteResourceTables &&
				(includeWriteResourceTables.isEmpty() ||
				includeWriteResourceTables.contains(table));
	}

	/**
	 * Returns the user whose data should be synchronized.
	 *
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Returns whether the specified table should be included. It returns false
	 * in the following cases:
	 * 
	 * <p><ul>
	 * <li>if it's a reserved table</li>
	 * <li>if the table appears in the exclude tables</li>
	 * <li>if there are include tables and the specified table is not one of
	 * them</li>
	 * </ul></p>
	 * 
	 * @param table the table name
	 * @return true if the table should be included, false otherwise
	 * @throws DatabaseException if a database error occurs
	 */
	private boolean isTableIncluded(String table) throws DatabaseException {
		if (table.startsWith("_"))
			return false;
		if (excludeTables != null && excludeTables.contains(table))
			return false;
		if (includeTables != null && !includeTables.isEmpty() &&
				!includeTables.contains(table)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Returns whether the specified table contains user data. This is true if
	 * the table has a field "user".
	 * 
	 * @param database the database
	 * @param table the table name
	 * @return true if the table contains user data, false otherwise
	 * @throws DatabaseException if a database error occurs
	 */
	private boolean isUserTable(Database database, String table)
			throws DatabaseException {
		DatabaseCache cache = DatabaseCache.getInstance();
		List<String> fields = cache.getTableFields(database, table);
		return fields.contains("user");
	}

	/**
	 * Returns the synchronisation progress, which defines what data has already
	 * been synchronised to this database, given the properties of this
	 * synchroniser. It returns zero or one progress object for each table that
	 * is synchronised. The result of this method can be passed to a subsequent
	 * call of {@link #readSyncActions(Database, List, int, Long, List)
	 * readSyncActions()} at the remote side.
	 * 
	 * <p>See the remarks at {@link #setTimeRangeRestrictions(List)
	 * setTimeRangeRestrictions()} in case you have used it.</p>
	 *
	 * @param database the database
	 * @return the synchronisation progress
	 * @throws DatabaseException if a database error occurs
	 */
	public List<SyncProgress> getSyncProgress(Database database)
			throws DatabaseException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal("user", user);
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("table", true)
		};
		List<SyncProgress> allProgress = database.select(
				new SyncProgressTableDef(), criteria, 0, sort);
		Map<String,SyncProgress> tableProgress = new LinkedHashMap<>();
		for (SyncProgress sp : allProgress) {
			String table = sp.getTable();
			if (!isTableIncluded(table))
				continue;
			if (tableProgress.containsKey(table)) {
				throw new DatabaseException(String.format(
						"Duplicate sync progress for table \"%s\"", table));
			}
			tableProgress.put(table, sp);
		}
		return new ArrayList<>(tableProgress.values());
	}
	
	/**
	 * Returns statistics about new database actions that should be written to
	 * a remote database. It defines the number of new database actions and the
	 * time of the latest database action (if any). The values are obtained
	 * from multiple queries (for each table first the count, then the latest
	 * time), so they may not exactly correspond. To get a database action with
	 * the specified latest time (there may be more than one), it may be needed
	 * to read more actions than the specified count. You may use the count as
	 * an indication of how many database actions need to be synchronised, so
	 * you could inform the user about the progress.
	 * 
	 * <p>The specified progress list defines what data has already been
	 * synchronised before. It can contain zero or one progress object for each
	 * table. The user field in the progress object is ignored and may be set
	 * to null. You can set "excludeSources" to ensure that you don't select
	 * database actions that were written by the remote database itself.</p>
	 * 
	 * @param database the database
	 * @param progress the progress or null
	 * @param excludeSources database actions with one of these sources will
	 * be excluded. This can be set to null or an empty list.
	 * @return the statistics about new database actions
	 * @throws DatabaseException if a database error occurs
	 */
	public SyncActionStats getSyncActionStats(Database database,
			List<SyncProgress> progress, List<String> excludeSources)
			throws DatabaseException {
		List<String> tables = getSyncTables(database);
		int totalCount = 0;
		Long maxTime = null;
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		for (String table : tables) {
			SyncProgress sp = findTableProgress(table, progress);
			DatabaseCriteria criteria = getReadSyncCriteria(table, sp,
					null, excludeSources);
			String actionUser = null;
			if (isUserTable(database, table))
				actionUser = this.user;
			DatabaseActionTable actionTable = DatabaseCache
					.getInstance().initActionTable(database, actionUser, table);
			int count = database.count(actionTable.getName(),
					DatabaseAction.class, criteria);
			if (count == 0)
				continue;
			DatabaseSort[] sort = new DatabaseSort[] {
					new DatabaseSort("time", false)
			};
			List<Map<String,?>> maps = database.selectMaps(
					actionTable.getName(), DatabaseAction.class, criteria,
					1, sort);
			if (maps.isEmpty())
				continue;
			DatabaseAction action = mapper.mapToObject(maps.get(0),
					DatabaseAction.class, false);
			totalCount += count;
			if (maxTime == null || action.getTime() > maxTime)
				maxTime = action.getTime();
		}
		return new SyncActionStats(progress, totalCount, maxTime);
	}

	/**
	 * Reads new database actions that should be written to a remote database.
	 * The specified progress list defines what data has already been
	 * synchronised before. It can contain zero or one progress object for
	 * each table. The user field in the progress object is ignored and may be
	 * set to null. You can set "excludeSources" to ensure that you don't
	 * select database actions that were written by the remote database itself.
	 * 
	 * <p>You can read actions in batches by specifying maxCount. Because new
	 * data can be written continuously, it's recommended to specify maxTime
	 * as well, so it can be guaranteed that the sync process ends (this method
	 * returns an empty list). Use {@link
	 * #getSyncActionStats(Database, List, List) getSyncActionStats()} to get
	 * the maximum time.</p>
	 *
	 * <p>This method may merge database actions using {@link
	 * DatabaseActionMerger DatabaseActionMerger}.</p>
	 *
	 * @param database the database
	 * @param progress the progress or null
	 * @param maxCount the maximum number of actions to read (&lt;= 0 if no
	 * limit)
	 * @param maxTime the maximum time of an action, as a unix time in
	 * milliseconds (null if no limit)
	 * @param excludeSources database actions with one of these sources will
	 * be excluded. This can be set to null or an empty list.
	 * @return the new database actions
	 * @throws DatabaseException if a database error occurs
	 */
	public List<DatabaseAction> readSyncActions(Database database,
			List<SyncProgress> progress, int maxCount, Long maxTime,
			List<String> excludeSources) throws DatabaseException {
		List<String> tables = getSyncTables(database);
		List<DatabaseAction> actions = new ArrayList<>();
		Iterator<String> tableIt = tables.iterator();
		while (tableIt.hasNext() && (maxCount <= 0 ||
				maxCount > actions.size())) {
			String table = tableIt.next();
			SyncProgress sp = findTableProgress(table, progress);
			int iterMaxCount = maxCount > 0 ?
					maxCount - actions.size() : 0;
			String actionUser = null;
			if (isUserTable(database, table))
				actionUser = this.user;
			List<DatabaseAction> unmerged = readTableSyncActions(database,
					actionUser, table, sp, iterMaxCount, maxTime,
					excludeSources);
			DatabaseActionMerger merger = new DatabaseActionMerger();
			try {
				actions.addAll(merger.mergeActions(unmerged));
			} catch (MergeException ex) {
				throw new DatabaseException(
						"Can't merge database actions: " + ex.getMessage(),
						ex);
			}
		}
		return actions;
	}

	/**
	 * Returns the names of the tables that can be synchronised. It returns
	 * all tables in the database for which {@link #isTableIncluded(String)
	 * isTableIncluded()} returns true.
	 * 
	 * @param database the database
	 * @return the table names
	 * @throws DatabaseException if a database error occurs
	 */
	private List<String> getSyncTables(Database database)
			throws DatabaseException {
		List<String> allTables = database.selectTables();
		List<String> syncTables = new ArrayList<>();
		for (String table : allTables) {
			if (isTableIncluded(table))
				syncTables.add(table);
		}
		return syncTables;
	}

	/**
	 * Finds the progress for the specified table. If there is no progress
	 * object for that table, this method returns null.
	 *
	 * @param table the table name
	 * @param progress the progress objects (may be null)
	 * @return the progress object for the specified table or null
	 */
	private SyncProgress findTableProgress(String table,
			List<SyncProgress> progress) {
		if (progress == null)
			return null;
		for (SyncProgress sp : progress) {
			if (sp.getTable().equals(table))
				return sp;
		}
		return null;
	}

	/**
	 * Reads new database actions for the specified user and table that should
	 * be written to a remote database. The specified progress defines what
	 * data has already been synchronised before. It may be null. The user
	 * field in the progress object is ignored and may be set to null. You can
	 * set "excludeSources" to ensure that you don't select database actions
	 * that were written by the remote database itself.
	 *
	 * @param database the database
	 * @param user the user or null (if the table has general data)
	 * @param table the table name
	 * @param progress the progress or null
	 * @param maxCount the maximum number of actions to read (&lt;= 0 if no
	 * limit)
	 * @param maxTime the maximum time of an action, as a unix time in
	 * milliseconds (null if no limit)
	 * @param excludeSources database actions with one of these sources will
	 * be excluded. This can be set to null or an empty list.
	 * @return the new database actions
	 * @throws DatabaseException if a database error occurs
	 */
	private List<DatabaseAction> readTableSyncActions(Database database,
			String user, String table, SyncProgress progress, int maxCount,
			Long maxTime, List<String> excludeSources)
			throws DatabaseException {
		DatabaseCriteria criteria = getReadSyncCriteria(table, progress,
				maxTime, excludeSources);
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("time", true),
				new DatabaseSort("order", true)
		};
		DatabaseActionTable actionTable = DatabaseCache
				.getInstance().initActionTable(database, user, table);
		List<Map<String,?>> maps = database.selectMaps(actionTable.getName(),
				DatabaseAction.class, criteria, maxCount, sort);
		List<DatabaseAction> result = new ArrayList<>();
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		for (Map<String,?> map : maps) {
			result.add(mapper.mapToObject(map, DatabaseAction.class, false));
		}
		return result;
	}

	/**
	 * Purges records in the database for tables that have a time range
	 * restriction (see {@link #getTimeRangeRestrictions()
	 * getTimeRangeRestrictions()}). It deletes the records that have been
	 * synchronized and that are before the specified time range. It also
	 * deletes related records from the action tables.
	 * 
	 * @param database the database
	 * @param progress the synchronization progress or null
	 * @throws DatabaseException if a database error occurs
	 */
	public void purgeTimeRangeRecords(Database database,
			List<SyncProgress> progress) throws DatabaseException {
		List<String> syncTables = getSyncTables(database);
		for (SyncTimeRangeRestriction restrict : timeRangeRestrictions) {
			if (!syncTables.contains(restrict.getTable()))
				continue;
			SyncProgress tableProgress = findTableProgress(restrict.getTable(),
					progress);
			if (tableProgress == null)
				continue;
			purgeTimeRangeRecords(database, restrict.getTable(), restrict,
					tableProgress);
		}
	}

	/**
	 * Purges records in the database for the specified table. The table should
	 * have a time range restriction (see {@link #getTimeRangeRestrictions()
	 * getTimeRangeRestrictions()}). It deletes the records that have been
	 * synchronized and that are before the specified time range. It also
	 * deletes related records from the action tables.
	 * 
	 * @param database the database
	 * @param table the table
	 * @param restrict the time range restriction of the table
	 * @param progress the synchronization progress of the table
	 * @throws DatabaseException if a database error occurs
	 */
	private void purgeTimeRangeRecords(Database database, String table,
			SyncTimeRangeRestriction restrict, SyncProgress progress)
			throws DatabaseException {
		List<String> tableFields = DatabaseCache.getInstance().getTableFields(
				database, table);
		boolean isTimeTable = tableFields.contains("localTime");
		if (!isTimeTable)
			return;
		boolean isUtcTable = tableFields.contains("utcTime");
		boolean oldSyncEnabled = database.isSyncEnabled();
		database.setSyncEnabled(false);
		String actionUser = null;
		List<DatabaseCriteria> andCriteria = new ArrayList<>();
		if (isUserTable(database, table)) {
			actionUser = this.user;
			andCriteria.add(new DatabaseCriteria.Equal("user", user));
		}
		if (isUtcTable) {
			andCriteria.add(new DatabaseCriteria.LessThan("utcTime",
						restrict.getStartTime()));
		} else {
			Instant instant = Instant.ofEpochMilli(restrict.getStartTime());
			DateTimeFormatter format = DateTimeFormatter.ofPattern(
					"yyyy-MM-dd'T'HH:mm:ss.SSS");
			String localTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
					.toLocalDateTime().format(format);
			andCriteria.add(new DatabaseCriteria.LessThan("localTime",
					localTime));
		}
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				andCriteria.toArray(new DatabaseCriteria[0]));
		database.delete(table, null, criteria);
		database.setSyncEnabled(oldSyncEnabled);
		DatabaseActionTable actionTable = DatabaseCache.getInstance()
				.initActionTable(database, actionUser, table);
		criteria = new DatabaseCriteria.And(
			new DatabaseCriteria.Or(
				new DatabaseCriteria.LessThan("time", progress.getTime()),
				new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("time", progress.getTime()),
					new DatabaseCriteria.LessEqual("order", progress.getOrder())
				)
			),
			new DatabaseCriteria.LessThan("sampleTime", restrict.getStartTime())
		);
		database.delete(actionTable.getName(), null, criteria);
	}
	
	/**
	 * Returns database criteria to select new database actions that should be
	 * written to a remote database. The progress defines what data has already
	 * been synchronised before. This can be null. The user field in the
	 * progress object is ignored and may be set to null. You can set
	 * "excludeSources" to ensure that you don't select database actions that
	 * were written by the remote database itself.
	 *
	 * @param table the table name
	 * @param progress the progress or null
	 * @param maxTime the maximum time of an action, as a unix time in
	 * milliseconds (null if no limit)
	 * @param excludeSources database actions with one of these sources will
	 * be excluded. This can be set to null or an empty list.
	 * @return the database criteria
	 */
	private DatabaseCriteria getReadSyncCriteria(String table,
			SyncProgress progress, Long maxTime, List<String> excludeSources) {
		List<DatabaseCriteria> criteriaItems = new ArrayList<>();
		criteriaItems.add(new DatabaseCriteria.NotEqual("action", "SELECT"));
		if (excludeSources != null) {
			for (String excludeSource : excludeSources) {
				criteriaItems.add(new DatabaseCriteria.NotEqual("source",
						excludeSource));
			}
		}
		if (progress != null) {
			criteriaItems.add(new DatabaseCriteria.Or(
				new DatabaseCriteria.GreaterThan("time", progress.getTime()),
				new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("time", progress.getTime()),
					new DatabaseCriteria.GreaterThan("order",
					progress.getOrder())
				)
			));
		}
		if (maxTime != null) {
			criteriaItems.add(new DatabaseCriteria.LessEqual("time", maxTime));
		}
		SyncTimeRangeRestriction restriction = findTimeRangeRestriction(table);
		if (restriction != null) {
			criteriaItems.add(new DatabaseCriteria.GreaterEqual(
					"sampleTime", restriction.getStartTime()));
			criteriaItems.add(new DatabaseCriteria.LessThan(
					"sampleTime", restriction.getEndTime()));
		}
		DatabaseCriteria[] criteriaArray = criteriaItems.toArray(
				new DatabaseCriteria[0]);
		return new DatabaseCriteria.And(criteriaArray);
	}

	/**
	 * Tries to find a time range restriction for the specified table. If no
	 * restriction is found, this method returns null.
	 * 
	 * @param table the table name
	 * @return the restriction or null
	 */
	private SyncTimeRangeRestriction findTimeRangeRestriction(String table) {
		if (timeRangeRestrictions == null)
			return null;
		for (SyncTimeRangeRestriction restriction : timeRangeRestrictions) {
			if (restriction.getTable().equals(table))
				return restriction;
		}
		return null;
	}

	/**
	 * Writes the specified database actions from a remote database to this
	 * database. It validates every action in the context of this synchroniser.
	 * It checks whether the data is valid and whether the action is allowed.
	 * For example the data is invalid if a required field is set to null or
	 * some field value is not consistent with another. The action is not
	 * allowed for example if this synchroniser allows only data for one user
	 * and the action affects data for another user.
	 *
	 * <p>After writing an action, this method updates the table
	 * "_sync_progress". The action is also added to the table "_action_log"
	 * with the specified source ID. You can pass the same source ID when you
	 * read new database actions. This ensures that you won't get the actions
	 * that you wrote with this method.</p>
	 *
	 * @param database the database
	 * @param actions the actions
	 * @param source the source of the database action. This should identify
	 * the remote database and is used to ensure that these database actions
	 * are excluded at a reverse synchronization.
	 * @throws SyncForbiddenException if an action is not allowed
	 * @throws IllegalInputException if an action is invalid
	 * @throws DatabaseException if a database error occurs
	 */
	public void writeSyncActions(Database database,
			List<DatabaseAction> actions, String source)
			throws SyncForbiddenException, IllegalInputException,
			DatabaseException {
		List<SyncProgress> progressList = getSyncProgress(database);
		CurrentIterator<DatabaseAction> actionIt =
				new CurrentIterator<>(actions.iterator());
		actionIt.moveNext();
		DatabaseActionGroup actionGroup = getNextActionGroup(database,
				actionIt, progressList);
		DatabaseCache cache = DatabaseCache.getInstance();
		while (actionGroup != null) {
			List<DatabaseActionGroup.Item> groupItems =
					actionGroup.getActionsToRun();
			if (!groupItems.isEmpty()) {
				DatabaseAction action = groupItems.get(0).action;
				DatabaseAction.Action actionType = action.getAction();
				String actionUser = null;
				if (cache.isTableSplitByUser(database, action.getTable()))
					actionUser = action.getUser();
				switch (actionType) {
				case INSERT:
					writeSyncInsertActionGroup(database, groupItems, source);
					break;
				case UPDATE:
					writeSyncUpdateActionGroup(database, groupItems, actionUser,
							source);
					break;
				case DELETE:
					writeSyncDeleteActionGroup(database, groupItems, actionUser,
							source);
					break;
				default:
					break;
				}
			}

			// update sync progress
			DatabaseAction lastAction = actionGroup.getLastAction();
			String table = lastAction.getTable();
			DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("table", table),
					new DatabaseCriteria.Equal("user", user)
			);
			SyncProgress progress = database.selectOne(
					new SyncProgressTableDef(), criteria, null);
			if (progress == null) {
				progress = new SyncProgress();
				progress.setTable(table);
				progress.setUser(user);
			}
			progress.setTime(lastAction.getTime());
			progress.setOrder(lastAction.getOrder());
			if (progress.getId() == null)
				database.insert(SyncProgressTableDef.NAME, progress);
			else
				database.update(SyncProgressTableDef.NAME, progress);

			actionGroup = getNextActionGroup(database, actionIt, progressList);
		}
	}
	
	/**
	 * Writes a group of insert database actions. This method is called from
	 * {@link #writeSyncActions(Database, List, String) writeSyncActions()}.
	 * The specified action group contains one or more insert actions for the
	 * same table. The actions have been validated.
	 * 
	 * @param database the database
	 * @param actions the actions
	 * @param source the source of the database action. This should identify
	 * the remote database and is used to ensure that these database actions
	 * are excluded at a reverse synchronization.
	 * @throws DatabaseException if a database error occurs
	 */
	private void writeSyncInsertActionGroup(Database database,
			List<DatabaseActionGroup.Item> actions, String source)
			throws DatabaseException {
		String table = actions.get(0).action.getTable();
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("Run group of {} insert actions on table {}",
				actions.size(), table);
		List<Map<String,Object>> values = new ArrayList<>();
		for (DatabaseActionGroup.Item item : actions) {
			values.add(item.data);
		}
		database.insertMaps(table, values, source);
	}

	/**
	 * Writes a group of update database actions. This method is called from
	 * {@link #writeSyncActions(Database, List, String) writeSyncActions()}.
	 * The specified action group contains one or more update actions for the
	 * same table. The actions have been validated.
	 * 
	 * @param database the database
	 * @param actions the actions
	 * @param user if the actions are for a table that is split by user, you
	 * should specify the user to which the actions belong. Otherwise you should
	 * set this to null.
	 * @param source the source of the database action. This should identify
	 * the remote database and is used to ensure that these database actions
	 * are excluded at a reverse synchronization.
	 * @throws DatabaseException if a database error occurs
	 */
	private void writeSyncUpdateActionGroup(Database database,
			List<DatabaseActionGroup.Item> actions, String user, String source)
			throws DatabaseException {
		String table = actions.get(0).action.getTable();
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("Run group of {} update actions on table {}",
				actions.size(), table);
		for (DatabaseActionGroup.Item item : actions) {
			DatabaseAction action = item.action;
			DatabaseCriteria criteria;
			if (user == null) {
				criteria = new DatabaseCriteria.Equal("id",
						action.getRecordId());
			} else {
				criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", user),
					new DatabaseCriteria.Equal("id", action.getRecordId())
				);
			}
			database.update(action.getTable(), null, criteria, item.data,
					source);
		}
	}

	/**
	 * Writes a group of delete database actions. This method is called from
	 * {@link #writeSyncActions(Database, List, String) writeSyncActions()}.
	 * The specified action group contains one or more delete actions for the
	 * same table. The actions have been validated.
	 * 
	 * @param database the database
	 * @param actions the actions
	 * @param user if the actions are for a table that is split by user, you
	 * should specify the user to which the actions belong. Otherwise you should
	 * set this to null.
	 * @param source the source of the database action. This should identify
	 * the remote database and is used to ensure that these database actions
	 * are excluded at a reverse synchronization.
	 * @throws DatabaseException if a database error occurs
	 */
	private void writeSyncDeleteActionGroup(Database database,
			List<DatabaseActionGroup.Item> actions, String user, String source)
			throws DatabaseException {
		String table = actions.get(0).action.getTable();
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("Run group of {} delete actions on table {}",
				actions.size(), table);
		for (DatabaseActionGroup.Item item : actions) {
			DatabaseAction action = item.action;
			DatabaseCriteria criteria;
			if (user == null) {
				criteria = new DatabaseCriteria.Equal("id",
						action.getRecordId());
			} else {
				criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", user),
					new DatabaseCriteria.Equal("id", action.getRecordId())
				);
			}
			database.delete(action.getTable(), null, criteria, source);
		}
	}

	/**
	 * Consumes the next group of actions with the same action type and the
	 * same table. It skips actions that have already been synchronised
	 * according to the specified progress list. When you call this method, the
	 * action iterator should be positioned at the first action or later. When
	 * it returns, the iterator will be positioned at the start of the next
	 * group, or after the last action.
	 *
	 * <p>If the table is split by user, the returned actions will also belong
	 * to the same user.</p>
	 * 
	 * <p>This method validates each action and throws an exception if an
	 * action is invalid. See {@link #writeSyncActions(Database, List, String)
	 * writeSyncActions()} for more info.</p>
	 * 
	 * @param database the database
	 * @param actionIt the action iterator, positioned at the first or a later
	 * action
	 * @param progressList the progress list or null
	 * @return the next action group or null
	 * @throws IllegalInputException it the action data is invalid
	 */
	private DatabaseActionGroup getNextActionGroup(Database database,
			CurrentIterator<DatabaseAction> actionIt,
			List<SyncProgress> progressList) throws SyncForbiddenException,
			IllegalInputException, DatabaseException {
		DatabaseAction first = null;
		boolean isTableSplitByUser = false;
		DatabaseActionGroup actionGroup = null;
		List<DatabaseActionGroup.Item> groupItems = new ArrayList<>();
		while (actionIt.getCurrent() != null) {
			DatabaseAction action = actionIt.getCurrent();
			if (action.getTable() == null)
				throw new IllegalInputException("Table not set");
	
			// check if already synchronised
			SyncProgress progress = findTableProgress(action.getTable(),
					progressList);
			if (progress != null && (action.getTime() < progress.getTime() ||
					(action.getTime() == progress.getTime() &&
					action.getOrder() <= progress.getOrder()))) {
				actionIt.moveNext();
				continue;
			}

			ValidateWriteResult validation = validateWriteAction(database,
					action);
			if (first == null) {
				first = action;
				actionGroup = new DatabaseActionGroup();
				actionGroup.setActionsToRun(groupItems);
				actionGroup.setLastAction(action);
				DatabaseCache cache = DatabaseCache.getInstance();
				isTableSplitByUser = cache.isTableSplitByUser(database,
						first.getTable());
			} else if (!validation.skipAction) {
				if (first.getAction() != action.getAction()) {
					return actionGroup;
				}
				if (!first.getTable().equals(action.getTable())) {
					return actionGroup;
				}
				if (isTableSplitByUser && !first.getUser().equals(
						action.getUser())) {
					return actionGroup;
				}
			}
			if (!validation.skipAction) {
				groupItems.add(new DatabaseActionGroup.Item(action,
						validation.data));
			}
			actionGroup.setLastAction(action);
			actionIt.moveNext();
		}
		return actionGroup;
	}

	/**
	 * The result of {@link
	 * DatabaseSynchronizer#validateWriteAction(Database, DatabaseAction)
	 * validateWriteAction()}.
	 */
	private static class ValidateWriteResult {
		public boolean skipAction = false;
		public Map<String,Object> data = null;
	}

	/**
	 * Validates the table and user of the specified database action in the
	 * context of this synchronizer. If the validation fails, this method throws
	 * an exception.
	 *
	 * @param database the database
	 * @param action the action
	 * @throws SyncForbiddenException if the action is not allowed
	 * @throws IllegalInputException it the action data is invalid
	 * @throws DatabaseException if a database error occurs
	 */
	private void validateWriteTableUser(Database database,
			DatabaseAction action) throws SyncForbiddenException,
			IllegalInputException, DatabaseException {
		String table = action.getTable();
		// validate table name
		if (table == null) {
			throw new IllegalInputException("Table not set");
		}
		List<String> dbTables = database.selectTables();
		if (!dbTables.contains(table)) {
			throw new IllegalInputException(String.format(
					"Table \"%s\" not found", action.getTable()));
		}
		if (table.startsWith("_")) {
			throw new SyncForbiddenException(String.format(
					"Writing to reserved table \"%s\" not allowed",
					action.getTable()));
		}
		if (!isTableIncluded(table)) {
			throw new SyncForbiddenException(String.format(
					"Writing to table \"%s\" not allowed", action.getTable()));
		}

		// validate user
		if (isUserTable(database, table) && action.getUser() == null) {
			throw new SyncForbiddenException(String.format(
					"User not specified in database action for user data table \"%s\"",
					action.getTable()));
		}
		if (action.getUser() != null && !user.equals(action.getUser())) {
			throw new SyncForbiddenException(String.format(
					"Writing data for user \"%s\" not allowed",
					action.getUser()));
		}
	}

	/**
	 * Validates the specified database action in the context of this
	 * synchronizer. This is called from {@link
	 * #writeSyncActions(Database, List, String) writeSyncActions()}.
	 *
	 * <p>This method first calls {@link
	 * #validateWriteTableUser(Database, DatabaseAction)
	 * validateWriteTableUser()}. Then it validates that data is included with
	 * an insert or update action and the data is valid (in particular the
	 * sensitive "id" and "user" fields). The parsed action data is set in
	 * "data" in the result.</p>
	 *
	 * <p>Some valid actions need to be skipped or modified. Some actions need
	 * to be skipped if they have already been performed earlier. This can
	 * happen because the progress was not properly updated after the previous
	 * synchronization, for example because the process was aborted. In
	 * particular, it can happen that a delete action on a record has been
	 * performed, and then an earlier update action, or the same delete action
	 * is requested again. In this case the update or delete action can be
	 * skipped. An insert action of the same record can still be performed,
	 * because the later already executed delete action will eventually be
	 * performed again as well. If an action needs to be skipped, this method
	 * sets "skipAction" in the result to true.</p>
	 *
	 * <p>An action that needs to be modified is an insert action for a record
	 * that already exists. This can happen due to merging of database actions.
	 * This means that the original insert action and later update actions are
	 * merged into one new insert action. If the original insert action has
	 * already been performed, the record already exists, and the new insert
	 * action should be changed to an update action. In this case, this method
	 * just changes the action.</p>
	 *
	 * <p>To check whether the action should be skipped or modified, it will
	 * try to read the current record from the database.</p>
	 *
	 * @param database the database
	 * @param action the action (may be modified)
	 * @return the validation result
	 * @throws SyncForbiddenException if the action is not allowed
	 * @throws IllegalInputException it the action data is invalid
	 * @throws DatabaseException if a database error occurs
	 */
	private ValidateWriteResult validateWriteAction(Database database,
			DatabaseAction action) throws SyncForbiddenException,
			IllegalInputException, DatabaseException {
		ValidateWriteResult result = new ValidateWriteResult();
		validateWriteTableUser(database, action);
		ObjectMapper mapper = new ObjectMapper();
		Map<?,?> uncheckedData = null;
		if (action.getJsonData() != null) {
			try {
				uncheckedData = mapper.readValue(action.getJsonData(),
						Map.class);
			} catch (IOException ex) {
				throw new IllegalInputException("Can't parse JSON data: " +
						ex.getMessage(), ex);
			}
		}
		Map<String,Object> data = null;
		if (uncheckedData != null) {
			data = new LinkedHashMap<>();
			for (Object key : uncheckedData.keySet()) {
				data.put((String)key, uncheckedData.get(key));
			}
		}
		String table = action.getTable();
		boolean isUserTable = isUserTable(database, table);
		if (!isUserTable && !isWriteResourceTableAllowed(table)) {
			throw new SyncForbiddenException(String.format(
					"Writing to resource table \"%s\" not allowed", table));
		}
		DatabaseCache cache = DatabaseCache.getInstance();
		List<String> fields = cache.getTableFields(database, table);

		// validate record ID
		if (action.getRecordId() == null) {
			throw new IllegalInputException("Record ID not set");
		}
		String user = null;
		if (cache.isTableSplitByUser(database, table))
			user = action.getUser();
		BaseDatabaseObject record = selectRecord(database, table, user,
				action.getRecordId());
		String recordUser = null;
		if (record instanceof UserDatabaseObject userRecord)
			recordUser = userRecord.getUser();
		if (action.getAction() == DatabaseAction.Action.UPDATE ||
				action.getAction() == DatabaseAction.Action.DELETE) {
			if (record == null) {
				result.skipAction = true;
				return result;
			}
			if (isUserTable && !action.getUser().equals(recordUser)) {
				throw new SyncForbiddenException(String.format(
						"Record with ID \"%s\" does not match user \"%s\"",
						action.getRecordId(), action.getUser()));
			}
		} else if (action.getAction() == DatabaseAction.Action.INSERT) {
			if (record != null && isUserTable &&
					!action.getUser().equals(recordUser)) {
				throw new SyncForbiddenException(String.format(
						"Record with ID \"%s\" already exists and does not match user \"%s\"",
						action.getRecordId(), action.getUser()));
			}
			if (record != null)
				action.setAction(DatabaseAction.Action.UPDATE);
		}

		// validate data
		if (action.getAction() == DatabaseAction.Action.INSERT ||
				action.getAction() == DatabaseAction.Action.UPDATE) {
			if (data == null) {
				throw new IllegalInputException("Data not found for action " +
						action.getAction());
			}
			for (String key : data.keySet()) {
				if (!key.equals("id") && !fields.contains(key)) {
					throw new IllegalInputException(String.format(
							"Unknown field \"%s\" in table \"%s\"",
							key, table));
				}
			}
		}
		if (action.getAction() == DatabaseAction.Action.INSERT) {
			Object dataId = data.get("id");
			if (dataId == null) {
				throw new IllegalInputException(
						"Field \"id\" not set in data to insert");
			}
			if (!action.getRecordId().equals(dataId)) {
				throw new IllegalInputException(String.format(
						"Field \"id\" (%s) does not match record ID (%s)",
						dataId, action.getRecordId()));
			}
		}
		if (action.getAction() == DatabaseAction.Action.UPDATE &&
				data.containsKey("id")) {
			Object dataId = data.get("id");
			if (!action.getRecordId().equals(dataId)) {
				throw new SyncForbiddenException(
						"Changing record ID not allowed");
			}
			data.remove("id");
		}
		if (action.getAction() == DatabaseAction.Action.INSERT && isUserTable) {
			Object dataUser = data.get("user");
			if (dataUser == null) {
				throw new IllegalInputException(String.format(
						"Field \"user\" not set in data to insert into user data table \"%s\"",
						table));
			}
			if (!action.getUser().equals(dataUser)) {
				throw new IllegalInputException(String.format(
						"Field \"user\" (%s) does not match user of action to insert into user data table \"%s\" (%s)",
						dataUser, table, action.getUser()));
			}
		}
		if (action.getAction() == DatabaseAction.Action.UPDATE && isUserTable &&
				data.containsKey("user")) {
			Object dataUser = data.get("user");
			if (!action.getUser().equals(dataUser)) {
				throw new SyncForbiddenException(String.format(
						"Changing field \"user\" not allowed in user data table \"%s\"",
						table));
			}
		}
		result.data = data;
		return result;
	}

	/**
	 * Selects a record from the specified table. If the record does not exist,
	 * this method returns null. If the table is split by user, you must specify
	 * a user.
	 *
	 * @param database the database
	 * @param table the table name
	 * @param user the user or null
	 * @param id the record ID
	 * @return the record or null
	 * @throws DatabaseException if a database error occurs
	 */
	private BaseDatabaseObject selectRecord(Database database, String table,
			String user, String id) throws DatabaseException {
		Class<? extends BaseDatabaseObject> clazz;
		if (isUserTable(database, table))
			clazz = UserDatabaseObject.class;
		else
			clazz = BaseDatabaseObject.class;
		DatabaseCriteria criteria;
		if (user != null) {
			criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("id", id),
					new DatabaseCriteria.Equal("user", user));
		} else {
			criteria = new DatabaseCriteria.Equal("id", id);
		}
		List<? extends BaseDatabaseObject> objects = database.select(table,
				clazz, criteria, 0, null);
		if (objects.isEmpty())
			return null;
		else
			return objects.get(0);
	}
}
