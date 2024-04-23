package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.dao.BaseDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

public class MobileWakeRequest extends BaseDatabaseObject {
	@DatabaseField(value= DatabaseType.STRING, index=true)
	private String user;

	@DatabaseField(value=DatabaseType.STRING)
	private String deviceId;

	@DatabaseField(value=DatabaseType.STRING)
	private String fcmToken;

	@DatabaseField(value=DatabaseType.INT)
	private int interval;

	/**
	 * Returns the user ID.
	 *
	 * @return the user ID
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the user ID.
	 *
	 * @param user the user ID
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Returns the device ID.
	 *
	 * @return the device ID
	 */
	public String getDeviceId() {
		return deviceId;
	}

	/**
	 * Sets the device ID.
	 *
	 * @param deviceId the device ID
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	/**
	 * Returns the client token for Firebase Cloud Messaging.
	 *
	 * @return the client token for Firebase Cloud Messaging
	 */
	public String getFcmToken() {
		return fcmToken;
	}

	/**
	 * Sets the client token for Firebase Cloud Messaging.
	 *
	 * @param fcmToken the client token for Firebase Cloud Messaging
	 */
	public void setFcmToken(String fcmToken) {
		this.fcmToken = fcmToken;
	}

	/**
	 * Returns the interval in seconds when wake requests should be sent to the
	 * mobile device.
	 *
	 * @return the interval in seconds
	 */
	public int getInterval() {
		return interval;
	}

	/**
	 * Sets the interval in seconds when wake requests should be sent to the
	 * mobile device.
	 *
	 * @param interval the interval in seconds
	 */
	public void setInterval(int interval) {
		this.interval = interval;
	}
}
