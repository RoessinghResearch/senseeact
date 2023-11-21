package nl.rrd.senseeact.service.export;

public enum DataExportStatus {
	/**
	 * The data exporter has not started yet.
	 */
	IDLE,

	/**
	 * The data exporter is currently running.
	 */
	RUNNING,

	/**
	 * The data export has been completed.
	 */
	COMPLETED,

	/**
	 * The data exporter stopped with an error.
	 */
	FAILED,

	/**
	 * The data export has been cancelled.
	 */
	CANCELLED
}
