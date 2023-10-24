package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class TokenExpirationSerializer extends JsonSerializer<Integer> {
	@Override
	public void serialize(Integer value, JsonGenerator gen,
			SerializerProvider serializers) throws IOException {
		if (value == null)
			gen.writeString("never");
		else
			gen.writeNumber(value);
	}
}
