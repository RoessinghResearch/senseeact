package nl.rrd.senseeact.dao.sync;

import nl.rrd.utils.json.JsonObject;
import nl.rrd.senseeact.dao.Database;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * A sync time range restriction can be added to a {@link DatabaseSynchronizer
 * DatabaseSynchronizer}. When you read database actions for the specified table
 * from the synchronizer, you will only get actions with startTime &lt;=
 * recordTime &lt; endTime. This is only useful if the table has a time field.
 * It may define a UTC time or a local time:
 * 
 * <p><ul>
 * <li>If the table has field "utcTime", the specified start and end time will
 * be matched to that field.</li>
 * <li>If the table does not have field "utcTime" but it has field "localTime",
 * then the specified start and end time will be converted to a local time in
 * time zone UTC and then matched to the "localTime" field.</li>
 * </ul></p>
 *
 * <p>It can also be used to purge old records from the local database to
 * save storage space. See {@link
 * DatabaseSynchronizer#purgeTimeRangeRecords(Database, List)
 * DatabaseSynchronizer.purgeTimeRangeRecords()}.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class SyncTimeRangeRestriction extends JsonObject {
	private String table;
	private long startTime;
	private long endTime;
	
	/**
	 * Constructs a new instance that still needs to be initialized.
	 */
	public SyncTimeRangeRestriction() {
	}
	
	/**
	 * Constructs a new instance. The times should be UNIX times in
	 * milliseconds. If the times are matched to field "localTime", they will
	 * be converted to a local time in time zone UTC.
	 * 
	 * @param table the table name
	 * @param startTime the start time (inclusive)
	 * @param endTime the end time (exclusive)
	 */
	public SyncTimeRangeRestriction(String table, long startTime,
			long endTime) {
		this.table = table;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	/**
	 * Returns the table name.
	 * 
	 * @return the table name
	 */
	public String getTable() {
		return table;
	}

	/**
	 * Sets the table name.
	 * 
	 * @param table the table name
	 */
	public void setTable(String table) {
		this.table = table;
	}

	/**
	 * Returns the start time (inclusive) as a UNIX time in milliseconds. If the
	 * time is matched to field "localTime", it should be converted to a local
	 * time in time zone UTC.
	 * 
	 * @return the start time (inclusive)
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Sets the start time (inclusive) as a UNIX time in milliseconds. If the
	 * time is matched to field "localTime", it will be converted to a local
	 * time in time zone UTC.
	 * 
	 * @param startTime the start time (inclusive)
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * Returns the end time (exclusive) as a UNIX time in milliseconds. If the
	 * time is matched to field "localTime", it should be converted to a local
	 * time in time zone UTC.
	 * 
	 * @return the end time (exclusive)
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * Sets the end time (exclusive) as a UNIX time in milliseconds. If the time
	 * is matched to field "localTime", it will be converted to a local time in
	 * time zone UTC.
	 * 
	 * @param endTime the end time (exclusive)
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * Returns a start time to select records for the specified date. This
	 * method returns a time so that it is guaranteed that all samples of the
	 * specified date are selected in any time zone. It will be before the
	 * actual start time of the date.
	 *
	 * @param date the date
	 * @return the start time
	 */
	public static ZonedDateTime getStartTimeForDate(LocalDate date) {
		return date.minusDays(1).atStartOfDay(ZoneOffset.UTC);
	}

	/**
	 * Returns an end time to select records for the specified date. This method
	 * returns a time so that it is guaranteed that all samples of the specified
	 * date are selected in any time zone. It will be after the actual end time
	 * of the date.
	 *
	 * @param date the date
	 * @return the end time
	 */
	public static ZonedDateTime getEndTimeForDate(LocalDate date) {
		return date.plusDays(2).atStartOfDay(ZoneOffset.UTC);
	}
}
