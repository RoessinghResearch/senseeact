package nl.rrd.senseeact.client.model;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonObject;

public class SubjectEvent extends JsonObject {
	public enum Type {
		/**
		 * {@link SubjectEvent SubjectEvent}
		 */
		ADDED,

		/**
		 * {@link SubjectEvent SubjectEvent}
		 */
		REMOVED,

		/**
		 * {@link ProfileUpdated SubjectEvent.ProfileUpdated}
		 */
		PROFILE_UPDATED
	}
	
	private Type type;
	private String user;
	
	public SubjectEvent() {
	}
	
	public SubjectEvent(Type type, String user) {
		this.type = type;
		this.user = user;
	}
	
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
	
	public static SubjectEvent parse(String json) throws ParseException {
		ObjectMapper mapper = new ObjectMapper();
		try {
			Map<?,?> map = mapper.readValue(json, Map.class);
			return parse(map);
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
	
	public static SubjectEvent parse(Object jsonObj) throws ParseException {
		if (!(jsonObj instanceof Map)) {
			throw new ParseException("Parsed JSON object is not a Map: " +
					jsonObj);
		}
		Map<?,?> map = (Map<?,?>)jsonObj;
		ObjectMapper mapper = new ObjectMapper();
		try {
			Type type = mapper.convertValue(map.get("type"), Type.class);
			switch (type) {
			case ADDED:
			case REMOVED:
				return mapper.convertValue(map, SubjectEvent.class);
			case PROFILE_UPDATED:
				return mapper.convertValue(map, ProfileUpdated.class);
			default:
				throw new ParseException("Unsupported event type: " + type);
			}
		} catch (IllegalArgumentException ex) {
			throw new ParseException(
					"Can't convert parsed JSON object to SubjectEvent: " +
					ex.getMessage(), ex);
		}
	}
	
	public static class ProfileUpdated extends SubjectEvent {
		private User oldProfile;
		private User newProfile;
		
		public ProfileUpdated() {
		}
		
		public ProfileUpdated(String user, User oldProfile, User newProfile) {
			super(Type.PROFILE_UPDATED, user);
			this.oldProfile = oldProfile;
			this.newProfile = newProfile;
		}
		
		public User getOldProfile() {
			return oldProfile;
		}

		public void setOldProfile(User oldProfile) {
			this.oldProfile = oldProfile;
		}

		public User getNewProfile() {
			return newProfile;
		}

		public void setNewProfile(User newProfile) {
			this.newProfile = newProfile;
		}
	}
}
