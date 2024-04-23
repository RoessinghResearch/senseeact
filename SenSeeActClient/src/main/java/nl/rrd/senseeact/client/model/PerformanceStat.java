package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import nl.rrd.senseeact.dao.BaseDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.utils.json.DateTimeFromIsoDateTimeDeserializer;
import nl.rrd.utils.json.IsoDateTimeSerializer;

import java.time.ZonedDateTime;

/**
 * This class defines a performance statistic. It defines the start time, end
 * time and duration for a certain operation. Although the duration can be
 * derived from the start and end time, it is stored in a separate field to
 * allow easy database searches.
 * 
 * <p>The times are stored in two fields: as a UTC time and as an ISO time. The
 * ISO time is human-readable, but can't be used for sorting because it can
 * contain times in different time zones. The ISO time specifies a local time
 * and a time zone offset. The time zone offset changes not only when moving to
 * another location, but also when entering or leaving DST time.</p>
 * 
 * <p>Depending on the statistic you may set optional extras in the "extra"
 * field, which is a free text field.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class PerformanceStat extends BaseDatabaseObject {
	
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String name;
	
	@DatabaseField(value=DatabaseType.LONG, index=true)
	private long startUtcTime;
	
	@DatabaseField(value=DatabaseType.ISOTIME)
	@JsonSerialize(using=IsoDateTimeSerializer.class)
	@JsonDeserialize(using=DateTimeFromIsoDateTimeDeserializer.class)
	private ZonedDateTime startIsoTime;
	
	@DatabaseField(value=DatabaseType.LONG)
	private long endUtcTime;
	
	@DatabaseField(value=DatabaseType.ISOTIME)
	@JsonSerialize(using=IsoDateTimeSerializer.class)
	@JsonDeserialize(using=DateTimeFromIsoDateTimeDeserializer.class)
	private ZonedDateTime endIsoTime;
	
	@DatabaseField(value=DatabaseType.LONG)
	private long duration;
	
	@DatabaseField(value=DatabaseType.TEXT)
	private String extra = null;
	
	/**
	 * Constructs a new empty statistic.
	 */
	public PerformanceStat() {
	}
	
	/**
	 * Constructs a new statistic. The extra field will be null.
	 * 
	 * @param name the name that identifies the statistic
	 * @param startTime the start time
	 * @param endTime the end time
	 */
	public PerformanceStat(String name, ZonedDateTime startTime,
			ZonedDateTime endTime) {
		this.name = name;
		updateStartEndTime(startTime, endTime);
	}

	/**
	 * Returns the name that identifies this statistic.
	 * 
	 * @return the name that identifies this statistic
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name that identifies this statistic.
	 * 
	 * @param name the name that identifies this statistic
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the start time, end time and duration. This method sets the fields
	 * "startUtcTime", "startIsoTime", "endUtcTime", "endIsoTime" and
	 * "duration".
	 * 
	 * @param startTime the start time
	 * @param endTime the end time
	 */
	public void updateStartEndTime(ZonedDateTime startTime,
			ZonedDateTime endTime) {
		startUtcTime = startTime.toInstant().toEpochMilli();
		startIsoTime = startTime;
		endUtcTime = endTime.toInstant().toEpochMilli();
		endIsoTime = endTime;
		duration = endUtcTime - startUtcTime;
	}

	/**
	 * Returns the start time as a unix time in milliseconds.
	 * 
	 * @return the start time
	 */
	public long getStartUtcTime() {
		return startUtcTime;
	}

	/**
	 * Sets the start time as a unix time in milliseconds. This is used for
	 * DataAccessObjects. Users should call {@link
	 * #updateStartEndTime(ZonedDateTime, ZonedDateTime) updateStartEndTime()}
	 * instead.
	 * 
	 * @param startUtcTime the start time
	 */
	public void setStartUtcTime(long startUtcTime) {
		this.startUtcTime = startUtcTime;
	}
	
	/**
	 * Returns the start time.
	 * 
	 * @return the start time
	 */
	public ZonedDateTime getStartIsoTime() {
		return startIsoTime;
	}

	/**
	 * Sets the start time. This field is stored in the database as an ISO
	 * date/time string. It is used for DataAccessObjects. Users should call
	 * {@link #updateStartEndTime(ZonedDateTime, ZonedDateTime)
	 * updateStartEndTime()} instead.
	 * 
	 * @param startIsoTime the start time
	 */
	public void setStartIsoTime(ZonedDateTime startIsoTime) {
		this.startIsoTime = startIsoTime;
	}

	/**
	 * Returns the end time as a unix time in milliseconds.
	 * 
	 * @return the end time
	 */
	public long getEndUtcTime() {
		return endUtcTime;
	}

	/**
	 * Sets the end time as a unix time in milliseconds. This is used for
	 * DataAccessObjects. Users should call {@link
	 * #updateStartEndTime(ZonedDateTime, ZonedDateTime) updateStartEndTime()}
	 * instead.
	 * 
	 * @param endUtcTime the end time
	 */
	public void setEndUtcTime(long endUtcTime) {
		this.endUtcTime = endUtcTime;
	}
	
	/**
	 * Returns the end time.
	 * 
	 * @return the end time
	 */
	public ZonedDateTime getEndIsoTime() {
		return endIsoTime;
	}

	/**
	 * Sets the end time. This field is stored in the database as an ISO
	 * date/time string. It is used for DataAccessObjects. Users should call
	 * {@link #updateStartEndTime(ZonedDateTime, ZonedDateTime)
	 * updateStartEndTime()} instead.
	 * 
	 * @param endIsoTime the end time
	 */
	public void setEndIsoTime(ZonedDateTime endIsoTime) {
		this.endIsoTime = endIsoTime;
	}

	/**
	 * Returns the duration in milliseconds.
	 * 
	 * @return the duration
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Sets the duration in milliseconds. This is used for DataAccessObjects.
	 * Users should call {@link
	 * #updateStartEndTime(ZonedDateTime, ZonedDateTime) updateStartEndTime()}
	 * instead.
	 * 
	 * @param duration the duration
	 */
	public void setDuration(long duration) {
		this.duration = duration;
	}

	/**
	 * Returns the extra details. This may be defined depending on the type of
	 * statistic. The default is null.
	 * 
	 * @return the extra details
	 */
	public String getExtra() {
		return extra;
	}

	/**
	 * Sets the extra details. This may be defined depending on the type of
	 * statistic. The default is null.
	 * 
	 * @param extra the extra details
	 */
	public void setExtra(String extra) {
		this.extra = extra;
	}
}
