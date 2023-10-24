package nl.rrd.senseeact.client.model;

import nl.rrd.utils.json.JsonObject;

/**
 * This class is used when a web service wants to return a value or null. This
 * is to ensure that the Spring Framework returns a valid JSON string. Normally
 * if a Spring method returns null, it results in an empty response.
 * 
 * @author Dennis Hofs (RRD)
 *
 * @param <T> the value type
 */
public class NullableResponse<T> extends JsonObject {
	private T value = null;

	/**
	 * Constructs a new response with value null. This default constructor is
	 * needed for JSON serialization.
	 */
	public NullableResponse() {
	}
	
	/**
	 * Constructs a new response with the specified value.
	 * 
	 * @param value the value or null
	 */
	public NullableResponse(T value) {
		this.value = value;
	}

	/**
	 * Returns the value. This can be null.
	 * 
	 * @return the value or null
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Sets the value. This can be null.
	 * 
	 * @param value the value or null
	 */
	public void setValue(T value) {
		this.value = value;
	}
}
