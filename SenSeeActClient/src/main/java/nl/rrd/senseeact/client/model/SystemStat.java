package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.DateTimeFromIsoDateTimeDeserializer;
import nl.rrd.utils.json.IsoDateTimeSerializer;
import nl.rrd.senseeact.dao.BaseDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.ZonedDateTime;

/**
 * This class defines the value of a system statistic and the time when the
 * value was assessed. Examples:
 * 
 * <p><ul>
 * <li>Free memory</li>
 * <li>Free disk space</li>
 * <li>Number of registered users</li>
 * <li>Number of linked Fitbits</li>
 * <li>Number of unique active users per day/week/month</li>
 * </ul></p>
 * 
 * <p>Values are stored as a long. If you want to store a float, convert it to
 * an acceptable unit that can be stored as a long. For example instead of
 * kilometres store metres.</p>
 * 
 * <p>The time is stored in two fields: utcTime and isoTime. The ISO time is
 * human-readable, but can't be used for sorting because it can contain times
 * in different time zones. The ISO time specifies a local time and a time zone
 * offset. The time zone offset changes not only when moving to another
 * location, but also when entering or leaving DST time.</p>
 * 
 * <p>Depending on the statistic you may set optional extras in the "extra"
 * field, which is a free text field.</p>
 * 
 * <p>This class defines some statistics in STAT_* constants. See their
 * documentation for their values and extras.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class SystemStat extends BaseDatabaseObject {
	
	public enum Name {
		/**
		 * The amount of free memory in the Java Virtual Machine. The value is
		 * in bytes. The extra field is null.
		 */
		FREE_MEMORY("free_memory"),
	
		/**
		 * The maximum amount of memory that the Java Virtual Machine will
		 * attempt to use. The value is in bytes. The extra field is null.
		 */
		MAX_MEMORY("max_memory"),
		
		/**
		 * The total amount of memory in the Java Virtual Machine. The value is
		 * in bytes. The extra field is null.
		 */
		TOTAL_MEMORY("total_memory"),
		
		/**
		 * The total size of the file store in bytes. The extra field identifies
		 * the file store. For example in Windows this could be the volume name
		 * and drive letter. In Linux this could be the mount point and
		 * partition.
		 */
		TOTAL_FILE_STORE_SPACE("total_file_store_space"),
		
		/**
		 * The free space in the file store in bytes. Not all space may be
		 * available to the Java Virtual Machine. See also {@link
		 * #USABLE_FILE_STORE_SPACE USABLE_FILE_STORE_SPACE}. The extra field
		 * identifies the file store. For example in Windows this could be the
		 * volume name and drive letter. In Linux this could be the mount point
		 * and partition.
		 */
		UNALLOCATED_FILE_STORE_SPACE("unallocated_file_store_space"),
	
		/**
		 * The free space in the file store that can be used by the Java Virtual
		 * Machine. The file store may have more free space that is not usable.
		 * See also {@link #UNALLOCATED_FILE_STORE_SPACE
		 * UNALLOCATED_FILE_STORE_SPACE}. The extra field identifies the file
		 * store. For example in Windows this could be the volume name and drive
		 * letter. In Linux this could be the mount point and partition.
		 */
		USABLE_FILE_STORE_SPACE("usable_file_store_space"),
		
		/**
		 * CPU load of the entire system in percent, 1-minute average,
		 * as obtained by getSystemLoadAverage().
		 * The extra field is null.
		 */
		SYSTEM_CPU_LOAD_AVG_1MIN("system_cpu_load_avg_1min");
		
		private final String name;
		
		Name(String name) {
			this.name = name;
		}
		
		public static Name fromStringValue(String val) {
			for (Name stat : values()) {
				if (stat.name.equals(val))
					return stat;
			}
			throw new IllegalArgumentException(String.format(
					"Unknown name \"%s\" in enum %s", val,
					Name.class.getName()));
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private Name name;

	@DatabaseField(value=DatabaseType.LONG)
	private long value;
	
	@DatabaseField(value=DatabaseType.LONG, index=true)
	private long utcTime;
	
	@DatabaseField(value=DatabaseType.ISOTIME)
	@JsonSerialize(using=IsoDateTimeSerializer.class)
	@JsonDeserialize(using=DateTimeFromIsoDateTimeDeserializer.class)
	private ZonedDateTime isoTime;
	
	@DatabaseField(value=DatabaseType.TEXT)
	private String extra = null;

	/**
	 * Constructs a new empty statistic.
	 */
	public SystemStat() {
	}
	
	/**
	 * Constructs a new statistic. For the name you may use one of the STAT_*
	 * constants defined in this class. See its documentation about the value.
	 * The extra field will be null.
	 * 
	 * @param name the name that identifies the statistic
	 * @param value the value
	 * @param time the time when the value was assessed
	 */
	public SystemStat(Name name, long value, ZonedDateTime time) {
		this.name = name;
		this.value = value;
		updateTime(time);
	}
	
	/**
	 * Constructs a new statistic. For the name you may use one of the STAT_*
	 * constants defined in this class. See its documentation about the value
	 * and extras.
	 * 
	 * @param name the name that identifies the statistic
	 * @param value the value
	 * @param time the time when the value was assessed
	 * @param extra the extra field
	 */
	public SystemStat(Name name, long value, ZonedDateTime time, String extra) {
		this.name = name;
		this.value = value;
		this.extra = extra;
		updateTime(time);
	}

	/**
	 * Returns the name that identifies the statistic. You may use one of the
	 * STAT_* constants defined in this class.
	 * 
	 * @return the name
	 */
	public Name getName() {
		return name;
	}

	/**
	 * Sets the name that identifies the statistic. You may use one of the
	 * STAT_* constants defined in this class.
	 * 
	 * @param name the name
	 */
	public void setName(Name name) {
		this.name = name;
	}

	/**
	 * Returns the value. If the name is one of the STAT_* constants, see its
	 * documentation at the top of this page about the value.
	 * 
	 * @return the value
	 */
	public long getValue() {
		return value;
	}

	/**
	 * Sets the value. If you set the name to one of the STAT_* constants, see
	 * its documentation at the top of this page about the value.
	 * 
	 * @param value the value
	 */
	public void setValue(long value) {
		this.value = value;
	}

	/**
	 * Returns the time when the value was assessed as a unix time in
	 * milliseconds.
	 * 
	 * @return the time
	 */
	public long getUtcTime() {
		return utcTime;
	}

	/**
	 * Sets the time when the value was assessed as a unix time in milliseconds.
	 * This is used for DataAccessObjects. Users should call {@link
	 * #updateTime(ZonedDateTime) updateTime()} instead.
	 * 
	 * @param utcTime the time
	 */
	public void setUtcTime(long utcTime) {
		this.utcTime = utcTime;
	}
	
	/**
	 * Returns the time when the value was assessed.
	 * 
	 * @return the time
	 */
	public ZonedDateTime getIsoTime() {
		return isoTime;
	}

	/**
	 * Sets the time when the value was assessed. This field is stored in the
	 * database as an ISO date/time string. It is used for DataAccessObjects.
	 * Users should call {@link #updateTime(ZonedDateTime) updateTime()} instead.
	 * 
	 * @param isoTime the time
	 */
	public void setIsoTime(ZonedDateTime isoTime) {
		this.isoTime = isoTime;
	}

	/**
	 * Sets the time when the value was assessed. This method sets the fields
	 * "utcTime" and "isoTime", which store the time in the database as a unix
	 * time in milliseconds and an ISO date/time string respectively.
	 * 
	 * @param time the time
	 */
	public void updateTime(ZonedDateTime time) {
		utcTime = time.toInstant().toEpochMilli();
		isoTime = time;
	}

	/**
	 * Returns the extra details. This may be defined depending on the type of
	 * statistic. The default is null.
	 * 
	 * <p>If the name is one of the STAT_* constants, see its documentation at
	 * the top of this page about the content of the extra field.</p>
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
	 * <p>If you set the name to one of the STAT_* constants, see its
	 * documentation at the top of this page about the content of the extra
	 * field.</p>
	 * 
	 * @param extra the extra details
	 */
	public void setExtra(String extra) {
		this.extra = extra;
	}
}
