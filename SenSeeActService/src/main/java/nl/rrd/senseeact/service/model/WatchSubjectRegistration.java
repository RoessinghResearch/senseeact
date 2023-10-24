package nl.rrd.senseeact.service.model;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;
import nl.rrd.senseeact.client.model.SubjectEvent;
import nl.rrd.senseeact.dao.AbstractDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.util.ArrayList;
import java.util.List;

public class WatchSubjectRegistration extends AbstractDatabaseObject {
	
	@DatabaseField(value=DatabaseType.STRING)
	private String user;
	
	@DatabaseField(value=DatabaseType.STRING)
	private String project;
	
	@DatabaseField(value=DatabaseType.LONG, index=true)
	private long lastWatchTime;
	
	// JSON code for eventList
	@DatabaseField(value=DatabaseType.TEXT)
	private String events;
	
	private List<SubjectEvent> eventList = new ArrayList<>();

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
	 * Returns the project code where subjects should be watched.
	 * 
	 * @return the project code where subjects should be watched
	 */
	public String getProject() {
		return project;
	}

	/**
	 * Sets the project code where subjects should be watched.
	 * 
	 * @param project the project code where subjects should be watched
	 */
	public void setProject(String project) {
		this.project = project;
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

	/**
	 * Returns the JSON code for the events. This method is used for the DAO.
	 * Users can call {@link #getEventList() getEventList()}.
	 * 
	 * @return the JSON code for the events
	 */
	public String getEvents() {
		return JsonMapper.generate(eventList);
	}

	/**
	 * Sets the JSON code for the events. This method is used for the DAO. Users
	 * can call {@link #setEventList(List) setEventList()}.
	 * 
	 * @param events the JSON code for the events
	 * @throws ParseException if the JSON code is invalid
	 */
	public void setEvents(String events) throws ParseException {
		this.eventList = JsonMapper.parse(events,
				new TypeReference<List<SubjectEvent>>() {});
	}

	/**
	 * Returns the list of events that occurred for this registration.
	 * 
	 * @return the list of events that occurred for this registration
	 */
	public List<SubjectEvent> getEventList() {
		return eventList;
	}

	/**
	 * Sets the list of events that occurred for this registration.
	 * 
	 * @param eventList the list of events that occurred for this registration
	 */
	public void setEventList(List<SubjectEvent> eventList) {
		this.eventList = eventList;
	}
}
