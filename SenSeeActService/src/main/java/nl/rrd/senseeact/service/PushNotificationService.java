package nl.rrd.senseeact.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import nl.rrd.utils.AppComponent;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;
import nl.rrd.utils.json.JsonObject;
import nl.rrd.senseeact.client.SenSeeActClient;
import nl.rrd.senseeact.client.SyncTableRestriction;
import nl.rrd.senseeact.client.model.PushMessageData;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseAction;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.senseeact.service.model.SyncPushRegistration;
import nl.rrd.senseeact.service.model.SyncPushRegistrationTable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

@AppComponent
public class PushNotificationService {
	private static final String SERVICE_NAME =
			PushNotificationService.class.getSimpleName();

	private Logger logger;
	private final Object lock = new Object();
	private boolean stopped = false;

	private Map<DatabaseTableUserKey,List<SyncPushRegistration>> registrations =
			new LinkedHashMap<>();
	private List<PushUpdate> pendingUpdates = new ArrayList<>();
	
	public void startService() {
		logger = AppComponents.getLogger(SERVICE_NAME);
		logger.info("Start " + SERVICE_NAME);
		if (FirebaseApp.getApps().isEmpty()) {
			FirebaseOptions options;
			try {
				options = FirebaseOptions.builder()
						.setCredentials(GoogleCredentials.getApplicationDefault())
						.build();
			} catch (IOException ex) {
				logger.error("Failed to read Google credentials: " +
						ex.getMessage(), ex);
				return;
			}
			FirebaseApp.initializeApp(options);
		}
		new Thread(this::runServiceThread).start();
	}
	
	public void stopService() {
		synchronized (lock) {
			if (stopped)
				return;
			stopped = true;
			registrations.clear();
			lock.notifyAll();
		}
		logger.info("Stop " + SERVICE_NAME);
	}
	
	private void runServiceThread() {
		List<SyncPushRegistration> regs = readPushRegistrations();
		if (regs == null)
			return;
		for (SyncPushRegistration reg : regs) {
			addRegistration(reg);
			if (stopped)
				return;
		}
		while (!stopped) {
			PushUpdate update;
			synchronized (lock) {
				while (!stopped && pendingUpdates.isEmpty()) {
					try {
						lock.wait();
					} catch (InterruptedException ex) {
						throw new RuntimeException("Thread interrupted", ex);
					}
				}
				if (stopped)
					return;
				update = pendingUpdates.remove(0);
			}
			if (!pushUpdate(update)) {
				synchronized (lock) {
					if (stopped)
						return;
					pendingUpdates.add(0, update);
					if (!sleep(10000))
						return;
				}
			}
		}
	}
	
	private List<SyncPushRegistration> readPushRegistrations() {
		while (true) {
			Exception exception;
			try {
				return readPushRegistrationsSingle();
			} catch (IOException | DatabaseException ex) {
				exception = ex;
			}
			synchronized (lock) {
				if (stopped)
					return null;
				logger.error("Can't read push registrations: " +
						exception.getMessage(), exception);
				if (!sleep(10000))
					return null;
			}
		}
	}
	
	private boolean sleep(int ms) {
		synchronized (lock) {
			long now = System.currentTimeMillis();
			long end = now + ms;
			while (!stopped && now < end) {
				try {
					lock.wait(end - now);
				} catch (InterruptedException ex) {
					throw new RuntimeException("Thread interrupted", ex);
				}
				now = System.currentTimeMillis();
			}
			return !stopped;
		}
	}
	
	private List<SyncPushRegistration> readPushRegistrationsSingle()
			throws IOException, DatabaseException {
		DatabaseLoader dbLoader = DatabaseLoader.getInstance();
		DatabaseConnection dbConn = dbLoader.openConnection();
		try {
			Database authDb = dbLoader.initAuthDatabase(dbConn);
			return authDb.select(new SyncPushRegistrationTable(), null, 0,
					null);
		} finally {
			dbConn.close();
		}
	}
	
	/**
	 * Adds a sync push registration. If the service is stopped, then this
	 * method has no effect. If the registration refers to a non-existing
	 * project, then this method tries to remove the registration from the
	 * database.
	 * 
	 * @param registration the registration
	 */
	public void addRegistration(SyncPushRegistration registration) {
		synchronized (lock) {
			if (stopped)
				return;
			ProjectRepository projects = AppComponents.get(
					ProjectRepository.class);
			BaseProject project = projects.findProjectByCode(
					registration.getProject());
			if (project == null) {
				logger.info(String.format(
						"Remove registration for non-existing project \"%s\"",
						registration.getProject()) + ": " + registration);
				tryRemoveRegistrationFromDb(registration);
				return;
			}
			List<? extends DatabaseTableDef<?>> projectTables =
					project.getDatabaseTables();
			SyncTableRestriction restriction =
					registration.toSyncReadRestriction();
			for (DatabaseTableDef<?> projectTable : projectTables) {
				if (restriction.matchesTable(projectTable.getName()))
					addRegistration(registration, projectTable.getName());
			}
		}
	}
	
	/**
	 * Adds a sync push registration that should be triggered for the specified
	 * table.
	 * 
	 * @param registration the registration
	 */
	private void addRegistration(SyncPushRegistration registration,
			String table) {
		DatabaseTableUserKey key = new DatabaseTableUserKey(
				registration.getDatabase(), table, registration.getUser());
		List<SyncPushRegistration> list = registrations.computeIfAbsent(key,
				k -> new ArrayList<>());
		list.removeIf(other -> other.getId().equals(registration.getId()));
		list.add(registration);
	}
	
	/**
	 * Removes the registrations for the specified user and project.
	 * 
	 * @param user the user
	 * @param project the project
	 */
	public void removeUserProject(String user, String project) {
		synchronized (lock) {
			if (stopped)
				return;
			String dbName = DatabaseLoader.getProjectDatabaseName(project);
			Set<DatabaseTableUserKey> keySet = new HashSet<>(
					registrations.keySet());
			for (DatabaseTableUserKey key : keySet) {
				if (key.database.equals(dbName) && key.user.equals(user)) {
					registrations.remove(key);
				}
			}
		}
	}

	private void removeRegistration(DatabaseTableUserKey key,
			SyncPushRegistration reg) {
		synchronized (lock) {
			if (stopped)
				return;
			List<SyncPushRegistration> regs = registrations.get(key);
			if (regs != null) {
				regs.remove(reg);
				if (regs.isEmpty())
					registrations.remove(key);
			}
		}
		tryRemoveRegistrationFromDb(reg);
	}

	private void tryRemoveRegistrationFromDb(SyncPushRegistration reg) {
		try {
			removeRegistrationFromDb(reg);
		} catch (DatabaseException ex) {
			logger.error("Database error: " + ex.getMessage(), ex);
		} catch (IOException ex) {
			logger.error("Connection error: " + ex.getMessage(), ex);
		}
	}

	private void removeRegistrationFromDb(SyncPushRegistration reg)
			throws DatabaseException, IOException {
		DatabaseLoader dbLoader = DatabaseLoader.getInstance();
		DatabaseConnection dbConn = dbLoader.openConnection();
		try {
			Database db = dbLoader.initAuthDatabase(dbConn);
			db.delete(SyncPushRegistrationTable.NAME, reg);
		} finally {
			dbConn.close();
		}
	}
	
	public void removeRegistrations(String database, String user,
			String deviceId) {
		synchronized (lock) {
			if (stopped)
				return;
			Set<DatabaseTableUserKey> keySet = new HashSet<>(
					registrations.keySet());
			for (DatabaseTableUserKey key : keySet) {
				if (!key.database.equals(database) || !key.user.equals(user))
					continue;
				List<SyncPushRegistration> regs = registrations.get(key);
				removeDevice(regs, deviceId);
				if (regs.isEmpty())
					registrations.remove(key);
			}
		}
	}
	
	private void removeDevice(List<SyncPushRegistration> regs,
			String deviceId) {
		regs.removeIf(reg -> reg.getDeviceId().equals(deviceId));
	}

	public void onAddDatabaseActions(String project, String database,
			String table, List<DatabaseAction> actions) {
		Boolean isUserTable = null;
		List<String> users = new ArrayList<>();
		for (DatabaseAction action : actions) {
			isUserTable = action.getUser() != null;
			List<String> excludeSources = new ArrayList<>();
			excludeSources.add(SenSeeActClient.SYNC_REMOTE_ID);
			if (isUserTable)
				excludeSources.add(action.getUser());
			if (excludeSources.contains(action.getSource()))
				continue;
			if (isUserTable && !users.contains(action.getUser()))
				users.add(action.getUser());
		}
		synchronized (lock) {
			if (stopped || isUserTable == null)
				return;
			List<PushUpdate> updates = new ArrayList<>();
			if (isUserTable) {
				for (String user : users) {
					updates.add(new PushUpdate(database, table, user, project));
				}
			} else {
				updates.add(new PushUpdate(database, table, null, project));
			}
			for (PushUpdate update : updates) {
				if (!pendingUpdates.contains(update)) {
					pendingUpdates.add(update);
					lock.notifyAll();
				}
			}
		}
	}
	
	private boolean pushUpdate(PushUpdate update) {
		List<SyncPushRegistration> updateRegs = new ArrayList<>();
		synchronized (lock) {
			if (stopped)
				return false;
			for (DatabaseTableUserKey key : registrations.keySet()) {
				if (key.matchesUpdate(update))
					updateRegs.addAll(registrations.get(key));
			}
			if (updateRegs.isEmpty())
				return true;
		}
		PushMessageData data = new PushMessageData(update.project, update.user,
				update.table);
		Map<String,String> dataMap;
		try {
			dataMap = JsonMapper.convert(data, new TypeReference<>() {});
		} catch (ParseException ex) {
			logger.error("Failed to create push message data map: " +
					ex.getMessage(), ex);
			return false;
		}
		logger.info("Sending push message: " + dataMap);
		for (SyncPushRegistration reg : updateRegs) {
			DatabaseTableUserKey key = new DatabaseTableUserKey(
					reg.getDatabase(), update.table, reg.getUser());
			AndroidConfig androidConfig = AndroidConfig.builder()
					.setPriority(AndroidConfig.Priority.HIGH)
					.build();
			Message.Builder builder = Message.builder()
					.putAllData(dataMap)
					.setToken(reg.getFcmToken())
					.setAndroidConfig(androidConfig);
			Message message = builder.build();
			try {
				String response = FirebaseMessaging.getInstance().send(message);
				logger.info("Sent push message to " + reg.getFcmToken());
				logger.info("Response: " + response);
			} catch (FirebaseMessagingException ex) {
				MessagingErrorCode error = ex.getMessagingErrorCode();
				if (error == MessagingErrorCode.UNREGISTERED ||
						error == MessagingErrorCode.SENDER_ID_MISMATCH) {
					unregisterOnError(key, reg, error);
				} else {
					logger.error("Failed to send push message to " +
							reg.getFcmToken() + ": " + ex.getMessage(), ex);
					return false;
				}
			}
		}
		return true;
	}

	private void unregisterOnError(DatabaseTableUserKey key,
			SyncPushRegistration reg, MessagingErrorCode error) {
		logger.info(String.format("Delete FCM registration on error %s: ",
				error) + reg);
		removeRegistration(key, reg);
	}

	/**
	 * This key is used in the "registrations" map.
	 */
	private static class DatabaseTableUserKey {
		public String database;
		public String table;
		public String user;
		
		public DatabaseTableUserKey(String database, String table,
				String user) {
			this.database = database;
			this.table = table;
			this.user = user;
		}

		public boolean matchesUpdate(PushUpdate update) {
			if (!database.equals(update.database))
				return false;
			if (!table.equals(update.table))
				return false;
			if (update.user != null && !user.equals(update.user))
				return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int result = database.hashCode();
			result += 31 * table.hashCode();
			result += 31 * user.hashCode();
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (obj.getClass() != getClass())
				return false;
			DatabaseTableUserKey other = (DatabaseTableUserKey)obj;
			if (!database.equals(other.database))
				return false;
			if (!table.equals(other.table))
				return false;
			if (!user.equals(other.user))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return JsonObject.toString(this);
		}
	}

	/**
	 * An instance of this class is created when a database action occurs.
	 */
	private static class PushUpdate {
		public String database;
		public String table;
		public String user;
		public String project;

		/**
		 * Constructs a new instance.
		 *
		 * @param database the name of the database where the action occurred
		 * @param table the table name
		 * @param user the user or null if it is a database action in a table
		 * without a user field
		 * @param project the project code
		 */
		public PushUpdate(String database, String table, String user,
				String project) {
			this.database = database;
			this.table = table;
			this.user = user;
			this.project = project;
		}
		
		@Override
		public int hashCode() {
			int result = database.hashCode();
			result += 31 * table.hashCode();
			if (user != null)
				result += 31 * user.hashCode();
			result += 31 * project.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (obj.getClass() != getClass())
				return false;
			PushUpdate other = (PushUpdate)obj;
			if (!database.equals(other.database))
				return false;
			if (!table.equals(other.table))
				return false;
			if ((user == null) != (other.user == null))
				return false;
			if (user != null && !user.equals(other.user))
				return false;
			if (!project.equals(other.project))
				return false;
			return true;
		}
	}
}
