package nl.rrd.senseeact.service;

import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.client.model.SubjectEvent;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;
import nl.rrd.senseeact.service.model.WatchSubjectRegistration;
import nl.rrd.senseeact.service.model.WatchSubjectRegistrationTable;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WatchSubjectListener implements UserListener {
	private static final String LOGTAG =
			WatchSubjectListener.class.getSimpleName();

	/**
	 * A watch subject registration can be automatically removed if it hasn't
	 * been watched for this number of minutes.
	 */
	public static final int REMOVE_AFTER_WATCH_MINUTES = 60;

	private static final Object LOCK = new Object();

	private WatchSubjectRegistration registration;

	private final Object lock = new Object();
	private Object currentWatch = null;
	private Logger logger;
	
	private User regUser = null;
	private Set<String> regActiveSubjects = null;

	public WatchSubjectListener(WatchSubjectRegistration registration) {
		this.registration = registration;
	}
	
	private void initUserSubjects(User user, Database authDb)
			throws DatabaseException {
		logger = AppComponents.getLogger(LOGTAG);
		regUser = user;
		List<User> subjects = User.findProjectUsers(registration.getProject(),
				authDb, regUser, Role.PATIENT, false);
		regActiveSubjects = new HashSet<>();
		for (User subject : subjects) {
			regActiveSubjects.add(subject.getUserid());
		}
	}

	private boolean initUserSubjects(String user, Database authDb)
			throws DatabaseException {
		UserCache userCache = UserCache.getInstance();
		User userObj = userCache.findByUserid(registration.getUser());
		if (userObj == null)
			return false;
		initUserSubjects(userObj, authDb);
		return true;
	}
	
	public WatchSubjectRegistration getRegistration() {
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
	
	public List<SubjectEvent> getSubjectEvents() {
		synchronized (lock) {
			return new ArrayList<>(registration.getEventList());
		}
	}
	
	public void clearSubjectEvents()
			throws DatabaseException, IOException {
		synchronized (lock) {
			if (registration.getEventList().isEmpty())
				return;
			registration.getEventList().clear();
			DatabaseLoader dbLoader = DatabaseLoader.getInstance();
			DatabaseConnection conn = dbLoader.openConnection();
			try {
				Database authDb = dbLoader.initAuthDatabase(conn);
				authDb.update(WatchSubjectRegistrationTable.NAME, registration);
			} finally {
				conn.close();
			}
		}
	}

	@Override
	public void userProfileUpdated(User user, User oldProfile) {
		synchronized (lock) {
			if (!regActiveSubjects.contains(user.getUserid()))
				return;
			registration.getEventList().add(new SubjectEvent.ProfileUpdated(
					user.getUserid(), oldProfile, user));
			lock.notifyAll();
			saveRegistration();
		}
	}

	@Override
	public void userRoleChanged(User user, Role oldRole) {
	}
	
	@Override
	public void userActiveChanged(User user) {
		synchronized (lock) {
			if (user.isActive() && !regActiveSubjects.contains(
					user.getUserid())) {
				DatabaseLoader dbLoader = DatabaseLoader.getInstance();
				DatabaseConnection dbConn = null;
				try {
					dbConn = dbLoader.openConnection();
					Database authDb = dbLoader.initAuthDatabase(dbConn);
					if (!isRegistrationSubject(authDb, user))
						return;
				} catch (DatabaseException ex) {
					logger.error("Database error: " + ex.getMessage(), ex);
					return;
				} catch (IOException ex) {
					logger.error("Communication error: " + ex.getMessage(), ex);
					return;
				} finally {
					if (dbConn != null)
						dbConn.close();
				}
				regActiveSubjects.add(user.getUserid());
				registration.getEventList().add(new SubjectEvent(
						SubjectEvent.Type.ADDED, user.getUserid()));
				lock.notifyAll();
				saveRegistration();
			} else if (!user.isActive() && regActiveSubjects.contains(
					user.getUserid())) {
				regActiveSubjects.remove(user.getUserid());
				registration.getEventList().add(new SubjectEvent(
						SubjectEvent.Type.REMOVED, user.getUserid()));
				lock.notifyAll();
				saveRegistration();
			}
		}
	}
	
	private boolean isRegistrationSubject(Database authDb, User user)
			throws DatabaseException {
		if (!user.isActive())
			return false;
		if (regUser.getRole() == Role.ADMIN) {
			return User.isProjectUser(authDb, registration.getProject(),
					user.getUserid(), Role.PATIENT);
		} else if (regUser.getRole() == Role.PROFESSIONAL) {
			List<User> subjects = User.findProjectUsers(
					registration.getProject(), authDb, regUser, Role.PATIENT,
					false);
			for (User subject : subjects) {
				if (subject.getUserid().equals(user.getUserid()))
					return true;
			}
			return false;
		} else {
			return user.getUserid().equals(regUser.getUserid()) &&
					User.isProjectUser(authDb, registration.getProject(),
					user.getUserid(), Role.PATIENT);
		}
	}

	@Override
	public void userAddedToProject(User user, String project, Role role) {
		synchronized (lock) {
			if (!project.equals(registration.getProject()) ||
					role != Role.PATIENT ||
					!user.isActive() ||
					regActiveSubjects.contains(user.getUserid())) {
				return;
			}
			if (regUser.getRole() == Role.PROFESSIONAL) {
				List<User> subjects;
				DatabaseLoader dbLoader = DatabaseLoader.getInstance();
				DatabaseConnection conn = null;
				try {
					conn = dbLoader.openConnection();
					Database authDb = dbLoader.initAuthDatabase(conn);
					subjects = User.findProjectUsers(project, authDb, regUser,
							Role.PATIENT, false);
				} catch (DatabaseException ex) {
					logger.error("Database error while retrieving subjects: " +
							ex.getMessage(), ex);
					return;
				} catch (IOException ex) {
					logger.error("Communication error while retrieving subjects: " +
							ex.getMessage(), ex);
					return;
				} finally {
					if (conn != null)
						conn.close();
				}
				boolean found = false;
				for (User subject : subjects) {
					if (subject.getUserid().equals(user.getUserid())) {
						found = true;
						break;
					}
				}
				if (!found)
					return;
			} else if (regUser.getRole() == Role.PATIENT) {
				if (!user.getUserid().equals(regUser.getUserid()))
					return;
			}
			regActiveSubjects.add(user.getUserid());
			registration.getEventList().add(new SubjectEvent(
					SubjectEvent.Type.ADDED, user.getUserid()));
			lock.notifyAll();
			saveRegistration();
		}
	}

	@Override
	public void userRemovedFromProject(User user, String project, Role role) {
		synchronized (lock) {
			if (!project.equals(registration.getProject()) ||
					role != Role.PATIENT ||
					!regActiveSubjects.contains(user.getUserid())) {
				return;
			}
			regActiveSubjects.remove(user.getUserid());
			registration.getEventList().add(new SubjectEvent(
					SubjectEvent.Type.REMOVED, user.getUserid()));
			lock.notifyAll();
			saveRegistration();
		}
	}

	@Override
	public void userAddedAsSubject(User user, User profUser) {
		synchronized (lock) {
			if (!regUser.getUserid().equals(profUser.getUserid()) ||
					!user.isActive() ||
					regActiveSubjects.contains(user.getUserid())) {
				return;
			}
			DatabaseLoader dbLoader = DatabaseLoader.getInstance();
			DatabaseConnection conn = null;
			try {
				conn = dbLoader.openConnection();
				Database authDb = dbLoader.initAuthDatabase(conn);
				if (!User.isProjectUser(authDb, registration.getProject(),
						user.getUserid(), Role.PATIENT)) {
					return;
				}
			} catch (DatabaseException ex) {
				logger.error("Database error while retrieving subjects: " +
						ex.getMessage(), ex);
				return;
			} catch (IOException ex) {
				logger.error("Communication error while retrieving subjects: " +
						ex.getMessage(), ex);
				return;
			} finally {
				if (conn != null)
					conn.close();
			}
			regActiveSubjects.add(user.getUserid());
			registration.getEventList().add(new SubjectEvent(
					SubjectEvent.Type.ADDED, user.getUserid()));
			lock.notifyAll();
			saveRegistration();
		}
	}

	@Override
	public void userRemovedAsSubject(User user, User profUser) {
		synchronized (lock) {
			if (!regUser.getUserid().equals(profUser.getUserid()) ||
					!regActiveSubjects.contains(user.getUserid())) {
				return;
			}
			regActiveSubjects.remove(user.getUserid());
			registration.getEventList().add(new SubjectEvent(
					SubjectEvent.Type.REMOVED, user.getUserid()));
			lock.notifyAll();
			saveRegistration();
		}
	}
	
	private boolean saveRegistration() {
		DatabaseLoader dbLoader = DatabaseLoader.getInstance();
		DatabaseConnection conn = null;
		try {
			conn = dbLoader.openConnection();
			Database authDb = dbLoader.initAuthDatabase(conn);
			authDb.update(WatchSubjectRegistrationTable.NAME, registration);
			return true;
		} catch (DatabaseException ex) {
			logger.error("Database error while saving events: " +
					ex.getMessage(), ex);
			return false;
		} catch (IOException ex) {
			logger.error("Communication error while saving events: " +
					ex.getMessage(), ex);
			return false;
		} finally {
			if (conn != null)
				conn.close();
		}
	}

	public static void initListeners(String project, Database authDb)
			throws DatabaseException {
		synchronized (LOCK) {
			cleanRegistrations(authDb);
			WatchSubjectRegistrationTable table =
					new WatchSubjectRegistrationTable();
			DatabaseCriteria criteria = new DatabaseCriteria.Equal(
					"project", project);
			List<WatchSubjectRegistration> registrations = authDb.select(
					table, criteria, 0, null);
			UserListenerRepository repository =
					UserListenerRepository.getInstance();
			for (WatchSubjectRegistration registration : registrations) {
				WatchSubjectListener listener = new WatchSubjectListener(
						registration);
				if (listener.initUserSubjects(registration.getUser(), authDb)) {
					repository.addUserListener(listener);
				} else {
					authDb.delete(WatchSubjectRegistrationTable.NAME,
							registration);
				}
			}
		}
	}
	
	private static void cleanRegistrations(Database authDb)
			throws DatabaseException {
		synchronized (LOCK) {
			ZonedDateTime now = DateTimeUtils.nowMs();
			long minWatchTime = now.minusMinutes(REMOVE_AFTER_WATCH_MINUTES)
					.toInstant().toEpochMilli();
			UserListenerRepository repository =
					UserListenerRepository.getInstance();
			List<UserListener> listeners = repository.getUserListeners();
			for (UserListener listener : listeners) {
				if (!(listener instanceof WatchSubjectListener))
					continue;
				WatchSubjectListener watchListener =
						(WatchSubjectListener)listener;
				if (watchListener.registration.getLastWatchTime() <
						minWatchTime) {
					repository.removeUserListener(watchListener);
				}
			}
			DatabaseCriteria criteria = new DatabaseCriteria.LessThan(
					"lastWatchTime", minWatchTime);
			authDb.delete(new WatchSubjectRegistrationTable(), criteria);
		}
	}
	
	public static boolean setRegistrationWatchTime(Database authDb,
			WatchSubjectRegistration reg) throws DatabaseException {
		synchronized (LOCK) {
			ZonedDateTime now = DateTimeUtils.nowMs();
			WatchSubjectListener listener = findListener(reg.getId());
			if (listener == null)
				return false;
			reg.setLastWatchTime(now.toInstant().toEpochMilli());
			authDb.update(WatchSubjectRegistrationTable.NAME, reg);
			listener.registration.setLastWatchTime(now.toInstant()
					.toEpochMilli());
			return true;
		}
	}
	
	public static String addRegistration(Database authDb, User user,
			String project, boolean reset) throws DatabaseException {
		synchronized (LOCK) {
			cleanRegistrations(authDb);
			WatchSubjectListener listener = findListener(user.getUserid(),
					project);
			ZonedDateTime now = DateTimeUtils.nowMs();
			if (listener != null) {
				WatchSubjectRegistration reg = listener.registration;
				listener.setCurrentWatch(null);
				reg.setLastWatchTime(now.toInstant().toEpochMilli());
				if (reset)
					reg.getEventList().clear();
				authDb.update(WatchSubjectRegistrationTable.NAME, reg);
				return listener.registration.getId();
			} else {
				WatchSubjectRegistration reg = new WatchSubjectRegistration();
				reg.setUser(user.getUserid());
				reg.setProject(project);
				reg.setLastWatchTime(now.toInstant().toEpochMilli());
				listener = new WatchSubjectListener(reg);
				listener.initUserSubjects(user, authDb);
				authDb.insert(WatchSubjectRegistrationTable.NAME, reg);
				UserListenerRepository repository =
						UserListenerRepository.getInstance();
				repository.addUserListener(listener);
				return reg.getId();
			}
		}
	}
	
	public static void removeRegistration(Database authDb,
			WatchSubjectListener listener) throws DatabaseException {
		synchronized (LOCK) {
			listener.setCurrentWatch(null);
			authDb.delete(WatchSubjectRegistrationTable.NAME,
					listener.registration);
			UserListenerRepository repository =
					UserListenerRepository.getInstance();
			repository.removeUserListener(listener);
		}
	}
	
	private static WatchSubjectListener findListener(String user,
			String project) {
		synchronized (LOCK) {
			UserListenerRepository repository =
					UserListenerRepository.getInstance();
			List<UserListener> listeners = repository.getUserListeners();
			for (UserListener listener : listeners) {
				if (!(listener instanceof WatchSubjectListener))
					continue;
				WatchSubjectListener watchListener =
						(WatchSubjectListener)listener;
				WatchSubjectRegistration reg = watchListener.registration;
				if (!reg.getUser().equals(user))
					continue;
				if (!reg.getProject().equals(project))
					continue;
				return watchListener;
			}
			return null;
		}
	}
	
	public static WatchSubjectListener findListener(String regId) {
		synchronized (LOCK) {
			UserListenerRepository repository =
					UserListenerRepository.getInstance();
			List<UserListener> listeners = repository.getUserListeners();
			for (UserListener listener : listeners) {
				if (!(listener instanceof WatchSubjectListener))
					continue;
				WatchSubjectListener watchListener =
						(WatchSubjectListener)listener;
				if (watchListener.registration.getId().equals(regId))
					return watchListener;
			}
			return null;
		}
	}
}
