package nl.rrd.senseeact.client.model.sample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserInteractionEvent extends UTCSample {
	public enum Type {
		RESUME_ACTIVITY,
		PAUSE_ACTIVITY,
		RESUME_FRAGMENT,
		PAUSE_FRAGMENT,
		BACK_PRESSED,
		BUTTON_CLICK,
		STARTUP,
		SHUTDOWN
	}

	@DatabaseField(value=DatabaseType.STRING)
	private Type type;

	@DatabaseField(value=DatabaseType.TEXT)
	private String jsonData = null;

	public UserInteractionEvent() {
	}

	public UserInteractionEvent(String user, ZonedDateTime tzTime) {
		super(user, tzTime);
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getJsonData() {
		return jsonData;
	}

	public void setJsonData(String jsonData) {
		this.jsonData = jsonData;
	}

	public Map<String,String> toDataMap() {
		if (jsonData == null || jsonData.length() == 0)
			return new LinkedHashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(jsonData,
					new TypeReference<LinkedHashMap<String,String>>() {});
		} catch (IOException ex) {
			throw new RuntimeException("Can't parse JSON data: " +
					ex.getMessage(), ex);
		}
	}

	public void writeDataMap(Map<String,String> data) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			this.jsonData = mapper.writeValueAsString(data);
		} catch (JsonProcessingException ex) {
			throw new RuntimeException("Can't convert string map to JSON: " +
					ex.getMessage(), ex);
		}
	}
}
