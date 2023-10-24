package nl.rrd.senseeact.client;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.senseeact.dao.sync.SyncTimeRangeRestriction;
import nl.rrd.utils.json.JsonObject;

/**
 * This class defines synchronization restrictions for reading database actions
 * from the server or writing database actions to the server.
 *
 * @author Dennis Hofs (RRD)
 */
public class SyncRestriction extends JsonObject {
	private boolean syncEnabled = true;
	private SyncTableRestriction tableRestriction = new SyncTableRestriction();
	private List<SyncTimeRangeRestriction> timeRangeRestrictions =
			new ArrayList<>();

	/**
	 * Returns whether a synchronization should be run on the project database.
	 * The default is true. If this method returns false, then no
	 * synchronization will be run, even if the project database exists and
	 * regardless of other restrictions.
	 * 
	 * @return true if synchronization of the project database is enabled
	 * (default), false otherwise
	 */
	public boolean isSyncEnabled() {
		return syncEnabled;
	}

	/**
	 * Sets whether a synchronization should be run on the project database.
	 * The default is true. If this is set to false, then no synchronization
	 * will be run, even if the project database exists and regardless of other
	 * restrictions.
	 * 
	 * @param syncEnabled true if synchronization of the project database
	 * is enabled (default), false otherwise
	 */
	public void setSyncEnabled(boolean syncEnabled) {
		this.syncEnabled = syncEnabled;
	}

	/**
	 * Returns tables that should be included or excluded when reading or
	 * writing database actions from the project database.
	 *
	 * @return the table restriction on the project database
	 */
	public SyncTableRestriction getTableRestriction() {
		return tableRestriction;
	}

	/**
	 * Sets tables that should be included or excluded when reading or writing
	 * database actions from the project database.
	 *
	 * @param tableRestriction the table restriction on the project database
	 */
	public void setTableRestriction(SyncTableRestriction tableRestriction) {
		this.tableRestriction = tableRestriction;
	}

	/**
	 * Returns time range restrictions for the project database. This is used to
	 * limit the range of database actions to read from the server, and to purge
	 * old records from the local database to save storage space.
	 *
	 * <p>During synchronization, the restrictions are only used when reading
	 * database actions from the server, not when writing actions to the
	 * server.</p>
	 *
	 * @return the time range restrictions
	 */
	public List<SyncTimeRangeRestriction> getTimeRangeRestrictions() {
		return timeRangeRestrictions;
	}

	/**
	 * Sets time range restrictions for the project database. This is used to
	 * limit the range of database actions to read from the server, and to purge
	 * old records from the local database to save storage space.
	 *
	 * <p>During synchronization, the restrictions are only used when reading
	 * database actions from the server, not when writing actions to the
	 * server.</p>
	 *
	 * @param timeRangeRestrictions the time range restrictions
	 */
	public void setTimeRangeRestrictions(
			List<SyncTimeRangeRestriction> timeRangeRestrictions) {
		this.timeRangeRestrictions = timeRangeRestrictions;
	}

	/**
	 * Adds a time range restriction on a table in the project database. This is
	 * used to limit the range of database actions to read from the server, and
	 * to purge old records from the local database to save storage space.
	 *
	 * <p>During synchronization, the restrictions are only used when reading
	 * database actions from the server, not when writing actions to the
	 * server.</p>
	 *
	 * @param restriction the time range restriction
	 */
	public void addTimeRangeRestriction(SyncTimeRangeRestriction restriction) {
		timeRangeRestrictions.add(restriction);
	}

	/**
	 * Adds a time range restriction on a table in the project database. This is
	 * used to limit the range of database actions to read from the server, and
	 * to purge old records from the local database to save storage space.
	 *
	 * <p>During synchronization, the restrictions are only used when reading
	 * database actions from the server, not when writing actions to the
	 * server.</p>
	 * 
	 * <p>The times should be UNIX times in milliseconds. If the times are
	 * matched to field "localTime", they will be converted to a local time in
	 * time zone UTC.</p>
	 *
	 * @param table the table name
	 * @param startTime the start time (inclusive)
	 * @param endTime the end time (exclusive)
	 */
	public void addTimeRangeRestriction(String table, long startTime,
			long endTime) {
		timeRangeRestrictions.add(new SyncTimeRangeRestriction(table,
				startTime, endTime));
	}

	/**
	 * Merges this restriction with an other restriction, so that all data
	 * that passes either restriction, will pass the result restriction.
	 *
	 * <p>There should not be a time range restriction on the same table in both
	 * restrictions.</p>
	 *
	 * @param other the other restriction
	 * @param tables the tables in the project (can be empty or null)
	 * @return the merged restriction
	 */
	public SyncRestriction mergeOr(SyncRestriction other, List<String> tables) {
		SyncRestriction result = new SyncRestriction();
		if (isSyncEnabled() && other.isSyncEnabled()) {
			SyncTableRestriction tableRestrict = tableRestriction.mergeOr(
					other.getTableRestriction(), tables);
			if (tableRestrict != null) {
				result.setTableRestriction(tableRestrict);
			} else {
				result.setSyncEnabled(false);
			}
		} else if (isSyncEnabled()) {
			result.setTableRestriction(tableRestriction);
		} else if (other.isSyncEnabled()) {
			result.setTableRestriction(other.tableRestriction);
		} else {
			result.setSyncEnabled(false);
		}
		result.timeRangeRestrictions.addAll(timeRangeRestrictions);
		result.timeRangeRestrictions.addAll(other.timeRangeRestrictions);
		return result;
	}
}
