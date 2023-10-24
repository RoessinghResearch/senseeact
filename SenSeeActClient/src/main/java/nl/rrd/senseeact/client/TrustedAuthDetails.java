package nl.rrd.senseeact.client;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.DateTimeFromIsoDateTimeDeserializer;
import nl.rrd.utils.json.IsoDateTimeSerializer;
import nl.rrd.utils.json.JsonObject;
import nl.rrd.utils.validation.ValidateNotNull;

import java.time.ZonedDateTime;

public class TrustedAuthDetails extends JsonObject {
	@ValidateNotNull
	private String user;
	
	@ValidateNotNull
	@JsonSerialize(using=IsoDateTimeSerializer.class)
	@JsonDeserialize(using=DateTimeFromIsoDateTimeDeserializer.class)
	private ZonedDateTime time;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public ZonedDateTime getTime() {
		return time;
	}

	public void setTime(ZonedDateTime time) {
		this.time = time;
	}
}
