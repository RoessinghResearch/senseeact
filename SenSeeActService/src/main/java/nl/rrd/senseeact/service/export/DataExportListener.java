package nl.rrd.senseeact.service.export;

import java.time.ZonedDateTime;

public interface DataExportListener {
	/**
	 * Called when the status has changed to {@link DataExportStatus#COMPLETED
	 * COMPLETED} or {@link DataExportStatus#FAILED FAILED}.
	 *
	 * @param exporter the data exporter
	 * @param status the new status
	 */
	void onStatusChange(DataExporter exporter, DataExportStatus status);

	/**
	 * Called when the export progress has changed (step, total and
	 * status message).
	 *
	 * @param exporter the data exporter
	 * @param step the current step
	 * @param total the total number of steps, or 0 if the progress is not known
	 * @param statusMessage a status message or null
	 */
	void onUpdateProgress(DataExporter exporter, int step, int total,
			String statusMessage);

	/**
	 * Called when a new log message occurs.
	 *
	 * @param exporter the data exporter
	 * @param time the current time
	 * @param message the message
	 */
	void onLogMessage(DataExporter exporter, ZonedDateTime time,
			String message);
}
