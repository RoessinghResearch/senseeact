package nl.rrd.senseeact.service;

import nl.rrd.senseeact.client.model.PerformanceStatTable;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.client.model.SystemStatTable;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.*;
import nl.rrd.senseeact.dao.listener.DatabaseActionListener;
import nl.rrd.senseeact.dao.listener.DatabaseListenerRepository;
import nl.rrd.senseeact.service.controller.AuthControllerExecution;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.UserTable;
import nl.rrd.senseeact.service.model.*;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.ReferenceParameter;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.schedule.AbstractScheduledTask;
import nl.rrd.utils.schedule.ScheduleParams;
import nl.rrd.utils.schedule.TaskSchedule;
import nl.rrd.utils.schedule.TaskScheduler;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Utility class to load the authentication database and project databases.
 * This is thread-safe.
 * 
 * @author Dennis Hofs (RRD)
 */
public class DatabaseLoader {
	private static final int MIN_KEEP_OPEN_DURATION = 300000; // milliseconds
	private static final int MAX_KEEP_OPEN_DURATION = 600000; // milliseconds
	private static final int CLEAN_INTERVAL = 60000; // milliseconds
	
	private final Object AUTH_DB_LOCK = new Object();
	private final Map<String,Object> PROJECT_DB_LOCKS = new LinkedHashMap<>();
	
	private final List<String> listeningDatabases = new ArrayList<>();
	private List<OpenDatabaseConnection> openConns = new ArrayList<>();
	
	private static final Object INSTANCE_LOCK = new Object();
	private static DatabaseLoader instance = null;
	
	private boolean closed = false;
	private String cleanTaskId;
	
	private DatabaseLoader() {
		TaskScheduler scheduler = AppComponents.get(TaskScheduler.class);
		cleanTaskId = scheduler.generateTaskId();
		scheduler.scheduleTask(null, new CleanConnectionsTask(), cleanTaskId);
	}
	
	public static DatabaseLoader getInstance() {
		synchronized (INSTANCE_LOCK) {
			if (instance == null)
				instance = new DatabaseLoader();
			return instance;
		}
	}
	
	/**
	 * Opens a connection to the database server. It enables action logging
	 * for synchronisation with a remote database. When you have completed the
	 * database operations, you should close the connection.
	 * 
	 * @return the database connection
	 * @throws IOException if the connection could not be opened
	 */
	public DatabaseConnection openConnection()
			throws IOException {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		synchronized (INSTANCE_LOCK) {
			if (closed)
				throw new IOException("DatabaseLoader closed");
			OpenDatabaseConnection openConn = findMatchingOpenConnection();
			if (openConn != null) {
				CloseListenDatabaseConnection conn =
						new CloseListenDatabaseConnection(openConn);
				openConn.dbConns.add(conn);
				logger.trace("Reuse database connection");
				return conn;
			}
		}
		DatabaseFactory dbFactory = AppComponents.getInstance()
				.getComponent(DatabaseFactory.class);
		boolean saved = false;
		DatabaseConnection baseConn = dbFactory.connect();
		logger.trace("Created new database connection");
		try {
			baseConn.setSyncEnabled(true);
			synchronized (INSTANCE_LOCK) {
				if (closed)
					throw new IOException("DatabaseLoader closed");
				OpenDatabaseConnection openConn = findMatchingOpenConnection();
				if (openConn != null) {
					CloseListenDatabaseConnection conn =
							new CloseListenDatabaseConnection(openConn);
					openConn.dbConns.add(conn);
					logger.trace("Reuse simultaneously created new database connection");
					return conn;
				}
				openConn = new OpenDatabaseConnection();
				openConn.baseConn = baseConn;
				openConn.openTime = System.currentTimeMillis();
				CloseListenDatabaseConnection conn =
						new CloseListenDatabaseConnection(openConn);
				openConn.dbConns.add(conn);
				openConns.add(openConn);
				saved = true;
				logger.trace("Saved new database connection");
				return conn;
			}
		} finally {
			if (!saved) {
				baseConn.close();
				logger.trace("Closed unsaved database connection");
			}
		}
	}
	
	/**
	 * Closes this database loader and any open database connections.
	 */
	public void close() {
		synchronized (INSTANCE_LOCK) {
			if (closed)
				return;
			closed = true;
			TaskScheduler scheduler = AppComponents.get(TaskScheduler.class);
			scheduler.cancelTask(null, cleanTaskId);
			for (OpenDatabaseConnection openConn : openConns) {
				openConn.baseConn.close();
			}
			openConns.clear();
			Logger logger = AppComponents.getLogger(getClass().getSimpleName());
			logger.info("Closed database loader and connections");
		}
	}
	
	/**
	 * Returns the authentication database. It will create, initialise or
	 * upgrade the database if needed. If no user exists, it will create the
	 * admin user. You should not call any queries that change the database
	 * structure.
	 * 
	 * @param conn the database connection
	 * @return the database
	 * @throws DatabaseException if a database error occurs
	 */
	public Database initAuthDatabase(DatabaseConnection conn)
			throws DatabaseException {
		synchronized (AUTH_DB_LOCK) {
			List<DatabaseTableDef<?>> tableDefs = getAuthDbTables();
			Configuration config = AppComponents.get(Configuration.class);
			String dbNamePrefix = config.get(Configuration.DB_NAME_PREFIX);
			Database db = conn.initDatabase(dbNamePrefix + "_auth", tableDefs,
					true);
			db.setSyncEnabled(false);
			UserCache userCache = UserCache.createInstance(db);
			int count = userCache.getCount();
			if (count == 0)
				createInitialAuthData(db);
			return db;
		}
	}

	public static List<DatabaseTableDef<?>> getAuthDbTables() {
		List<DatabaseTableDef<?>> result = new ArrayList<>();
		result.add(new UserTable());
		result.add(new GroupTable());
		result.add(new UserProjectTable());
		result.add(new GroupMemberTable());
		result.add(new ProjectUserAccessTable());
		result.add(new UserActiveChangeTable());
		result.add(new SyncPushRegistrationTable());
		result.add(new WatchSubjectRegistrationTable());
		result.add(new WatchTableRegistrationTable());
		result.add(new MobileWakeRequestTable());
		result.add(new SystemStatTable());
		result.add(new PerformanceStatTable());
		OAuthTableRepository oauthRepo = AppComponents.get(
				OAuthTableRepository.class);
		result.addAll(oauthRepo.getOAuthTables());
		ProjectUserAccessControlRepository uacRepo = AppComponents.get(
				ProjectUserAccessControlRepository.class);
		Map<String,ProjectUserAccessControl> projectAccessControlMap =
				uacRepo.getProjectMap();
		for (String project : projectAccessControlMap.keySet()) {
			ProjectUserAccessControl projectAccessControl =
					projectAccessControlMap.get(project);
			result.addAll(projectAccessControl.getTables());
		}
		return result;
	}
	
	/**
	 * This method is called on the authentication database if it doesn't have
	 * any users. It will create the admin user.
	 * 
	 * @param db the authentication database
	 * @throws DatabaseException if a database error occurs
	 */
	private void createInitialAuthData(Database db) throws DatabaseException {
		Configuration config = AppComponents.get(Configuration.class);
		User user = new User();
		user.setUserid(UUID.randomUUID().toString().toLowerCase()
				.replaceAll("-", ""));
		user.setEmail(config.get(Configuration.ADMIN_EMAIL));
		try {
			AuthControllerExecution.setPassword(user,
					config.get(Configuration.ADMIN_PASSWORD), "password",
					false);
		} catch (HttpException ex) {
			throw new RuntimeException(
					"Invalid admin password in configuration: " +
					ex.getMessage(), ex);
		}
		ZonedDateTime now = DateTimeUtils.nowMs();
		user.setCreated(now);
		user.setLastActive(now);
		user.setRole(Role.ADMIN);
		UserCache userCache = UserCache.getInstance();
		userCache.createUser(db, user);
	}
	
	/**
	 * Returns the database for the specified project. This can be null if the
	 * project doesn't use a database. This is determined by {@link
	 * BaseProject#getDatabaseTables() BaseProject.getDatabaseTables()}. It will
	 * create, initialise or upgrade the database if needed. You should not call
	 * any queries that change the database structure.
	 * 
	 * @param conn the database connection
	 * @param project the project code
	 * @return the database or null
	 * @throws DatabaseException if a database error occurs
	 */
	public Database initProjectDatabase(DatabaseConnection conn, String project)
			throws DatabaseException {
		final Object lock;
		synchronized (PROJECT_DB_LOCKS) {
			if (PROJECT_DB_LOCKS.containsKey(project)) {
				lock = PROJECT_DB_LOCKS.get(project);
			} else {
				lock = new Object();
				PROJECT_DB_LOCKS.put(project, lock);
			}
		}
		synchronized (lock) {
			String name = getProjectDatabaseName(project);
			if (name == null)
				return null;
			ProjectRepository projects = AppComponents.get(
					ProjectRepository.class);
			BaseProject baseProject = projects.findProjectByCode(project);
			List<? extends DatabaseTableDef<?>> tables =
					baseProject.getDatabaseTables();
			boolean firstInit;
			synchronized (listeningDatabases) {
				firstInit = !listeningDatabases.contains(name);
				if (firstInit) {
					DatabaseListenerRepository listeners =
							DatabaseListenerRepository.getInstance();
					listeners.addDatabaseActionListener(name,
							new ProjectDatabaseActionListener(project));
					listeningDatabases.add(name);
				}
			}
			Database db = conn.initDatabase(name, tables, true);
			if (firstInit) {
				Database authDb = initAuthDatabase(conn);
				WatchTableListener.initListeners(project, authDb, db);
				WatchSubjectListener.initListeners(project, authDb);
			}
			return db;
		}
	}
	
	/**
	 * Returns the name of the database for the specified project. This can be
	 * null if the project doesn't use a database. This is determined by {@link
	 * BaseProject#getDatabaseTables() BaseProject.getDatabaseTables()}.
	 * 
	 * @param project the project code
	 * @return the database name or null
	 */
	public static String getProjectDatabaseName(String project) {
		ProjectRepository projects = AppComponents.get(ProjectRepository.class);
		BaseProject baseProject = projects.findProjectByCode(project);
		List<? extends DatabaseTableDef<?>> tables =
				baseProject.getDatabaseTables();
		if (tables == null || tables.isEmpty())
			return null;
		Configuration config = AppComponents.get(Configuration.class);
		String dbNamePrefix = config.get(Configuration.DB_NAME_PREFIX);
		return dbNamePrefix + "_" + project + "_samples";
	}
	
	private static class ProjectDatabaseActionListener implements
	DatabaseActionListener {
		private String project;
		
		public ProjectDatabaseActionListener(String project) {
			this.project = project;
		}

		@Override
		public void onAddDatabaseActions(String database, String table,
				List<DatabaseAction> actions) {
			PushNotificationService pushService = AppComponents.get(
					PushNotificationService.class);
			pushService.onAddDatabaseActions(project, database, table, actions);
		}
	}
	
	private OpenDatabaseConnection findMatchingOpenConnection() {
		ReferenceParameter<Boolean> reusable = new ReferenceParameter<>();
		Iterator<OpenDatabaseConnection> it = openConns.iterator();
		while (it.hasNext()) {
			OpenDatabaseConnection openConn = it.next();
			if (cleanConnection(openConn, reusable)) {
				it.remove();
			} else if (reusable.get()) {
				return openConn;
			}
		}
		return null;
	}

	private void cleanConnections() {
		synchronized (INSTANCE_LOCK) {
			if (closed)
				return;
			openConns.removeIf(openConn -> cleanConnection(openConn, null));
		}
	}

	private boolean cleanConnection(OpenDatabaseConnection openConn,
			ReferenceParameter<Boolean> reusable) {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		long now = System.currentTimeMillis();
		long openMs = now - openConn.openTime;
		if (openMs > MIN_KEEP_OPEN_DURATION) {
			if (reusable != null)
				reusable.set(false);
			if (openMs > MAX_KEEP_OPEN_DURATION) {
				logger.warn("Close database connection that has been open for more than 10 minutes");
				openConn.baseConn.close();
				return true;
			}
			if (openConn.dbConns.isEmpty()) {
				openConn.baseConn.close();
				return true;
			}
		} else {
			if (reusable != null)
				reusable.set(true);
		}
		return false;
	}
	
	private void onCloseConnection(CloseListenDatabaseConnection dbConn,
			OpenDatabaseConnection openConn) {
		synchronized (INSTANCE_LOCK) {
			if (closed)
				return;
			openConn.dbConns.remove(dbConn);
			if (cleanConnection(openConn, null))
				openConns.remove(openConn);
		}
	}
	
	private class OpenDatabaseConnection {
		public long openTime;
		public DatabaseConnection baseConn;
		public List<CloseListenDatabaseConnection> dbConns = new ArrayList<>();
	}
	
	private class CloseListenDatabaseConnection extends
			DatabaseConnectionDecorator {
		private final Object LOCK = new Object();
		private boolean closed = false;
		private OpenDatabaseConnection openConn;
		
		public CloseListenDatabaseConnection(OpenDatabaseConnection openConn) {
			super(openConn.baseConn);
			this.openConn = openConn;
		}

		@Override
		public void close() {
			synchronized (LOCK) {
				if (closed)
					return;
				closed = true;
				onCloseConnection(this, openConn);
			}
		}
	}
	
	private class CleanConnectionsTask extends AbstractScheduledTask {
		public CleanConnectionsTask() {
			setSchedule(new TaskSchedule.FixedDelay(CLEAN_INTERVAL));
		}
		
		@Override
		public String getName() {
			return DatabaseLoader.class.getSimpleName() + "." +
					getClass().getSimpleName();
		}

		@Override
		public void run(Object context, String taskId, ZonedDateTime now,
				ScheduleParams scheduleParams) {
			cleanConnections();
		}
	}
}
