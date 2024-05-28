package nl.rrd.senseeact.client.model.sample;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.senseeact.dao.BaseDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.LocalDateTime;

/**
 * The abstract base class for resource sample tables (without a user). It only
 * defines a sample time. It has two subclasses: {@link ResourceUTCSample
 * ResourceUTCSample} and {@link ResourceLocalTimeSample
 * ResourceLocalTimeSample}. Other classes should never extend this class
 * directly, but always extend one of the two subclasses.
 *
 * @author Dennis Hofs (RRD)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ResourceSample extends BaseDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String localTime;

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public ResourceSample() {
	}

	/**
	 * Returns the local time, formatted as yyyy-MM-dd'T'HH:mm:ss.SSS.
	 *
	 * @return the local time, formatted as yyyy-MM-dd'T'HH:mm:ss.SSS
	 */
	public String getLocalTime() {
		return localTime;
	}

	/**
	 * Sets the local time, formatted as yyyy-MM-dd'T'HH:mm:ss.SSS. This is
	 * used for DataAccessObjects. Users should not call this.
	 *
	 * @param localTime the local time, formatted as yyyy-MM-dd'T'HH:mm:ss.SSS
	 */
	public void setLocalTime(String localTime) {
		this.localTime = localTime;
	}

	/**
	 * Returns the date/time. It defines the local time and the location-based
	 * time zone (not an offset).
	 *
	 * @return the date/time
	 */
	public LocalDateTime toLocalDateTime() {
		return Sample.LOCAL_TIME_FORMAT.parse(getLocalTime(),
				LocalDateTime::from);
	}
}
