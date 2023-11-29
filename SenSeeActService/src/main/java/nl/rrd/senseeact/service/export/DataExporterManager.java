package nl.rrd.senseeact.service.export;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import nl.rrd.senseeact.client.SenSeeActClient;
import nl.rrd.senseeact.client.exception.SenSeeActClientException;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.service.Configuration;
import nl.rrd.senseeact.service.DatabaseLoader;
import nl.rrd.senseeact.service.model.DataExportRecord;
import nl.rrd.senseeact.service.model.DataExportTable;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.exception.TaskException;
import nl.rrd.utils.http.HttpClientException;
import nl.rrd.utils.io.FileUtils;
import nl.rrd.utils.schedule.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DataExporterManager {
	private static final int MAX_AVAILABLE_DAYS = 7;

	// map from export ID to DataExporter
	private Map<String,DataExporter> exporters = new HashMap<>();

	private static final Object LOCK = new Object();
	private boolean closed = false;
	private boolean initialized = false;
	private String cleanTaskId = null;

	public void startExport(String project, User user)
			throws SenSeeActClientException, HttpClientException,
			ParseException, DatabaseException, IOException {
		if (!waitInit())
			return;
		ZonedDateTime now = DateTimeUtils.nowMs(user.toTimeZone());
		DataExportRecord export = new DataExportRecord(user.getUserid(), now);
		export.setProject(project);
		export.setStatus(DataExportStatus.IDLE.name());
		StartExportRunner runner = new StartExportRunner(export);
		runWithAuthDb(runner);
		if (runner.error != null)
			throwSenSeeActException(runner.error);
	}

	public void deleteExport(User user, String exportId)
			throws DatabaseException, IOException {
		if (!waitInit())
			return;
		runWithAuthDb(authDb -> doDeleteExport(authDb, user, exportId));
	}

	private void throwSenSeeActException(Exception ex)
			throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		if (ex instanceof SenSeeActClientException)
			throw (SenSeeActClientException)ex;
		else if (ex instanceof HttpClientException)
			throw (HttpClientException)ex;
		else if (ex instanceof ParseException)
			throw (ParseException) ex;
		else if (ex instanceof IOException)
			throw (IOException) ex;
	}

	private boolean waitInit() {
		synchronized (LOCK) {
			while (!closed && !initialized) {
				try {
					LOCK.wait();
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}
			return !closed && initialized;
		}
	}

	private void doDeleteExport(Database authDb, User user, String exportId)
			throws DatabaseException {
		DataExportTable table = new DataExportTable();
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("id", exportId),
				new DatabaseCriteria.Equal("user", user.getUserid())
		);
		DataExportRecord record = authDb.selectOne(table, criteria, null);
		if (record == null)
			return;
		deleteExport(authDb, record);
	}

	@PostConstruct
	public void init() {
		new Thread(this::runInit).start();
	}

	private void runInit() {
		String serviceName = getClass().getSimpleName();
		Logger logger = AppComponents.getLogger(serviceName);
		logger.info("Start " + serviceName);
		while (!closed) {
			try {
				runWithAuthDb(this::initDb);
				onInitDb();
				return;
			} catch (DatabaseException | IOException ex) {
				if (closed)
					return;
				logger.error("Database error: " + ex.getMessage());
			}
			wait(10000);
			if (closed)
				return;
			logger.info("Retry init");
		}
	}

	private void wait(int ms) {
		long now = System.currentTimeMillis();
		long end = now + ms;
		synchronized (LOCK) {
			while (!closed && now < end) {
				try {
					LOCK.wait(end - now);
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
				now = System.currentTimeMillis();
			}
		}
	}

	private void initDb(Database authDb) throws DatabaseException {
		DataExportTable table = new DataExportTable();
		List<DataExportRecord> exports = authDb.select(table, null, 0, null);
		for (DataExportRecord export : exports) {
			if (closed)
				return;
			ZonedDateTime now = DateTimeUtils.nowMs();
			ZonedDateTime minTime = now.minusDays(MAX_AVAILABLE_DAYS);
			if (export.toDateTime().isBefore(minTime)) {
				// export expired
				deleteExport(authDb, export);
				continue;
			}
			if (DataExportStatus.COMPLETED.name().equals(export.getStatus()) ||
					DataExportStatus.FAILED.name().equals(export.getStatus()) ||
					DataExportStatus.CANCELLED.name().equals(export.getStatus())) {
				// export not running
				continue;
			}
			resumeExporter(authDb, export);
		}
	}

	private void onInitDb() {
		synchronized (LOCK) {
			if (closed)
				return;
			Logger logger = AppComponents.getLogger(getClass().getSimpleName());
			TaskScheduler scheduler = AppComponents.get(TaskScheduler.class);
			cleanTaskId = scheduler.generateTaskId();
			scheduler.scheduleTask(null, new CleanExportsTask(), cleanTaskId);
			logger.info("Init completed");
			initialized = true;
			LOCK.notifyAll();
		}
	}

	private void resumeExporter(Database authDb, DataExportRecord export) {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		try {
			tryStartExporter(export);
		} catch (SenSeeActClientException | HttpClientException |
				 ParseException | IOException ex) {
			logger.error("Failed to resume " + getExportLog(export) + ": " +
					ex.getMessage(), ex);
			deleteExport(authDb, export);
		}
	}

	private void doStartExporter(Database authDb, DataExportRecord export)
			throws DatabaseException, SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		authDb.insert(DataExportTable.NAME, export);
		tryStartExporter(export);
	}

	private void tryStartExporter(DataExportRecord export)
			throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		DataExporterFactory factory = AppComponents.get(
				DataExporterFactory.class);
		SenSeeActClient client = openClient(export.getUser());
		User user = client.getUser(null);
		File dir = getExportDir(export);
		ExportListener listener = new ExportListener(export);
		DataExporter exporter = factory.create(export.getProject(),
				export.getId(), user, client, dir, listener);
		synchronized (LOCK) {
			if (closed)
				return;
			logger.info("Start " + getExportLog(export));
			exporters.put(export.getId(), exporter);
			exporter.start();
		}
	}

	private void runWithAuthDb(DatabaseRunner runner) throws DatabaseException,
			IOException {
		DatabaseLoader dbLoader = DatabaseLoader.getInstance();
		DatabaseConnection dbConn = dbLoader.openConnection();
		try {
			Database authDb = dbLoader.initAuthDatabase(dbConn);
			runner.run(authDb);
		} finally {
			dbConn.close();
		}
	}

	private void tryRunWithAuthDb(DatabaseRunner runner) {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		try {
			runWithAuthDb(runner);
		} catch (DatabaseException | IOException ex) {
			logger.error("Database error: " + ex.getMessage(), ex);
		}
	}

	private SenSeeActClient openClient(String userId)
			throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		Configuration config = AppComponents.get(Configuration.class);
		String baseUrl = config.get(Configuration.BASE_URL);
		String adminEmail = config.get(Configuration.ADMIN_EMAIL);
		String adminPassword = config.get(Configuration.ADMIN_PASSWORD);
		String path = baseUrl.replaceAll("^https?://[^/]+", "");
		baseUrl = "http://localhost:8080" + path;
		SenSeeActClient client = new SenSeeActClient(baseUrl);
		client.login(adminEmail, adminPassword);
		User user = client.getUser(userId);
		client.loginAs(user.getEmail());
		return client;
	}

	private File getExportDir(DataExportRecord export) throws IOException {
		Configuration config = AppComponents.get(Configuration.class);
		String dataDir = config.get(Configuration.DATA_DIR);
		LocalDate date = export.toLocalDateTime().toLocalDate();
		String fileName = date.format(DateTimeUtils.DATE_FORMAT) + "-" +
				export.getProject() + "-data-export";
		File exportDir = new File(dataDir, "data-exports" + File.separator +
				export.getId() + File.separator + fileName);
		FileUtils.mkdir(exportDir);
		return exportDir;
	}

	public File getExportZip(DataExportRecord export) throws IOException {
		File exportDir = getExportDir(export);
		return new File(exportDir.getParentFile(), exportDir.getName() +
				".zip");
	}

	@PreDestroy
	public void destroy() {
		synchronized (LOCK) {
			if (closed)
				return;
			String serviceName = getClass().getSimpleName();
			Logger logger = AppComponents.getLogger(serviceName);
			logger.info("Stop " + serviceName);
			closed = true;
			LOCK.notifyAll();
			Set<String> exportIds = exporters.keySet();
			for (String exportId : exportIds) {
				DataExporter exporter = exporters.remove(exportId);
				exporter.cancel();
			}
			if (cleanTaskId != null) {
				TaskScheduler scheduler = AppComponents.get(
						TaskScheduler.class);
				scheduler.cancelTask(null, cleanTaskId);
			}
		}
	}

	private void onStatusChange(DataExportRecord export, DataExporter exporter,
			DataExportStatus status) {
		synchronized (LOCK) {
			if (closed || !exporters.containsKey(export.getId()))
				return;
			if (status == DataExportStatus.COMPLETED ||
					status == DataExportStatus.FAILED ||
					status == DataExportStatus.CANCELLED) {
				exporters.remove(export.getId());
			}
		}
		new Thread(() -> onStatusChangeThread(export, exporter, status))
				.start();
	}

	private void onStatusChangeThread(DataExportRecord export,
			DataExporter exporter, DataExportStatus status) {
		String error = null;
		if (status == DataExportStatus.COMPLETED) {
			try {
				createZipFile(export);
			} catch (IOException ex) {
				status = DataExportStatus.FAILED;
				error = "Failed to create zip file";
			}
		} else if (status == DataExportStatus.FAILED) {
			error = exporter.getError().getMessage();
		}
		final DataExportStatus finalStatus = status;
		final String finalError = error;
		tryRunWithAuthDb(authDb -> {
			export.setStatus(finalStatus.name());
			export.setError(finalError);
			authDb.update(DataExportTable.NAME, export);
		});
	}

	private void createZipFile(DataExportRecord export) throws IOException {
		File zipFile = getExportZip(export);
		try (FileOutputStream out = new FileOutputStream(zipFile)) {
			try (ZipOutputStream zip = new ZipOutputStream(out)) {
				createZipFile(export, zip, out);
			}
		}
	}

	private void createZipFile(DataExportRecord export, ZipOutputStream zip,
			FileOutputStream out) throws IOException {
		File exportDir = getExportDir(export);
		addToZip(exportDir, exportDir.getName(), zip, out);
	}

	private void addToZip(File file, String path, ZipOutputStream zip,
			OutputStream out) throws IOException {
		if (file.isHidden())
			return;
		if (file.isDirectory()) {
			zip.putNextEntry(new ZipEntry(path + "/"));
			zip.closeEntry();
			File[] children = file.listFiles();
			for (File child : children) {
				addToZip(child, path + "/" + child.getName(), zip, out);
			}
		} else {
			zip.putNextEntry(new ZipEntry(path));
			try (FileInputStream in = new FileInputStream(file)) {
				FileUtils.copyStream(in, out, 0, null);
			}
			zip.closeEntry();
		}
	}

	private void onUpdateProgress(DataExportRecord export, int step, int total,
			String statusMessage) {
		synchronized (LOCK) {
			if (closed || !exporters.containsKey(export.getId()))
				return;
		}
		tryRunWithAuthDb(authDb -> {
			export.setStep(step);
			export.setTotal(total);
			export.setStatusMessage(statusMessage);
			authDb.update(DataExportTable.NAME, export);
		});
	}

	private void onLogMessage(DataExportRecord export, ZonedDateTime time,
			String message) {
		synchronized (LOCK) {
			if (closed || !exporters.containsKey(export.getId()))
				return;
		}
		tryRunWithAuthDb(authDb -> {
			export.getLogMessageList().add(new DataExportLogMessage(time,
					message));
			authDb.update(DataExportTable.NAME, export);
		});
	}

	private void deleteExport(Database authDb, DataExportRecord export) {
		synchronized (LOCK) {
			if (closed)
				return;
			Logger logger = AppComponents.getLogger(getClass().getSimpleName());
			logger.info("Delete " + getExportLog(export));
			DataExporter exporter = exporters.remove(export.getId());
			if (exporter != null)
				exporter.cancel();
			try {
				authDb.delete(DataExportTable.NAME, export);
			} catch (DatabaseException ex) {
				logger.error("Failed to delete export record: " +
						ex.getMessage(), ex);
			}
			try {
				File exportDir = getExportDir(export);
				FileUtils.deleteTree(exportDir.getParentFile());
			} catch (IOException ex) {
				logger.error("Failed to delete export directory: " +
						ex.getMessage(), ex);
			}
		}
	}

	private void cleanExports() {
		tryRunWithAuthDb(this::cleanExportsDb);
	}

	private void cleanExportsDb(Database authDb) throws DatabaseException {
		ZonedDateTime now = DateTimeUtils.nowMs();
		ZonedDateTime minTime = now.minusDays(MAX_AVAILABLE_DAYS);
		DataExportTable table = new DataExportTable();
		List<DataExportRecord> exports = authDb.select(table, null, 0, null);
		for (DataExportRecord export : exports) {
			if (export.toDateTime().isBefore(minTime)) {
				deleteExport(authDb, export);
			}
		}
	}

	private String getExportLog(DataExportRecord export) {
		return String.format("data export %s: project %s, user %s",
				export.getId(), export.getProject(), export.getUser());
	}

	private class ExportListener implements DataExportListener {
		private DataExportRecord export;

		public ExportListener(DataExportRecord export) {
			this.export = export;
		}

		@Override
		public void onStatusChange(DataExporter exporter,
				DataExportStatus status) {
			DataExporterManager.this.onStatusChange(export, exporter, status);
		}

		@Override
		public void onUpdateProgress(DataExporter exporter, int step, int total,
				String statusMessage) {
			DataExporterManager.this.onUpdateProgress(export, step, total,
					statusMessage);
		}

		@Override
		public void onLogMessage(DataExporter exporter, ZonedDateTime time,
				String message) {
			DataExporterManager.this.onLogMessage(export, time, message);
		}
	}

	private class CleanExportsTask extends AbstractScheduledTask {
		public CleanExportsTask() {
			LocalDate today = LocalDate.now();
			Random random = new Random();
			LocalTime time = LocalTime.of(0, random.nextInt(60));
			TaskSchedule.TimeSchedule schedule = new TaskSchedule.TimeSchedule(
					today, time);
			schedule.setRepeatDate(new DateDuration(1, DateUnit.DAY));
			schedule.setRepeatTime(new TimeDuration(1, TimeUnit.HOUR));
			setSchedule(schedule);
		}

		@Override
		public String getName() {
			return DataExporterManager.class.getSimpleName() + "." +
					getClass().getSimpleName();
		}

		@Override
		public void run(Object context, String taskId, ZonedDateTime now,
				ScheduleParams scheduleParams) throws TaskException {
			cleanExports();
		}
	}

	private interface DatabaseRunner {
		void run(Database db) throws DatabaseException;
	}

	private class StartExportRunner implements DatabaseRunner {
		private DataExportRecord export;
		private Exception error = null;

		public StartExportRunner(DataExportRecord export) {
			this.export = export;
		}

		@Override
		public void run(Database db) throws DatabaseException {
			try {
				doStartExporter(db, export);
			} catch (SenSeeActClientException | HttpClientException |
					 ParseException | IOException ex) {
				error = ex;
				deleteExport(db, export);
			}
		}
	}
}
