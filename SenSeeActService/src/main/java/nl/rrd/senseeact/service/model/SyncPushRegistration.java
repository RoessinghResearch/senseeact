package nl.rrd.senseeact.service.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rrd.senseeact.client.SyncTableRestriction;
import nl.rrd.senseeact.dao.AbstractDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.io.IOException;

public class SyncPushRegistration extends AbstractDatabaseObject {
	
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String user;
	
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String project;
	
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String database;
	
	@DatabaseField(value=DatabaseType.STRING)
	private String deviceId;
	
	@DatabaseField(value=DatabaseType.STRING)
	private String fcmToken;
	
	@DatabaseField(value=DatabaseType.TEXT)
	private String restrictions;

	/**
	 * Returns the user ID of the user for which push notifications should be
	 * sent.
	 * 
	 * @return the user ID of the user for which push notifications should be
	 * sent
	 */
	public String getUser() {
		return user;
	}
	
	/**
	 * Sets the user ID of the user for which push notifications should be sent.
	 * 
	 * @param user the user ID of the user for which push notifications should
	 * be sent
	 */
	public void setUser(String user) {
		this.user = user;
	}
	
	/**
	 * Returns the project code.
	 * 
	 * @return the project code
	 */
	public String getProject() {
		return project;
	}

	/**
	 * Sets the project code.
	 * 
	 * @param project the project code
	 */
	public void setProject(String project) {
		this.project = project;
	}

	/**
	 * Returns the database name.
	 * 
	 * @return the database name
	 */
	public String getDatabase() {
		return database;
	}

	/**
	 * Sets the database name.
	 * 
	 * @param database the database name
	 */
	public void setDatabase(String database) {
		this.database = database;
	}

	/**
	 * Returns the ID of the device to which the push message is sent.
	 * 
	 * @return the ID of the device to which the push message is sent
	 */
	public String getDeviceId() {
		return deviceId;
	}

	/**
	 * Sets the ID of the device to which the push message is sent.
	 * 
	 * @param deviceId the ID of the device to which the push message is sent
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
	 * Returns a JSON string for {@link SyncTableRestriction
	 * SyncTableRestriction}.
	 * 
	 * @return a JSON string for {@link SyncTableRestriction
	 * SyncTableRestriction}
	 */
	public String getRestrictions() {
		return restrictions;
	}
	
	/**
	 * Sets a JSON string for {@link SyncTableRestriction SyncTableRestriction}.
	 * 
	 * @param restrictions a JSON string for {@link SyncTableRestriction
	 * SyncTableRestriction}
	 */
	public void setRestrictions(String restrictions) {
		this.restrictions = restrictions;
	}
	
	public SyncTableRestriction toSyncReadRestriction() {
		ObjectMapper mapper = new ObjectMapper();
		Exception exception;
		try {
			return mapper.readValue(restrictions, SyncTableRestriction.class);
		} catch (IOException ex) {
			exception = ex;
		}
		throw new RuntimeException("Can't parse JSON string for " +
				SyncTableRestriction.class.getSimpleName() + ": " +
				exception.getMessage(), exception);
	}
}
