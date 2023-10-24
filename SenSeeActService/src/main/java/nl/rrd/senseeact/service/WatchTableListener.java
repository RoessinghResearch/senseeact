package nl.rrd.senseeact.service;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpClient;
import nl.rrd.utils.http.HttpClientException;
import nl.rrd.utils.json.JsonMapper;
import nl.rrd.utils.schedule.Job;
import nl.rrd.utils.schedule.SerialJobRunner;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseAction;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.dao.listener.DatabaseActionListener;
import nl.rrd.senseeact.dao.listener.DatabaseListenerRepository;
import nl.rrd.senseeact.service.model.WatchTableRegistration;
import nl.rrd.senseeact.service.model.WatchTableRegistrationTable;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

public class WatchTableListener implements DatabaseActionListener {
	/**
	 * A watch table registration without any callback URLs can be automatically
	 * removed if it hasn't been watched for this number of minutes.
	 */
	public static final int REMOVE_AFTER_WATCH_MINUTES = 60;

	/**
	 * A callback URL in a watch table registration can be removed if the
	 * callback failed for at least 24 hours with at least 5 attempts.
	 */
	public static final int REMOVE_AFTER_FAILED_CALLBACK_HOURS = 24;
	public static final int REMOVE_AFTER_FAILED_CALLBACK_COUNT = 5;

	private static final Object LOCK = new Object();

	private WatchTableRegistration registration;

	private final Object lock = new Object();
	private Object currentWatch = null;
	private SerialJobRunner jobRunner = new SerialJobRunner();

	public WatchTableListener(WatchTableRegistration registration) {
		this.registration = registration;
	}
	
	public WatchTableRegistration getRegistration() {
		return registration;
	}
	
	public Object getLock() {
		return lock;
	}
	
	public void setCurrentWatch(Object currentWatch) {
		synchronized (lock) {
			this.currentWatch = currentWatch;
			lock.notifyAll();
		}
	}
	
	public Object getCurrentWatch() {
		synchronized (lock) {
			return currentWatch;
		}
	}
	
	public List<String> getTriggeredSubjects() {
		synchronized (lock) {
			return new ArrayList<>(registration.getTriggeredSubjectsList());
		}
	}
	
	public void clearTriggeredSubjects()
			throws DatabaseException, IOException {
		synchronized (lock) {
			if (registration.getTriggeredSubjectsList().isEmpty())
				return;
			registration.getTriggeredSubjectsList().clear();
			DatabaseLoader dbLoader = DatabaseLoader.getInstance();
			DatabaseConnection conn = dbLoader.openConnection();
			try {
				Database authDb = dbLoader.initAuthDatabase(conn);
				authDb.update(WatchTableRegistrationTable.NAME, registration);
			} finally {
				conn.close();
			}
		}
	}

	@Override
	public void onAddDatabaseActions(String database, String table,
			List<DatabaseAction> actions) {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		if (!table.equals(registration.getTable()))
			return;
		Set<String> subjects = findMatchingSubjects(actions);
		List<String> triggeredClone;
		synchronized (lock) {
			boolean changed = false;
			List<String> triggered = registration.getTriggeredSubjectsList();
			for (String subject : subjects) {
				if (!triggered.contains(subject)) {
					triggered.add(subject);
					changed = true;
				}
			}
			if (changed) {
				Collections.sort(triggered);
				lock.notifyAll();
				DatabaseLoader dbLoader = DatabaseLoader.getInstance();
				DatabaseConnection conn = null;
				try {
					conn = dbLoader.openConnection();
					Database authDb = dbLoader.initAuthDatabase(conn);
					authDb.update(WatchTableRegistrationTable.NAME, registration);
				} catch (DatabaseException ex) {
					logger.error("Database error while saving triggered subjects: " +
							ex.getMessage(), ex);
				} catch (IOException ex) {
					logger.error("Communication error while saving triggered subjects: " +
							ex.getMessage(), ex);
				} finally {
					if (conn != null)
						conn.close();
				}
			}
			triggeredClone = new ArrayList<>(triggered);
		}
		if (registration.getCallbackUrl() != null &&
				!triggeredClone.isEmpty()) {
			new Thread(() -> startCallback(triggeredClone)).start();
		}
	}

	private void startCallback(List<String> triggeredSubjects) {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		Map<String,Object> data = new LinkedHashMap<>();
		data.put("project", registration.getProject());
		data.put("table", registration.getTable());
		data.put("subjects", triggeredSubjects);
		try (HttpClient client = new HttpClient(
				registration.getCallbackUrl())) {
			client.setMethod("POST")
					.writeJson(data)
					.readString();
			logger.info("Sent callback for watch project {}, table {}, subject {}: triggered subjects = {}",
					registration.getProject(), registration.getTable(),
					registration.getSubject(), triggeredSubjects);
			jobRunner.postJob(new OnCallbackJob(
					(authDb, projectDb) -> onCallbackSuccess(authDb,
							triggeredSubjects)),
					null);
		} catch (HttpClientException | IOException ex) {
			logger.info("Callback failed for watch project {}, table {}, subject {}: triggered subjects = {}",
					registration.getProject(), registration.getTable(),
					registration.getSubject(), triggeredSubjects);
			jobRunner.postJob(new OnCallbackJob((authDb, projectDb) ->
					onCallbackFailed(authDb, projectDb, ex)), null);
		}
	}

	private void onCallbackSuccess(Database authDb,
			List<String> triggeredSubjects) throws DatabaseException {
		synchronized (lock) {
			registration.setCallbackFailCount(0);
			registration.setCallbackFailStart(0);
			registration.getTriggeredSubjectsList().removeAll(triggeredSubjects);
			authDb.update(WatchTableRegistrationTable.NAME, registration);
		}
	}

	private void onCallbackFailed(Database authDb, Database projectDb,
			Exception ex) throws DatabaseException {
		if (isCallbackExpiredError(ex)) {
			removeRegistration(authDb, projectDb, this);
			return;
		}
		synchronized (lock) {
			ZonedDateTime now = DateTimeUtils.nowMs();
			if (registration.getCallbackFailCount() == 0) {
				registration.setCallbackFailCount(1);
				registration.setCallbackFailStart(
						now.toInstant().toEpochMilli());
			} else {
				registration.setCallbackFailCount(
						registration.getCallbackFailCount() + 1);
			}
			authDb.update(WatchTableRegistrationTable.NAME, registration);
		}
		cleanRegistrations(authDb, projectDb);
	}

	private boolean isCallbackExpiredError(Exception ex) {
		if (!(ex instanceof HttpClientException))
			return false;
		HttpClientException httpEx = (HttpClientException)ex;
		if (httpEx.getStatusCode() != 404)
			return false;
		String content = httpEx.getErrorContent().trim();
		if (content.isEmpty())
			return false;
		Map<String,Object> map;
		try {
			map = JsonMapper.parse(content, new TypeReference<>() {});
		} catch (ParseException parseEx) {
			return false;
		}
		if (!map.containsKey("error"))
			return false;
		Object value = map.get("error");
		if (!(value instanceof String))
			return false;
		String strValue = (String)value;
		return strValue.equalsIgnoreCase("callback_expired");
	}

	private Set<String> findMatchingSubjects(List<DatabaseAction> actions) {
		if (registration.getSubject() == null) {
			Set<String> result = new HashSet<>();
			for (DatabaseAction action : actions) {
				result.add(action.getUser());
			}
			return result;
		}
		for (DatabaseAction action : actions) {
			if (action.getUser().equals(registration.getSubject()))
				return Collections.singleton(action.getUser());
		}
		return Collections.emptySet();
	}

	public static void initListeners(String project, Database authDb,
			Database projectDb) throws DatabaseException {
		synchronized (LOCK) {
			Logger logger = AppComponents.getLogger(
					WatchTableListener.class.getSimpleName());
			cleanRegistrations(authDb, projectDb);
			WatchTableRegistrationTable table =
					new WatchTableRegistrationTable();
			DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("project", project)
			);
			List<WatchTableRegistration> registrations = authDb.select(
					table, criteria, 0, null);
			DatabaseListenerRepository repository =
					DatabaseListenerRepository.getInstance();
			for (WatchTableRegistration registration : registrations) {
				logger.info("Start watch project {}, table {}, subject {}",
						registration.getProject(), registration.getTable(),
						registration.getSubject());
				repository.addDatabaseActionListener(projectDb.getName(),
						new WatchTableListener(registration));
			}
		}
	}
	
	private static void cleanRegistrations(Database authDb, Database projectDb)
			throws DatabaseException {
		synchronized (LOCK) {
			Logger logger = AppComponents.getLogger(
					WatchTableListener.class.getSimpleName());
			ZonedDateTime now = DateTimeUtils.nowMs();
			long minWatchTime = now.minusMinutes(REMOVE_AFTER_WATCH_MINUTES)
					.toInstant().toEpochMilli();
			long minFailTime = now.minusHours(
					REMOVE_AFTER_FAILED_CALLBACK_HOURS).toInstant()
					.toEpochMilli();
			DatabaseListenerRepository repository =
					DatabaseListenerRepository.getInstance();
			List<DatabaseActionListener> listeners =
					repository.getDatabaseActionListeners(projectDb.getName());
			for (DatabaseActionListener listener : listeners) {
				if (!(listener instanceof WatchTableListener))
					continue;
				WatchTableListener watchListener = (WatchTableListener)listener;
				WatchTableRegistration reg = watchListener.registration;
				if (canRemoveRegistration(reg, now)) {
					logger.info("Autoremove watch project {}, table {}, subject {}",
							reg.getProject(), reg.getTable(), reg.getSubject());
					repository.removeDatabaseActionListener(projectDb.getName(),
							watchListener);
				}
			}
			DatabaseCriteria callbackCriteria = new DatabaseCriteria.And(
				new DatabaseCriteria.NotEqual("callbackUrl", (String)null),
				new DatabaseCriteria.GreaterEqual("callbackFailCount",
						REMOVE_AFTER_FAILED_CALLBACK_COUNT),
				new DatabaseCriteria.LessThan("callbackFailStart", minFailTime)
			);
			DatabaseCriteria watchCriteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("callbackUrl", (String)null),
				new DatabaseCriteria.LessThan("lastWatchTime", minWatchTime)
			);
			DatabaseCriteria criteria = new DatabaseCriteria.Or(
					callbackCriteria,
					watchCriteria
			);
			authDb.delete(new WatchTableRegistrationTable(), criteria);
		}
	}

	private static boolean canRemoveRegistration(WatchTableRegistration reg,
			ZonedDateTime now) {
		if (reg.getCallbackUrl() != null)
			return canRemoveCallbackRegistration(reg, now);
		else
			return canRemoveWatchRegistration(reg, now);
	}

	private static boolean canRemoveCallbackRegistration(
			WatchTableRegistration reg, ZonedDateTime now) {
		long minFailTime = now.minusHours(REMOVE_AFTER_FAILED_CALLBACK_HOURS)
				.toInstant().toEpochMilli();
		return reg.getCallbackFailCount() >=
				REMOVE_AFTER_FAILED_CALLBACK_COUNT &&
				reg.getCallbackFailStart() < minFailTime;
	}

	private static boolean canRemoveWatchRegistration(
			WatchTableRegistration reg, ZonedDateTime now) {
		long minWatchTime = now.minusMinutes(REMOVE_AFTER_WATCH_MINUTES)
				.toInstant().toEpochMilli();
		return reg.getLastWatchTime() < minWatchTime;
	}
	
	public static boolean setRegistrationWatchTime(Database authDb,
			Database projectDb, WatchTableRegistration reg)
			throws DatabaseException {
		synchronized (LOCK) {
			ZonedDateTime now = DateTimeUtils.nowMs();
			WatchTableListener listener = findListener(projectDb, reg.getId());
			if (listener == null)
				return false;
			reg.setLastWatchTime(now.toInstant().toEpochMilli());
			authDb.update(WatchTableRegistrationTable.NAME, reg);
			listener.registration.setLastWatchTime(
					now.toInstant().toEpochMilli());
			return true;
		}
	}

	public static String addRegistration(Database authDb, Database projectDb,
			String user, String project, String table, String callbackUrl,
			boolean reset, String subject) throws DatabaseException {
		synchronized (LOCK) {
			cleanRegistrations(authDb, projectDb);
			WatchTableListener listener = findListener(projectDb, user, table,
					subject, callbackUrl);
			ZonedDateTime now = DateTimeUtils.nowMs();
			WatchTableRegistration reg;
			if (listener != null) {
				reg = listener.registration;
				listener.setCurrentWatch(null);
				reg.setLastWatchTime(now.toInstant().toEpochMilli());
				if (reset)
					reg.getTriggeredSubjectsList().clear();
				authDb.update(WatchTableRegistrationTable.NAME, reg);
			} else {
				reg = new WatchTableRegistration();
				reg.setUser(user);
				reg.setProject(project);
				reg.setTable(table);
				reg.setSubject(subject);
				reg.setCallbackUrl(callbackUrl);
				reg.setLastWatchTime(now.toInstant().toEpochMilli());
				authDb.insert(WatchTableRegistrationTable.NAME, reg);
				DatabaseListenerRepository repository =
						DatabaseListenerRepository.getInstance();
				repository.addDatabaseActionListener(projectDb.getName(),
						new WatchTableListener(reg));
			}
			return reg.getId();
		}
	}
	
	public static void removeRegistration(Database authDb, Database projectDb,
			WatchTableListener listener) throws DatabaseException {
		synchronized (LOCK) {
			Logger logger = AppComponents.getLogger(
					WatchTableListener.class.getSimpleName());
			listener.setCurrentWatch(null);
			WatchTableRegistration reg = listener.registration;
			authDb.delete(WatchTableRegistrationTable.NAME, reg);
			DatabaseListenerRepository repository =
					DatabaseListenerRepository.getInstance();
			logger.info("Remove watch project {}, table {}, subject {}",
					reg.getProject(), reg.getTable(), reg.getSubject());
			repository.removeDatabaseActionListener(projectDb.getName(),
					listener);
		}
	}
	
	private static WatchTableListener findListener(Database projectDb,
			String user, String table, String subject, String callbackUrl) {
		synchronized (LOCK) {
			DatabaseListenerRepository repository =
					DatabaseListenerRepository.getInstance();
			List<DatabaseActionListener> listeners =
					repository.getDatabaseActionListeners(projectDb.getName());
			for (DatabaseActionListener listener : listeners) {
				if (!(listener instanceof WatchTableListener))
					continue;
				WatchTableListener watchListener = (WatchTableListener)listener;
				WatchTableRegistration reg = watchListener.registration;
				if (!reg.getUser().equals(user))
					continue;
				if (!reg.getTable().equals(table))
					continue;
				if ((reg.getSubject() == null) != (subject == null))
					continue;
				if (reg.getSubject() != null &&
						!reg.getSubject().equals(subject)) {
					continue;
				}
				if ((reg.getCallbackUrl() == null) != (callbackUrl == null))
					continue;
				if (reg.getCallbackUrl() != null &&
						!reg.getCallbackUrl().equals(callbackUrl)) {
					continue;
				}
				return watchListener;
			}
			return null;
		}
	}
	
	public static WatchTableListener findListener(Database projectDb,
			String regId) {
		synchronized (LOCK) {
			DatabaseListenerRepository repository =
					DatabaseListenerRepository.getInstance();
			List<DatabaseActionListener> listeners =
					repository.getDatabaseActionListeners(projectDb.getName());
			for (DatabaseActionListener listener : listeners) {
				if (!(listener instanceof WatchTableListener))
					continue;
				WatchTableListener watchListener = (WatchTableListener)listener;
				if (watchListener.registration.getId().equals(regId))
					return watchListener;
			}
			return null;
		}
	}

	private class OnCallbackJob implements Job {
		private OnCallbackRunner runner;

		public OnCallbackJob(OnCallbackRunner runner) {
			this.runner = runner;
		}

		@Override
		public void run() {
			Logger logger = AppComponents.getLogger(
					WatchTableListener.class.getSimpleName());
			DatabaseLoader dbLoader = DatabaseLoader.getInstance();
			DatabaseConnection dbConn = null;
			try {
				dbConn = dbLoader.openConnection();
				Database authDb = dbLoader.initAuthDatabase(dbConn);
				Database projectDb = dbLoader.initProjectDatabase(dbConn,
						registration.getProject());
				runner.run(authDb, projectDb);
			} catch (DatabaseException | IOException ex) {
				logger.error("Database error: " + ex.getMessage(), ex);
			} finally {
				if (dbConn != null)
					dbConn.close();
			}
		}

		@Override
		public void cancel() {
		}
	}

	private interface OnCallbackRunner {
		void run(Database authDb, Database projectDb) throws DatabaseException;
	}

}
