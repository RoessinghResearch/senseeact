package nl.rrd.senseeact.client.model.sample;

import java.time.ZonedDateTime;

/**
 * Sample with a numeric value.
 * 
 * @author Dennis Hofs (RRD)
 */
public abstract class NumericSample<T extends Number> extends UTCSample {

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects. Users
	 * should not call this.
	 */
	public NumericSample() {
	}

	/**
	 * Constructs a new sample at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 *
	 * @param user the user (user ID)
	 * @param tzTime the time
	 */
	public NumericSample(String user, ZonedDateTime tzTime) {
		super(user, tzTime);
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	public abstract T getValue();

	/**
	 * Sets the value.
	 * 
	 * @param value the value
	 */
	public abstract void setValue(T value);

	/**
	 * Sets an int value.
	 *
	 * @param value the value
	 */
	public abstract void setIntValue(int value);
}
