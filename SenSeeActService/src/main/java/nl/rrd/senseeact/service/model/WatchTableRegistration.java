package nl.rrd.senseeact.service.model;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.senseeact.dao.BaseDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WatchTableRegistration extends BaseDatabaseObject {
	
	@DatabaseField(value=DatabaseType.STRING)
	private String user;
	
	@DatabaseField(value=DatabaseType.STRING)
	private String project;
	
	@DatabaseField(value=DatabaseType.STRING)
	private String table;

	@DatabaseField(value=DatabaseType.STRING)
	private String subject;

	@DatabaseField(value=DatabaseType.TEXT)
	private String callbackUrl = null;
	
	@DatabaseField(value=DatabaseType.LONG, index=true)
	private long lastWatchTime;

	@DatabaseField(value=DatabaseType.LONG)
	private long callbackFailStart = 0;

	@DatabaseField(value=DatabaseType.INT, index=true)
	private int callbackFailCount = 0;
	
	// JSON code for List<String>
	@DatabaseField(value=DatabaseType.TEXT)
	private String triggeredSubjects;
	
	private List<String> triggeredSubjectsList = new ArrayList<>();

	/**
	 * Returns the user ID of the user who made the registration.
	 * 
	 * @return the user ID of the user who made the registration
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the user ID of the user who made the registration
	 * 
	 * @param user the user ID of the user who made the registration
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Returns the project code that contains the watched table.
	 * 
	 * @return the project code that contains the watched table
	 */
	public String getProject() {
		return project;
	}

	/**
	 * Sets the project code that contains the watched table.
	 * 
	 * @param project the project code that contains the watched table
	 */
	public void setProject(String project) {
		this.project = project;
	}

	/**
	 * Returns the name of the watched table.
	 * 
	 * @return the name of the watched table
	 */
	public String getTable() {
		return table;
	}

	/**
	 * Sets the name of the watched table.
	 * 
	 * @param table the name of the watched table
	 */
	public void setTable(String table) {
		this.table = table;
	}

	/**
	 * Returns the user ID of the subject user that is watched. It returns null
	 * if any subject is watched.
	 * 
	 * @return the user ID of the watched subject or null
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * Sets the user ID of the subject user that is watched. You can set it to
	 * null to watch any subject.
	 * 
	 * @param subject the user ID of the watched subject or null
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getCallbackUrl() {
		return callbackUrl;
	}

	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}

	/**
	 * Returns the time when the last watch call was made for this registration.
	 * Registrations that are not watched anymore can be automatically removed.
	 * 
	 * @return the time when the last watch call was made for this registration
	 */
	public long getLastWatchTime() {
		return lastWatchTime;
	}

	/**
	 * Sets the time when the last watch call was made for this registration.
	 * Registrations that are not watched anymore can be automatically removed.
	 * 
	 * @param lastWatchTime the time when the last watch call was made for this
	 * registration
	 */
	public void setLastWatchTime(long lastWatchTime) {
		this.lastWatchTime = lastWatchTime;
	}

	public long getCallbackFailStart() {
		return callbackFailStart;
	}

	public void setCallbackFailStart(long callbackFailStart) {
		this.callbackFailStart = callbackFailStart;
	}

	public int getCallbackFailCount() {
		return callbackFailCount;
	}

	public void setCallbackFailCount(int callbackFailCount) {
		this.callbackFailCount = callbackFailCount;
	}

	/**
	 * Returns the JSON code for the list of triggered subjects. This method is
	 * used for DAO. Users can call {@link #getTriggeredSubjectsList()
	 * getTriggeredSubjectsList()}.
	 * 
	 * @return the JSON code for the list of triggered subjects
	 */
	public String getTriggeredSubjects() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(triggeredSubjectsList);
		} catch (JsonProcessingException ex) {
			throw new RuntimeException("Can't write JSON string: " +
					ex.getMessage(), ex);
		}
	}

	/**
	 * Sets the JSON code for the list of triggered subjects. This method is
	 * used for DAO. Users can call {@link #setTriggeredSubjectsList(List)
	 * setTriggeredSubjectsList()}.
	 * 
	 * @param triggeredSubjects the JSON code for the list of triggered subjects
	 * @throws ParseException if an error occurs while parsing the JSON code
	 */
	public void setTriggeredSubjects(String triggeredSubjects)
			throws ParseException {
		ObjectMapper mapper = new ObjectMapper();
		try {
			triggeredSubjectsList = mapper.readValue(triggeredSubjects,
					new TypeReference<List<String>>() {});
		} catch (JsonMappingException ex) {
			throw new ParseException("Can't map JSON string to object: " +
					ex.getMessage(), ex);
		} catch (JsonParseException ex) {
			throw new ParseException("Can't parse JSON string: " +
					ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new ParseException("Can't read JSON string: " +
					ex.getMessage(), ex);
		}
	}

	/**
	 * Returns the list of triggered subjects for this registration.
	 * 
	 * @return the list of triggered subjects for this registration
	 */
	public List<String> getTriggeredSubjectsList() {
		return triggeredSubjectsList;
	}

	/**
	 * Sets the list of triggered subjects for this registration.
	 * 
	 * @param triggeredSubjectsList the list of triggered subjects for this
	 * registration
	 */
	public void setTriggeredSubjectsList(List<String> triggeredSubjectsList) {
		this.triggeredSubjectsList = triggeredSubjectsList;
	}
}
