package nl.rrd.senseeact.client.model.sample;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.senseeact.dao.BaseDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The abstract base class for sample tables. It only defines the user (email
 * address) and sample time. It has two subclasses: {@link UTCSample UTCSample}
 * and {@link LocalTimeSample LocalTimeSample}. Other classes should never
 * extend this class directly, but always extend one of the two subclasses.
 * 
 * @author Dennis Hofs (RRD)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public abstract class Sample extends BaseDatabaseObject {
	public static final DateTimeFormatter LOCAL_TIME_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String user;
	
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String localTime;

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public Sample() {
	}

	/**
	 * Returns the user (user ID).
	 * 
	 * @return the user (user ID)
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the user (user ID).
	 * 
	 * @param user the user (user ID)
	 */
	public void setUser(String user) {
		this.user = user;
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
		return LOCAL_TIME_FORMAT.parse(getLocalTime(), LocalDateTime::from);
	}
}
