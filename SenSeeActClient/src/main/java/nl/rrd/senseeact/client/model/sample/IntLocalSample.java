package nl.rrd.senseeact.client.model.sample;

import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.LocalDateTime;

public class IntLocalSample extends NumericLocalSample<Integer> {
	@DatabaseField(value=DatabaseType.INT)
	private Integer value;

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public IntLocalSample() {
	}

	/**
	 * Constructs a new sample at the specified time.
	 *
	 * @param user the user (user ID)
	 * @param time the time
	 */
	public IntLocalSample(String user, LocalDateTime time, int value) {
		super(user, time);
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
