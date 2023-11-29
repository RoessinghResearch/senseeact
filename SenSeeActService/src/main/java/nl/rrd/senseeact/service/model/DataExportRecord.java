package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.client.model.sample.UTCSample;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.senseeact.service.export.DataExportLogMessage;
import nl.rrd.senseeact.service.export.DataExportStatus;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataExportRecord extends UTCSample {
	@DatabaseField(value=DatabaseType.STRING)
	private String project;
	@DatabaseField(value=DatabaseType.STRING)
	private String status;
	@DatabaseField(value=DatabaseType.INT)
	private int step = 0;
	@DatabaseField(value=DatabaseType.INT)
	private int total = 0;
	@DatabaseField(value=DatabaseType.TEXT)
	private String statusMessage = null;
	@DatabaseField(value=DatabaseType.TEXT, json=true)
	private String logMessages;

	private List<DataExportLogMessage> logMessageList = new ArrayList<>();

	@DatabaseField(value=DatabaseType.TEXT)
	private String error = null;

	@DatabaseField(value=DatabaseType.TEXT, json=true)
	private String initParams;

	private Map<String,Object> initParamsMap;

	@DatabaseField(value=DatabaseType.TEXT, json=true)
	private String progress;

	private Map<String,Object> progressMap = new LinkedHashMap<>();

	/**
	 * Constructs a new empty record. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public DataExportRecord() {
	}

	/**
	 * Constructs a new record with the specified time when the export was
	 * started. It should define the local time and location-based time zone
	 * (not an offset).
	 *
	 * @param user the user (user ID)
	 * @param tzTime the time when the export was started
	 */
	public DataExportRecord(String user, ZonedDateTime tzTime) {
		super(user, tzTime);
	}

	/**
	 * Returns the project code.
	 *
	 * @return the project code
	 */
	public String getProject() {
		return project;
	}

	/**
	 * Sets the project code.
	 *
	 * @param project the project code
	 */
	public void setProject(String project) {
		this.project = project;
	}

	/**
	 * Returns the status. This should be one of {@link DataExportStatus
	 * DataExportStatus}.
	 *
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Sets the status. This should be one of {@link DataExportStatus
	 * DataExportStatus}.
	 *
	 * @param status the status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Returns the current step. Together with "total" this indicates the
	 * progress. This is only valid if the status is {@link
	 * DataExportStatus#RUNNING RUNNING}. If the total is 0, then the progress
	 * is still unknown.
	 *
	 * @return the current step
	 */
	public int getStep() {
		return step;
	}

	/**
	 * Sets the current step. Together with "total" this indicates the progress.
	 * This is only valid if the status is {@link DataExportStatus#RUNNING
	 * RUNNING}. If the total is 0, then the progress is still unknown.
	 *
	 * @param step the current step
	 */
	public void setStep(int step) {
		this.step = step;
	}

	/**
	 * Returns the total number of steps. Together with "step" this indicates
	 * the progress. This is only valid if the status is {@link
	 * DataExportStatus#RUNNING RUNNING}. If the total is 0, then the progress
	 * is still unknown.
	 *
	 * @return the total number of steps or 0
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * Sets the total number of steps. Together with "step" this indicates the
	 * progress. This is only valid if the status is {@link
	 * DataExportStatus#RUNNING RUNNING}. If the total is 0, then the progress
	 * is still unknown.
	 *
	 * @param total the total number of steps or 0
	 */
	public void setTotal(int total) {
		this.total = total;
	}

	/**
	 * Returns the current status message. This is only valid if the status is
	 * {@link DataExportStatus#RUNNING RUNNING}. If there is no status message,
	 * this method returns null.
	 *
	 * @return the current status message or null
	 */
	public String getStatusMessage() {
		return statusMessage;
	}

	/**
	 * Sets the current status message. This is only valid if the status is
	 * {@link DataExportStatus#RUNNING RUNNING}. If there is no status message,
	 * this method returns null.
	 *
	 * @param statusMessage the current status message or null
	 */
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	/**
	 * Returns the JSON code for the log messages. This method is used for the
	 * DAO. Users can call {@link #getLogMessageList() getLogMessageList()}.
	 *
	 * @return the JSON code for the log messages
	 */
	public String getLogMessages() {
		return logMessages;
	}

	/**
	 * Sets the JSON code for the log messages. This method is used for the DAO.
	 * Users can call {@link #setLogMessageList(List) setLogMessageList()}.
	 *
	 * @param logMessages the JSON code for the log messages
	 */
	public void setLogMessages(String logMessages) {
		this.logMessages = logMessages;
	}

	/**
	 * Returns a list of log messages.
	 *
	 * @return a list of log messages
	 */
	public List<DataExportLogMessage> getLogMessageList() {
		return logMessageList;
	}

	/**
	 * Sets a list of log messages.
	 *
	 * @param logMessageList a list of log messages
	 */
	public void setLogMessageList(List<DataExportLogMessage> logMessageList) {
		this.logMessageList = logMessageList;
	}

	/**
	 * If the status is {@link DataExportStatus#FAILED FAILED}, then this method
	 * returns an error message.
	 *
	 * @return the error message
	 */
	public String getError() {
		return error;
	}

	/**
	 * Sets an error message. This should be set if the status is {@link
	 * DataExportStatus#FAILED FAILED}.
	 *
	 * @param error the error message
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * Returns the JSON code for the init parameters. This method is used for
	 * the DAO. Users can call {@link #getInitParamsMap() getInitParamsMap()}.
	 *
	 * @return the JSON code for the init parameters
	 */
	public String getInitParams() {
		return initParams;
	}

	/**
	 * Sets the JSON code for the init parameters. This method is used for the
	 * DAO. Users can call {@link #setInitParamsMap(Map) setInitParamsMap()}.
	 *
	 * @param initParams the JSON code for the init parameters
	 */
	public void setInitParams(String initParams) {
		this.initParams = initParams;
	}

	/**
	 * Returns any init parameters that are needed to restart the data exporter.
	 * This is reserved for future use.
	 *
	 * @return the init parameters
	 */
	public Map<String,Object> getInitParamsMap() {
		return initParamsMap;
	}

	/**
	 * Sets any init parameters that are needed to restart the data exporter.
	 * This is reserved for future use.
	 *
	 * @param initParamsMap the init parameters
	 */
	public void setInitParamsMap(Map<String,Object> initParamsMap) {
		this.initParamsMap = initParamsMap;
	}

	/**
	 * Returns the JSON code for current progress details. This method is used
	 * for the DAO. Users can call {@link #getProgressMap() getProgressMap()}.
	 *
	 * @return the JSON code for current progress details
	 */
	public String getProgress() {
		return progress;
	}

	/**
	 * Sets the JSON code for current progress details. This method is used for
	 * the DAO. Users can call {@link #setProgressMap(Map) setProgressMap()}.
	 *
	 * @param progress the JSON code for current progress details
	 */
	public void setProgress(String progress) {
		this.progress = progress;
	}

	/**
	 * Returns current progress details. It can be used to resume the export.
	 * This is reserved for future use.
	 *
	 * @return current progress details
	 */
	public Map<String,Object> getProgressMap() {
		return progressMap;
	}

	/**
	 * Sets current progress details. It can be used to resume the export. This
	 * is reserved for future use.
	 *
	 * @param progressMap current progress details
	 */
	public void setProgressMap(Map<String,Object> progressMap) {
		this.progressMap = progressMap;
	}
}
