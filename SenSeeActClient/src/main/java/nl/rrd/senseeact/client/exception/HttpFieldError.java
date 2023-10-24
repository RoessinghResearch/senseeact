package nl.rrd.senseeact.client.exception;

import nl.rrd.utils.json.JsonObject;

/**
 * This class defines an error in the user input for a specified field.
 * 
 * @author Dennis Hofs (RRD)
 */
public class HttpFieldError extends JsonObject {
	private String field = null;
	private String message = null;

	/**
	 * Constructs a new empty field error.
	 */
	public HttpFieldError() {
	}
	
	/**
	 * Constructs a new HTTP field error without an error code and message.
	 * 
	 * @param field the field name
	 */
	public HttpFieldError(String field) {
		this.field = field;
	}

	/**
	 * Constructs a new HTTP field error without an error code.
	 * 
	 * @param field the field name
	 * @param message the error message (can be an empty string or null)
	 */
	public HttpFieldError(String field, String message) {
		this.field = field;
		this.message = message;
	}

	/**
	 * Returns the field name.
	 * 
	 * @return the field name
	 */
	public String getField() {
		return field;
	}

	/**
	 * Sets the field name.
	 * 
	 * @param field the field name
	 */
	public void setField(String field) {
		this.field = field;
	}

	/**
	 * Returns the error message.
	 * 
	 * @return the error message (can be an empty string or null)
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the error message.
	 * 
	 * @param message the error message (can be an empty string or null)
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
