package nl.rrd.senseeact.service.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.DateTimeFromIsoDateTimeDeserializer;
import nl.rrd.utils.json.IsoDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class PublicMfaRecord {
	private String id;
	private String type;
	@JsonDeserialize(using= DateTimeFromIsoDateTimeDeserializer.class)
	@JsonSerialize(using= IsoDateTimeSerializer.class)
	private ZonedDateTime created;
	private Map<String,Object> data = new LinkedHashMap<>();

	public static PublicMfaRecord fromMfaRecord(MfaRecord record) {
		PublicMfaRecord result = new PublicMfaRecord();
		result.id = record.getId();
		result.type = record.getType();
		result.created = record.getCreated();
		result.data = record.getPublicPairData();
		return result;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ZonedDateTime getCreated() {
		return created;
	}

	public void setCreated(ZonedDateTime created) {
		this.created = created;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}
}
