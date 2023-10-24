package nl.rrd.senseeact.client.model.sample;

import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.ZonedDateTime;

/**
 * Sample with a signed 32-bit integer value.
 * 
 * @author Dennis Hofs (RRD)
 */
public class IntSample extends NumericSample<Integer> {
	@DatabaseField(value=DatabaseType.INT)
	private Integer value;

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects. Users
	 * should not call this.
	 */
	public IntSample() {
	}

	/**
	 * Constructs a new sample at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 *
	 * @param user the user (user ID)
	 * @param tzTime the time
	 * @param value the sample value
	 */
	public IntSample(String user, ZonedDateTime tzTime, int value) {
		super(user, tzTime);
		this.value = value;
	}

	@Override
	public Integer getValue() {
		return value;
	}

	@Override
	public void setValue(Integer value) {
		this.value = value;
	}

	@Override
	public void setIntValue(int value) {
		this.value = value;
	}
}
