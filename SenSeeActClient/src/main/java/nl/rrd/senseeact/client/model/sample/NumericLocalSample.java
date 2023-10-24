package nl.rrd.senseeact.client.model.sample;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Sample with a numeric value and a local timestamp.
 * 
 * @author Dennis Hofs (RRD)
 */
public abstract class NumericLocalSample<T extends Number>
		extends LocalTimeSample {

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects. Users
	 * should not call this.
	 */
	public NumericLocalSample() {
	}

	/**
	 * Constructs a new sample at the specified time.
	 *
	 * @param user the user (user ID)
	 * @param time the time
	 */
	public NumericLocalSample(String user, LocalDateTime time) {
		super(user, time);
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
