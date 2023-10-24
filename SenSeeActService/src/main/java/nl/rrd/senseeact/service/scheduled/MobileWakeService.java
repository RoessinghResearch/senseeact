package nl.rrd.senseeact.service.scheduled;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import nl.rrd.senseeact.client.model.MobileWakePushMessage;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.listener.DatabaseEvent;
import nl.rrd.senseeact.dao.listener.DatabaseListener;
import nl.rrd.senseeact.dao.listener.DatabaseListenerRepository;
import nl.rrd.senseeact.service.Configuration;
import nl.rrd.senseeact.service.DatabaseLoader;
import nl.rrd.senseeact.service.model.MobileWakeRequest;
import nl.rrd.senseeact.service.model.MobileWakeRequestTable;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;

@Service
public class MobileWakeService {
	private static final Object LOCK = new Object();
	private boolean destroyed = false;

	private Map<String,MobileWakeTimer> wakeTimers = new HashMap<>();
	private String authDbName = null;
	private WakeDatabaseListener dbListener = null;

	@PostConstruct
	public void init() {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("Start " + getClass().getSimpleName());
		if (FirebaseApp.getApps().isEmpty()) {
			FirebaseOptions options;
			try {
				options = FirebaseOptions.builder()
						.setCredentials(GoogleCredentials.getApplicationDefault())
						.build();
			} catch (IOException ex) {
				throw new RuntimeException("Failed to read Google credentials: " +
						ex.getMessage(), ex);
			}
			FirebaseApp.initializeApp(options);
		}
		new Thread(this::doInit).start();
	}

	private void doInit() {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		DatabaseConnection dbConn = null;
		Database authDb = null;
		while (!destroyed && authDb == null) {
			AuthDbConnection authDbConn = null;
			try {
				authDbConn = initAuthDb();
			} catch (DatabaseException | IOException ex) {
				if (destroyed)
					return;
				logger.info("Failed to open auth database, retrying...");
			}
			if (authDbConn == null) {
				waitRetry();
			} else {
				dbConn = authDbConn.dbConn;
				authDb = authDbConn.authDb;
			}
		}
		try {
			if (destroyed)
				return;
			DatabaseListenerRepository repository =
					DatabaseListenerRepository.getInstance();
			authDbName = authDb.getName();
			dbListener = new WakeDatabaseListener();
			repository.addDatabaseListener(authDbName, dbListener);
			tryUpdateWakeTimers(authDb);
		} finally {
			dbConn.close();
		}
	}

	private void waitRetry() {
		synchronized (LOCK) {
			long now = System.currentTimeMillis();
			long end = now + 60000;
			while (!destroyed && now < end) {
				try {
					LOCK.wait(end - now);
				} catch (InterruptedException ex) {
					throw new RuntimeException("Thread interrupted", ex);
				}
				now = System.currentTimeMillis();
			}
		}
	}

	@PreDestroy
	public void destroy() {
		List<MobileWakeTimer> timers;
		synchronized (LOCK) {
			if (destroyed)
				return;
			destroyed = true;
			timers = new ArrayList<>(wakeTimers.values());
			wakeTimers.clear();
			LOCK.notifyAll();
		}
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("Stop " + getClass().getSimpleName());
		if (dbListener != null) {
			DatabaseListenerRepository repository =
					DatabaseListenerRepository.getInstance();
			repository.removeDatabaseListener(authDbName, dbListener);
		}
		for (MobileWakeTimer timer : timers) {
			timer.timer.cancel();
		}
	}

	private AuthDbConnection tryInitAuthDb() {
		try {
			return initAuthDb();
		} catch (DatabaseException | IOException ex) {
			Logger logger = AppComponents.getLogger(getClass().getSimpleName());
			logger.info("Failed to open auth database");
			return null;
		}
	}

	private AuthDbConnection initAuthDb() throws DatabaseException,
			IOException {
		DatabaseLoader dbLoader = DatabaseLoader.getInstance();
		Database authDb = null;
		DatabaseConnection dbConn = dbLoader.openConnection();
		try {
			authDb = dbLoader.initAuthDatabase(dbConn);
		} finally {
			if (authDb == null)
				dbConn.close();
		}
		return new AuthDbConnection(dbConn, authDb);
	}

	private void tryUpdateWakeTimers() {
		if (destroyed)
			return;
		AuthDbConnection authDbConn = tryInitAuthDb();
		if (authDbConn == null)
			return;
		try {
			tryUpdateWakeTimers(authDbConn.authDb);
		} finally {
			authDbConn.dbConn.close();
		}
	}

	private void tryUpdateWakeTimers(Database authDb) {
		if (destroyed)
			return;
		try {
			updateWakeTimers(authDb);
		} catch (DatabaseException ex) {
			Logger logger = AppComponents.getLogger(getClass().getSimpleName());
			logger.error("Database error while trying to update wake timers: " +
					ex.getMessage(), ex);
		}
	}

	private void updateWakeTimers(Database authDb) throws DatabaseException {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("Update wake timers");
		MobileWakeRequestTable table = new MobileWakeRequestTable();
		List<MobileWakeRequest> wakeRequests = authDb.select(table, null, 0,
				null);
		synchronized (LOCK) {
			if (destroyed)
				return;
			List<String> currIds = new ArrayList<>();
			for (MobileWakeRequest request : wakeRequests) {
				String id = request.getId();
				currIds.add(id);
				MobileWakeTimer timer = wakeTimers.get(id);
				if (timer == null) {
					createNewTimer(request);
				} else {
					updateExistingTimer(request);
				}
			}
			List<String> prevIds = new ArrayList<>(wakeTimers.keySet());
			for (String prevId : prevIds) {
				if (!currIds.contains(prevId)) {
					removeTimer(prevId);
				}
			}
		}
	}

	private void createNewTimer(MobileWakeRequest request) {
		Timer timer = new Timer();
		wakeTimers.put(request.getId(), new MobileWakeTimer(request, timer));
		int intervalMs = request.getInterval() * 1000;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				sendPushMessage(request);
			}
		}, intervalMs, intervalMs);
	}

	private void updateExistingTimer(MobileWakeRequest request) {
		MobileWakeTimer wakeTimer = wakeTimers.get(request.getId());
		if (wakeTimer.wakeRequest.equals(request))
			return;
		removeTimer(request.getId());
		createNewTimer(request);
	}

	private void removeTimer(String requestId) {
		MobileWakeTimer wakeTimer = wakeTimers.remove(requestId);
		wakeTimer.timer.cancel();
	}

	private void sendPushMessage(MobileWakeRequest request) {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		MobileWakePushMessage data = new MobileWakePushMessage(
				request.getUser());
		Map<String,String> dataMap;
		try {
			dataMap = JsonMapper.convert(data, new TypeReference<>() {});
		} catch (ParseException ex) {
			logger.error("Failed to create push message data map: " +
					ex.getMessage(), ex);
			return;
		}
		AndroidConfig androidConfig = AndroidConfig.builder()
				.setPriority(AndroidConfig.Priority.HIGH)
				.build();
		Message.Builder builder = Message.builder()
				.putAllData(dataMap)
				.setToken(request.getFcmToken())
				.setAndroidConfig(androidConfig);
		Message message = builder.build();
		try {
			String response = FirebaseMessaging.getInstance().send(message);
			logger.info("Sent mobile wake message to " + request.getFcmToken());
			logger.info("Response: " + response);
		} catch (FirebaseMessagingException ex) {
			MessagingErrorCode error = ex.getMessagingErrorCode();
			if (error == MessagingErrorCode.UNREGISTERED ||
					error == MessagingErrorCode.SENDER_ID_MISMATCH) {
				unregisterOnError(request, error);
			} else {
				logger.error("Failed to send push message to " +
						request.getFcmToken() + ": " + ex.getMessage(), ex);
			}
		}
	}

	private void unregisterOnError(MobileWakeRequest request,
			MessagingErrorCode error) {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.error("Unregister mobile wake request on FCM error: " + error);
		AuthDbConnection authDbConn = tryInitAuthDb();
		if (authDbConn == null)
			return;
		try {
			authDbConn.authDb.delete(MobileWakeRequestTable.NAME, request);
		} catch (DatabaseException ex) {
			logger.error("Failed to delete mobile wake request: " +
					ex.getMessage(), ex);
		} finally {
			authDbConn.dbConn.close();
		}
		synchronized (LOCK) {
			if (destroyed)
				return;
			MobileWakeTimer wakeTimer = wakeTimers.remove(request.getId());
			if (wakeTimer != null)
				wakeTimer.timer.cancel();
		}
	}

	private static class AuthDbConnection {
		public DatabaseConnection dbConn;
		public Database authDb;

		public AuthDbConnection(DatabaseConnection dbConn, Database authDb) {
			this.dbConn = dbConn;
			this.authDb = authDb;
		}
	}

	private static class MobileWakeTimer {
		public MobileWakeRequest wakeRequest;
		public Timer timer;

		public MobileWakeTimer(MobileWakeRequest wakeRequest, Timer timer) {
			this.wakeRequest = wakeRequest;
			this.timer = timer;
		}
	}

	private class WakeDatabaseListener implements DatabaseListener {
		@Override
		public void onDatabaseEvent(DatabaseEvent event) {
			if (!MobileWakeRequestTable.NAME.equals(event.getTable()))
				return;
			new Thread(MobileWakeService.this::tryUpdateWakeTimers).start();
		}
	}
}
