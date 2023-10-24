package nl.rrd.senseeact.client.exception;

/**
 * This exception is thrown when a HTTP request results in an error response.
 * 
 * @author Dennis Hofs (RRD)
 */
public class SenSeeActClientException extends Exception {
	private static final long serialVersionUID = 1L;

	private int statusCode;
	private String statusMessage;
	private HttpError httpError;

	/**
	 * Constructs a new HTTP client exception.
	 * 
	 * @param statusCode the HTTP status code
	 * @param statusMessage the status message
	 * @param httpError the error details
	 */
	public SenSeeActClientException(int statusCode, String statusMessage,
			HttpError httpError) {
		super(statusCode + " " + statusMessage + ": " +
			httpError.getMessage());
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.httpError = httpError;
	}

	/**
	 * Returns the HTTP status code.
	 * 
	 * @return the HTTP status code
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Returns the status message.
	 * 
	 * @return the status message
	 */
	public String getStatusMessage() {
		return statusMessage;
	}

	/**
	 * Returns the error details.
	 * 
	 * @return the error details
	 */
	public HttpError getHttpError() {
		return httpError;
	}
}
