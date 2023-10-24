package nl.rrd.senseeact.client.model.sample;

import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.ZonedDateTime;

/**
 * Sample with a float value.
 * 
 * @author Dennis Hofs (RRD)
 */
public class FloatSample extends NumericSample<Float> {
	@DatabaseField(value=DatabaseType.FLOAT)
	private Float value;

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects. Users
	 * should not call this.
	 */
	public FloatSample() {
	}

	/**
	 * Constructs a new sample at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 * 
	 * @param user the user (user ID)
	 * @param tzTime the time
	 * @param value the sample value
	 */
	public FloatSample(String user, ZonedDateTime tzTime, float value) {
		super(user, tzTime);
		this.value = value;
	}

	@Override
	public Float getValue() {
		return value;
	}

	@Override
	public void setValue(Float value) {
		this.value = value;
	}

	@Override
	public void setIntValue(int value) {
		this.value = (float)value;
	}
}
