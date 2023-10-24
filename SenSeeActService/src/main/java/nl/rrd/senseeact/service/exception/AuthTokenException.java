package nl.rrd.senseeact.service.exception;

public class AuthTokenException extends Exception {
	public AuthTokenException(String message) {
		super(message);
	}

	public AuthTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
