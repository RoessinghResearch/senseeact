package nl.rrd.senseeact.client.model.sample;

import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.ZonedDateTime;

/**
 * Sample with an signed byte value (-128..127).
 * 
 * @author Dennis Hofs (RRD)
 */
public class ByteSample extends NumericSample<Byte> {
	@DatabaseField(value=DatabaseType.BYTE)
	private Byte value;

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects. Users
	 * should not call this.
	 */
	public ByteSample() {
	}

	/**
	 * Constructs a new sample at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 * 
	 * @param user the user (user ID)
	 * @param tzTime the time
	 * @param value the sample value (-128..127)
	 */
	public ByteSample(String user, ZonedDateTime tzTime, byte value) {
		super(user, tzTime);
		this.value = value;
	}

	@Override
	public Byte getValue() {
		return value;
	}

	@Override
	public void setValue(Byte value) {
		this.value = value;
	}

	@Override
	public void setIntValue(int value) {
		this.value = (byte)value;
	}
}
