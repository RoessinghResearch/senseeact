package nl.rrd.senseeact.service.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.SenSeeActClient;
import nl.rrd.senseeact.client.model.SyncWatchResult;
import nl.rrd.senseeact.client.model.SyncWatchResult.ResultCode;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseAction;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.dao.listener.DatabaseActionListener;
import nl.rrd.senseeact.dao.listener.DatabaseListenerRepository;
import nl.rrd.senseeact.dao.sync.*;
import nl.rrd.senseeact.service.*;
import nl.rrd.senseeact.service.controller.model.SyncRegisterPushInput;
import nl.rrd.senseeact.service.controller.model.SyncWatchInput;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.SyncPushRegistration;
import nl.rrd.senseeact.service.model.SyncPushRegistrationTable;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.validation.MapReader;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SyncControllerExecution {

	/**
	 * Runs the query getReadStats().
	 *
	 * @param version the protocol version
	 * @param request the HTTP request
	 * @param authDb the authentication database
	 * @param database the database (can be null)
	 * @param user the user
	 * @param subject the user ID or email address of the subject or null
	 * @return statistics about new database actions
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public SyncActionStats getReadStats(ProtocolVersion version,
			HttpServletRequest request, Database authDb, Database database,
			User user, String subject) throws HttpException, Exception {
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		if (database == null)
			return new SyncActionStats(null, 0, null);
		boolean includeOwn = false;
		List<SyncProgress> progress = null;
		List<String> includeTables = null;
		List<String> excludeTables = null;
		List<SyncTimeRangeRestriction> timeRangeRestrictions = null;
		try {
			Map<String,?> params = HttpContentReader.readJsonParams(request,
					true);
			if (params != null) {
				MapReader paramReader = new MapReader(params);
				includeOwn = paramReader.readBoolean("includeOwn", false);
				progress = paramReader.readJson("progress",
						new TypeReference<>() {}, null);
				includeTables = paramReader.readJson("includeTables",
						new TypeReference<>() {}, null);
				excludeTables = paramReader.readJson("excludeTables",
						new TypeReference<>() {}, null);
				if (params.containsKey("timeRangeRestrictions")) {
					timeRangeRestrictions = paramReader.readJson(
							"timeRangeRestrictions", new TypeReference<>() {},
							null);
				} else {
					timeRangeRestrictions = paramReader.readJson(
							"sampleTimeRangeRestrictions",
							new TypeReference<>() {}, null);
				}
			}
		} catch (ParseException ex) {
			throw new BadRequestException("Invalid content: " +
					ex.getMessage());
		}
		DatabaseSynchronizer sync = new DatabaseSynchronizer(
				subjectUser.getUserid());
		sync.setIncludeTables(includeTables);
		sync.setExcludeTables(excludeTables);
		sync.setTimeRangeRestrictions(timeRangeRestrictions);
		return sync.getSyncActionStats(database, progress,
				includeOwn ? null :
				Arrays.asList(SenSeeActClient.SYNC_REMOTE_ID, user.getUserid()));
	}
	
	/**
	 * Runs the query read().
	 *
	 * @param version the protocol version
	 * @param request the HTTP request
	 * @param authDb the authentication database
	 * @param database the database (can be null)
	 * @param user the user
	 * @param subject the user ID or email address of the subject or null
	 * @return the database actions
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public List<DatabaseAction> read(ProtocolVersion version,
			HttpServletRequest request, Database authDb, Database database,
			User user, String subject) throws HttpException, Exception {
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		if (database == null)
			return new ArrayList<>();
		int maxCount = 0;
		Long maxTime = null;
		boolean includeOwn = false;
		List<SyncProgress> progress = null;
		List<String> includeTables = null;
		List<String> excludeTables = null;
		List<SyncTimeRangeRestriction> timeRangeRestrictions = null;
		try {
			Map<String,?> params = HttpContentReader.readJsonParams(request,
					true);
			if (params != null) {
				MapReader paramReader = new MapReader(params);
				maxCount = paramReader.readInt("maxCount", 0);
				maxTime = paramReader.readLong("maxTime", null);
				includeOwn = paramReader.readBoolean("includeOwn", false);
				progress = paramReader.readJson("progress",
						new TypeReference<>() {}, null);
				includeTables = paramReader.readJson("includeTables",
						new TypeReference<>() {}, null);
				excludeTables = paramReader.readJson("excludeTables",
						new TypeReference<>() {}, null);
				if (params.containsKey("timeRangeRestrictions")) {
					timeRangeRestrictions = paramReader.readJson(
							"timeRangeRestrictions", new TypeReference<>() {},
							null);
				} else {
					timeRangeRestrictions = paramReader.readJson(
							"sampleTimeRangeRestrictions",
							new TypeReference<>() {}, null);
				}
			}
		} catch (ParseException ex) {
			throw new BadRequestException("Invalid content: " +
					ex.getMessage());
		}
		DatabaseSynchronizer sync = new DatabaseSynchronizer(
				subjectUser.getUserid());
		sync.setIncludeTables(includeTables);
		sync.setExcludeTables(excludeTables);
		sync.setTimeRangeRestrictions(timeRangeRestrictions);
		return sync.readSyncActions(database, progress, maxCount, maxTime,
				includeOwn ? null :
				Arrays.asList(SenSeeActClient.SYNC_REMOTE_ID, user.getUserid()));
	}
	
	public Object registerPush(ProtocolVersion version,
			HttpServletRequest request, Database authDb, Database database,
			User user, BaseProject project, String subject)
			throws HttpException, Exception {
		SyncRegisterPushInput input = SyncRegisterPushInput.parse(version,
				request, authDb, database, user, subject);
		if (input == null)
			return null;
		ObjectMapper mapper = new ObjectMapper();
		String restrictJson = mapper.writeValueAsString(
				input.getRestrictions());
		DatabaseCriteria criteria = new DatabaseCriteria.And(
			new DatabaseCriteria.Equal("user",
					input.getSubjectUser().getUserid()),
			new DatabaseCriteria.Equal("project", project.getCode()),
			new DatabaseCriteria.Equal("database", database.getName()),
			new DatabaseCriteria.Equal("deviceId", input.getDeviceId())
		);
		SyncPushRegistration registration = authDb.selectOne(
				new SyncPushRegistrationTable(), criteria, null);
		if (registration != null) {
			registration.setFcmToken(input.getFcmToken());
			registration.setRestrictions(restrictJson);
			authDb.update(SyncPushRegistrationTable.NAME, registration);
		} else {
			registration = new SyncPushRegistration();
			registration.setUser(input.getSubjectUser().getUserid());
			registration.setProject(project.getCode());
			registration.setDatabase(database.getName());
			registration.setDeviceId(input.getDeviceId());
			registration.setFcmToken(input.getFcmToken());
			registration.setRestrictions(restrictJson);
			authDb.insert(SyncPushRegistrationTable.NAME, registration);
		}
		PushNotificationService pushService = AppComponents.get(
				PushNotificationService.class);
		pushService.addRegistration(registration);
		return null;
	}

	public Object unregisterPush(ProtocolVersion version, Database authDb,
			Database database, User user, BaseProject project, String subject,
			String deviceId) throws HttpException, Exception {
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		if (database == null)
			return null;
		DatabaseCriteria criteria = new DatabaseCriteria.And(
			new DatabaseCriteria.Equal("user", subjectUser.getUserid()),
			new DatabaseCriteria.Equal("project", project.getCode()),
			new DatabaseCriteria.Equal("database", database.getName()),
			new DatabaseCriteria.Equal("deviceId", deviceId)
		);
		authDb.delete(new SyncPushRegistrationTable(), criteria);
		PushNotificationService pushService = AppComponents.get(
				PushNotificationService.class);
		pushService.removeRegistrations(database.getName(),
				subjectUser.getUserid(), deviceId);
		return null;
	}

	/**
	 * Runs the query watch().
	 * 
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param versionName the protocol version
	 * @param project the project code
	 * @param subject the user ID or email address of the subject or null
	 * @return the query result
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public SyncWatchResult watch(final HttpServletRequest request,
			HttpServletResponse response, String versionName, String project,
			final String subject)
			throws HttpException, Exception {
		long queryStart = System.currentTimeMillis();
		long queryEnd = queryStart +
				ProjectControllerExecution.HANGING_GET_TIMEOUT;
		// verify authentication and project, init databases
		SyncWatchInput input = SyncWatchInput.parse(request, response,
				versionName, project, subject);
		SyncWatchResult result = new SyncWatchResult();
		if (input == null) {
			result.setResultCode(ResultCode.NO_DATA);
			return result;
		}
		DatabaseListenerRepository listeners =
				DatabaseListenerRepository.getInstance();
		final Object lock = new Object();
		SyncWatchListener listener = new SyncWatchListener(lock);
		listeners.addDatabaseActionListener(input.getDatabaseName(), listener);
		try {
			while (true) {
				List<DatabaseAction> actions = watchPoll(request, response,
						versionName, input, project);
				if (!actions.isEmpty()) {
					result.setResultCode(ResultCode.OK);
					result.setActions(actions);
					return result;
				}
				synchronized (lock) {
					boolean hasNewActions = listener.clearHasNewActions();
					long now = System.currentTimeMillis();
					while (now < queryEnd && !hasNewActions) {
						lock.wait(queryEnd - now);
						hasNewActions = listener.clearHasNewActions();
						now = System.currentTimeMillis();
					}
					if (!hasNewActions) {
						result.setResultCode(ResultCode.TIMEOUT);
						return result;
					}
				}
			}
		} finally {
			listeners.removeDatabaseActionListener(input.getDatabaseName(),
					listener);
		}
	}
	
	/**
	 * Polls the database for new database actions as part of the watch()
	 * query. If the database is null, this method returns null, meaning
	 * that there will never be any database actions. The user input is still
	 * unverified.
	 * 
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param versionName the protocol version
	 * @param input the validates query input
	 * @param project the project code
	 * @return the database actions or null
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	private List<DatabaseAction> watchPoll(HttpServletRequest request,
			HttpServletResponse response, String versionName,
			final SyncWatchInput input, String project) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				doWatchPoll(input, projectDb, user),
				versionName, project, request, response);
	}
	
	/**
	 * Polls the database for new database actions as part of the watch() query.
	 * 
	 * @param input the watch input
	 * @param database the database (can be null)
	 * @param user the user
	 * @return the database actions
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	private List<DatabaseAction> doWatchPoll(SyncWatchInput input,
			Database database, User user) throws HttpException, Exception {
		DatabaseSynchronizer sync = new DatabaseSynchronizer(
				input.getSubjectUser().getUserid());
		sync.setIncludeTables(input.getIncludeTables());
		sync.setExcludeTables(input.getExcludeTables());
		sync.setTimeRangeRestrictions(input.getTimeRangeRestrictions());
		return sync.readSyncActions(database, input.getProgress(),
				input.getMaxCount(), null,
				Arrays.asList(SenSeeActClient.SYNC_REMOTE_ID, user.getUserid()));
	}
	
	private static class SyncWatchListener implements DatabaseActionListener {
		private final Object lock;
		private boolean hasNewActions = false;
		
		public SyncWatchListener(Object lock) {
			this.lock = lock;
		}

		/**
		 * Returns the value of "hasNewActions" and sets it to false.
		 * 
		 * @return the value of "hasNewActions" before clearing it
		 */
		public boolean clearHasNewActions() {
			synchronized (lock) {
				boolean result = hasNewActions;
				hasNewActions = false;
				return result;
			}
		}
		
		@Override
		public void onAddDatabaseActions(String database, String table,
				List<DatabaseAction> actions) {
			synchronized (lock) {
				hasNewActions = true;
				lock.notifyAll();
			}
		}
	}
	
	/**
	 * Runs the query getProgress().
	 *
	 * @param version the protocol version
	 * @param request the HTTP request
	 * @param authDb the authentication database
	 * @param database the database (can be null)
	 * @param user the user
	 * @param subject the user ID or email address of the subject or null
	 * @return the sync progress
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public List<SyncProgress> getProgress(ProtocolVersion version,
			HttpServletRequest request, Database authDb, Database database,
			User user, String subject) throws HttpException, Exception {
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		if (database == null)
			return new ArrayList<>();
		List<String> includeTables = null;
		List<String> excludeTables = null;
		try {
			Map<String,?> params = HttpContentReader.readJsonParams(request,
					true);
			if (params != null) {
				MapReader paramReader = new MapReader(params);
				includeTables = paramReader.readJson("includeTables",
						new TypeReference<>() {}, null);
				excludeTables = paramReader.readJson("excludeTables",
						new TypeReference<>() {}, null);
			}
		} catch (ParseException ex) {
			throw new BadRequestException("Invalid content: " +
					ex.getMessage());
		}
		DatabaseSynchronizer sync = new DatabaseSynchronizer(
				subjectUser.getUserid());
		sync.setIncludeTables(includeTables);
		sync.setExcludeTables(excludeTables);
		return sync.getSyncProgress(database);
	}
	
	/**
	 * Runs the query write().
	 *
	 * @param version the protocol version
	 * @param request the HTTP request
	 * @param authDb the authentication database
	 * @param database the database
	 * @param user the user
	 * @param subject the user ID or email address of the subject or null
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Object write(ProtocolVersion version, HttpServletRequest request,
			Database authDb, Database database, User user, String subject)
			throws HttpException, Exception {
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		List<DatabaseAction> actions;
		List<String> includeTables;
		List<String> excludeTables;
		try {
			Map<String,?> params = HttpContentReader.readJsonParams(request);
			MapReader paramReader = new MapReader(params);
			actions = paramReader.readJson("actions", new TypeReference<>() {});
			includeTables = paramReader.readJson("includeTables",
					new TypeReference<>() {}, null);
			excludeTables = paramReader.readJson("excludeTables",
					new TypeReference<>() {}, null);
		} catch (ParseException ex) {
			throw new BadRequestException("Invalid content: " + ex.getMessage());
		}
		if (actions.isEmpty())
			return null;
		DatabaseSynchronizer sync = new DatabaseSynchronizer(
				subjectUser.getUserid());
		sync.setIncludeTables(includeTables);
		sync.setExcludeTables(excludeTables);
		Logger logger = AppComponents.getLogger(SenSeeActContext.LOGTAG);
		try {
			sync.writeSyncActions(database, actions, user.getUserid());
		} catch (SyncForbiddenException ex) {
			String error = "One or more database actions are not allowed";
			logger.error(error + ": " + ex.getMessage(), ex);
			throw new ForbiddenException(error);
		} catch (IllegalInputException ex) {
			String error = "Illegal database actions";
			logger.error(error + ": " + ex.getMessage(), ex);
			throw new BadRequestException(error);
		}
		return null;
	}
}
