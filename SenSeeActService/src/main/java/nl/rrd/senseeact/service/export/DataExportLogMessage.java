package nl.rrd.senseeact.service.export;

import java.time.ZonedDateTime;

public class DataExportLogMessage {
	private ZonedDateTime time;
	private String message;

	public DataExportLogMessage(ZonedDateTime time, String message) {
		this.time = time;
		this.message = message;
	}

	public ZonedDateTime getTime() {
		return time;
	}

	public void setTime(ZonedDateTime time) {
		this.time = time;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
