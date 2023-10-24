package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.JsonObject;
import nl.rrd.utils.json.SqlDateDeserializer;
import nl.rrd.utils.json.SqlDateSerializer;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ProjectUserAccessRestriction extends JsonObject {
	private String module;
	private AccessMode accessMode;
	@JsonSerialize(using=SqlDateSerializer.class)
	@JsonDeserialize(using=SqlDateDeserializer.class)
	private LocalDate start = null;
	@JsonSerialize(using=SqlDateSerializer.class)
	@JsonDeserialize(using=SqlDateDeserializer.class)
	private LocalDate end = null;

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public AccessMode getAccessMode() {
		return accessMode;
	}

	public void setAccessMode(AccessMode accessMode) {
		this.accessMode = accessMode;
	}

	public LocalDate getStart() {
		return start;
	}

	public void setStart(LocalDate start) {
		this.start = start;
	}

	public LocalDate getEnd() {
		return end;
	}

	public void setEnd(LocalDate end) {
		this.end = end;
	}
}
