package nl.rrd.senseeact.dao;

public class DatabaseFieldException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private String field;

	public DatabaseFieldException(String message, String field) {
		super(message);
		this.field = field;
	}

	public DatabaseFieldException(String message, String field,
			Throwable cause) {
		super(message, cause);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
