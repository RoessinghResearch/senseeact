package nl.rrd.senseeact.client.model.sample;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseFieldException;
import nl.rrd.senseeact.dao.DatabaseObjectMapper;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The base class for a resource sample table (without a user) that stores
 * samples with a UTC time. Resource sample classes should always extend either
 * this class or {@link ResourceLocalTimeSample ResourceLocalTimeSample}.
 *
 * <p>This base class only defines the sample time. Subclasses should define the
 * sample value. The sample time is defined by a UTC time and time zone. The UTC
 * time is defined as a long value, so it's possible to sort objects on that
 * field. This is not possible with ISO time strings with different time zones.
 * Note that a time zone offset as specified in an ISO time string changes not
 * only when moving to another location, but also when entering of leaving DST
 * time. The time zone field should be defined as a location such as
 * Europe/Amsterdam and not as an offset, so it is not affected by DST time.</p>
 *
 * <p>The local time is defined for a given UTC time and time zone, but there
 * is an extra field "localTime". A database table may index it. In particular
 * this is useful if you want to search objects by date.</p>
 *
 * <p>Set methods may throw a {@link ParseException ParseException}. In that
 * case the parameter value will be treated as illegal and the {@link
 * DatabaseObjectMapper DatabaseObjectMapper} will throw a {@link
 * DatabaseFieldException DatabaseFieldException}.</p>
 *
 * @author Dennis Hofs (RRD)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ResourceUTCSample extends ResourceSample {

	@DatabaseField(value=DatabaseType.LONG, index=true)
	private long utcTime;

	@DatabaseField(value=DatabaseType.STRING)
	private String timezone;

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public ResourceUTCSample() {
	}

	/**
	 * Constructs a new sample at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 *
	 * @param tzTime the time
	 */
	public ResourceUTCSample(ZonedDateTime tzTime) {
		this.utcTime = tzTime.toInstant().toEpochMilli();
		this.timezone = tzTime.getZone().getId();
		setLocalTime(tzTime.format(Sample.LOCAL_TIME_FORMAT));
	}

	/**
	 * Returns the UTC time as a unix time in milliseconds.
	 *
	 * @return the UTC time
	 */
	public long getUtcTime() {
		return utcTime;
	}

	/**
	 * Sets the UTC time as a unix time in milliseconds. This is used for
	 * DataAccessObjects. User should not call this.
	 *
	 * @param utcTime the UTC time
	 */
	public void setUtcTime(long utcTime) {
		this.utcTime = utcTime;
	}

	/**
	 * Returns the time zone. This should be a location-based identifier from
	 * the tz database, such as Europe/Amsterdam.
	 *
	 * @return the time zone
	 */
	public String getTimezone() {
		return timezone;
	}

	/**
	 * Sets the time zone. This should be a location-based identifier from the
	 * tz database, such as Europe/Amsterdam. This is used for
	 * DataAccessObjects. User should not call this.
	 *
	 * @param timezone the time zone
	 */
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	/**
	 * Returns the date/time. It defines the local time and the location-based
	 * time zone (not an offset).
	 *
	 * @return the date/time
	 */
	public ZonedDateTime toDateTime() {
		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(utcTime),
				toTimeZone());
	}

	/**
	 * Changes the date/time. It should define the local time and
	 * location-based time zone (not an offset).
	 *
	 * @param tzTime the date/time
	 */
	public void updateDateTime(ZonedDateTime tzTime) {
		this.utcTime = tzTime.toInstant().toEpochMilli();
		this.timezone = tzTime.getZone().getId();
		setLocalTime(tzTime.format(Sample.LOCAL_TIME_FORMAT));
	}

	/**
	 * Returns the time zone. This is a location-based time zone, not an
	 * offset.
	 *
	 * @return the time zone
	 */
	public ZoneId toTimeZone() {
		return ZoneId.of(timezone);
	}
}
