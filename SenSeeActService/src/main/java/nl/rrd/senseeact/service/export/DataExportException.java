package nl.rrd.senseeact.service.export;

public class DataExportException extends Exception {
	public DataExportException(String message) {
		super(message);
	}

	public DataExportException(String message, Throwable cause) {
		super(message, cause);
	}
}
