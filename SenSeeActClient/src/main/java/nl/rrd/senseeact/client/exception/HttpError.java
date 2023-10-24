package nl.rrd.senseeact.client.exception;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.utils.json.JsonObject;

/**
 * A HTTP error can be included as a JSON string in the content of a HTTP error
 * response. It has an optional error code. The core error codes are defined as
 * constants in {@link ErrorCode ErrorCode}.
 * 
 * @author Dennis Hofs (RRD)
 */
public class HttpError extends JsonObject {
	private String code = null;
	private String message = "";
	private List<HttpFieldError> fieldErrors = new ArrayList<>();

	/**
	 * Constructs a new empty error.
	 */
	public HttpError() {
	}
	
	/**
	 * Constructs a new error without an error code.
	 * 
	 * @param message the error message
	 */
	public HttpError(String message) {
		this.message = message;
	}
	
	/**
	 * Constructs a new error.
	 * 
	 * @param code the error code or null
	 * @param message the error message
	 */
	public HttpError(String code, String message) {
		this.code = code;
		this.message = message;
	}

	/**
	 * Returns the error code.
	 * 
	 * @return the error code or null
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Sets the error code.
	 * 
	 * @param code the error code or null
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Returns the error message.
	 * 
	 * @return the error message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the error message.
	 * 
	 * @param message the error message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Returns errors for fields where invalid user input was provided.
	 * 
	 * @return the field errors
	 */
	public List<HttpFieldError> getFieldErrors() {
		return fieldErrors;
	}

	/**
	 * Sets errors for fields where invalid user input was provided.
	 * 
	 * @param fieldErrors the field errors
	 */
	public void setFieldErrors(List<HttpFieldError> fieldErrors) {
		this.fieldErrors = fieldErrors;
	}

	/**
	 * Adds an error for a field where invalid user input was provided.
	 * 
	 * @param error the field error
	 */
	public void addFieldError(HttpFieldError error) {
		fieldErrors.add(error);
	}
}
