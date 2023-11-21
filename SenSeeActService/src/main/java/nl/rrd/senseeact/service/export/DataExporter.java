package nl.rrd.senseeact.service.export;

import nl.rrd.senseeact.client.SenSeeActClient;
import nl.rrd.senseeact.client.exception.SenSeeActClientException;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpClientException;
import nl.rrd.utils.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A data exporter can download data from SenSeeAct. write it to a directory,
 * and report on the export progress.
 *
 * @author Dennis Hofs (RRD)
 */
public abstract class DataExporter {
	private String id;
	private User user;
	private SenSeeActClient client;
	private File dir;
	private DataExportListener listener;

	private DataExportStatus status = DataExportStatus.IDLE;
	private int step = 0;
	private int total = 0;
	private String statusMessage = null;
	private List<DataExportLogMessage> logMessages = new ArrayList<>();
	private Exception error = null;

	private static final Object LOCK = new Object();

	/**
	 * Constructs a new data exporter.
	 *
	 * @param id the ID (a UUID in lower case without dashes)
	 * @param user the user who is exporting data
	 * @param client the client where the user is logged in
	 * @param dir the directory where the data should be written (a directory
	 * with the same name as the exporter ID)
	 * @param listener a data export listener or null
	 */
	public DataExporter(String id, User user, SenSeeActClient client, File dir,
			DataExportListener listener) {
		this.id = id;
		this.user = user;
		this.client = client;
		this.dir = dir;
		this.listener = listener;
	}

	/**
	 * Starts the data exporter. It starts a new thread in which the data is
	 * downloaded.
	 */
	public void start() {
		synchronized (LOCK) {
			if (status != DataExportStatus.IDLE)
				return;
			status = DataExportStatus.RUNNING;
			new Thread(this::runThread).start();
		}
	}

	/**
	 * Cancels the data exporter. It closes the SenSeeAct client.
	 */
	public void cancel() {
		synchronized (LOCK) {
			if (status != DataExportStatus.IDLE &&
					status != DataExportStatus.RUNNING)
				return;
			status = DataExportStatus.CANCELLED;
			client.close();
		}
	}

	/**
	 * Returns the ID of this data exporter.
	 *
	 * @return the ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the user who is exporting data.
	 *
	 * @return the user who is exporting data
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Returns the directory where the exported data is written. This is a
	 * directory with the same name as the exporter ID.
	 *
	 * @return the directory where the exported data is written
	 */
	public File getDir() {
		return dir;
	}

	/**
	 * Returns the status.
	 *
	 * @return the status
	 */
	public DataExportStatus getStatus() {
		synchronized (LOCK) {
			return status;
		}
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
		synchronized (LOCK) {
			return step;
		}
	}

	/**
	 * Returns the total number of steps. Together with "step" this indicated
	 * the progress. This is only valid if the status is {@link
	 * DataExportStatus#RUNNING RUNNING}. If the total is 0, then the progress
	 * is still unknown.
	 *
	 * @return the total number of steps or 0
	 */
	public int getTotal() {
		synchronized (LOCK) {
			return total;
		}
	}

	/**
	 * Returns the current status message. This is only valid if the status is
	 * {@link DataExportStatus#RUNNING RUNNING}. If there is no status message,
	 * this method returns null.
	 *
	 * @return the current status message or null
	 */
	public String getStatusMessage() {
		synchronized (LOCK) {
			return statusMessage;
		}
	}

	/**
	 * Returns a list of log messages. This is only valid if the status is
	 * {@link DataExportStatus#RUNNING RUNNING}.
	 *
	 * @return the log messages
	 */
	public List<DataExportLogMessage> getLogMessages() {
		return logMessages;
	}

	/**
	 * If the status is {@link DataExportStatus#FAILED FAILED}, then this method
	 * returns the exception. This should be one of the following classes:
	 *
	 * <p><ul>
	 * <li>{@link DataExportException DataExportException}</li>
	 * <li>{@link SenSeeActClientException SenSeeActClientException}</li>
	 * <li>{@link HttpClientException HttpClientException}</li>
	 * <li>{@link ParseException ParseException}</li>
	 * <li>{@link IOException IOException}</li>
	 * </ul></p>
	 *
	 * @return the exception
	 */
	public Exception getError() {
		synchronized (LOCK) {
			return error;
		}
	}

	private void runThread() {
		try {
			tryRunThread();
		} catch (DataExportException | SenSeeActClientException |
				 HttpClientException | ParseException | IOException ex) {
			setError(ex);
		}
	}

	private void setError(Exception error) {
		synchronized (LOCK) {
			if (status != DataExportStatus.RUNNING)
				return;
			status = DataExportStatus.FAILED;
			this.error = error;
		}
		if (listener != null)
			listener.onStatusChange(this, status);
	}

	private void tryRunThread() throws DataExportException,
			SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		FileUtils.mkdir(dir);
		List<User> users = readUsers();
		readData(users);
		synchronized (LOCK) {
			if (status != DataExportStatus.RUNNING)
				return;
			status = DataExportStatus.COMPLETED;
		}
		if (listener != null)
			listener.onStatusChange(this, status);
	}

	/**
	 * Reads the users whose data should be exported.
	 *
	 * @return the users whose data should be exported
	 * @throws DataExportException if a data export error occurs
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server, or while writing data to the export directory
	 */
	protected abstract List<User> readUsers() throws DataExportException,
			SenSeeActClientException, HttpClientException, ParseException,
			IOException;

	/**
	 * Reads the data from SenSeeAct and writes it to the export directory.
	 * This method is run in a thread while the exporter is running. It should
	 * continue to download data until the exporter is cancelled. During the
	 * export it can report on the progress by calling {@link
	 * #update(int, int, String) update()} and {@link
	 * #log(ZonedDateTime, String) log()}.
	 *
	 * @param users the users
	 * @throws DataExportException if a data export error occurs
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server, or while writing data to the export directory
	 */
	protected abstract void readData(List<User> users)
			throws DataExportException, SenSeeActClientException,
			HttpClientException, ParseException, IOException;

	/**
	 * This method can be called from {@link #readData(List) readData()} to
	 * report on the progress of the export. It notifies the data export
	 * listener.
	 *
	 * @param step the current step
	 * @param total the total number of steps, or 0 if the progress is not known
	 * @param statusMessage a status message or null
	 */
	public void update(int step, int total, String statusMessage) {
		synchronized (LOCK) {
			if (status != DataExportStatus.RUNNING)
				return;
			this.step = step;
			this.total = total;
			this.statusMessage = statusMessage;
			if (listener != null)
				listener.onUpdateProgress(this, step, total, statusMessage);
		}
	}

	/**
	 * This method can be called from {@link #readData(List) readData()} to
	 * report on the progress of the export. It notifies the data export
	 * listener.
	 *
	 * @param time the current time
	 * @param message the log message
	 */
	public void log(ZonedDateTime time, String message) {
		synchronized (LOCK) {
			if (status != DataExportStatus.RUNNING)
				return;
			logMessages.add(new DataExportLogMessage(time, message));
		}
		if (listener != null)
			listener.onLogMessage(this, time, message);
	}
}
