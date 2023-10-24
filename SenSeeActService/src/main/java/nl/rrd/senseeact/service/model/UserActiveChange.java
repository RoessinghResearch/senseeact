package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.client.model.sample.UTCSample;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.ZonedDateTime;

public class UserActiveChange extends UTCSample {
	@DatabaseField(value=DatabaseType.BYTE)
	private boolean active;

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public UserActiveChange() {
	}
	
	/**
	 * Constructs a new sample at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 * 
	 * @param user the user (user ID)
	 * @param tzTime the time
	 * @param active true if the user was activated, false if the user was
	 * deactivated
	 */
	public UserActiveChange(String user, ZonedDateTime tzTime, boolean active) {
		super(user, tzTime);
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
