package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.EnumCustomStringDeserializer;
import nl.rrd.utils.json.EnumCustomStringSerializer;

@JsonSerialize(using=EnumCustomStringSerializer.class)
@JsonDeserialize(using=AccessMode.JsonDeserializer.class)
public enum AccessMode {
	R,
	W,
	RW;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

	public boolean matchesRequest(AccessMode request) {
		if (request == RW && this != RW)
			return false;
		if (request == R && this != R && this != RW)
			return false;
		if (request == W && this != W && this != RW)
			return false;
		return true;
	}

	public static AccessMode fromStringValue(String value) {
		return valueOf(value.toUpperCase());
	}

	public static class JsonDeserializer extends
			EnumCustomStringDeserializer<AccessMode> {
		public JsonDeserializer() {
			super(AccessMode.class);
		}
	}
}
