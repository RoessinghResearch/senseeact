package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class TokenExpirationDeserializer extends JsonDeserializer<Integer> {
	@Override
	public Integer deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		String s = p.getValueAsString().toLowerCase();
		if (s.equals("never"))
			return null;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException ex) {
			throw new JsonParseException(p, "Invalid int value: " + s,
					p.getCurrentLocation(), ex);
		}
	}
}
