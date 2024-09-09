package nl.rrd.senseeact.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.HttpFieldError;
import nl.rrd.senseeact.client.model.*;
import nl.rrd.senseeact.client.model.compat.ProjectV1;
import nl.rrd.senseeact.client.model.compat.ProjectV2;
import nl.rrd.senseeact.client.model.compat.ProjectV3;
import nl.rrd.senseeact.client.model.sample.LocalTimeSample;
import nl.rrd.senseeact.client.model.sample.Sample;
import nl.rrd.senseeact.client.model.sample.UTCSample;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.*;
import nl.rrd.senseeact.service.*;
import nl.rrd.senseeact.service.controller.model.SelectFilterParser;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.exception.NotFoundException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.*;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.beans.PropertyReader;
import nl.rrd.utils.beans.PropertyWriter;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonAtomicToken;
import nl.rrd.utils.json.JsonMapper;
import nl.rrd.utils.json.JsonObjectStreamReader;
import nl.rrd.utils.json.JsonParseException;
import nl.rrd.utils.validation.TypeConversion;
import nl.rrd.utils.validation.ValidationException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class ProjectControllerExecution {
	private static final int BATCH_SIZE = 1000;
	public static final int HANGING_GET_TIMEOUT = 60000;

	/**
	 * Runs the list query.
	 *
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param user the user
	 * @return the project codes
	 * @throws DatabaseException if a database error occurs
	 */
	public List<?> list(ProtocolVersion version, Database authDb, User user)
			throws DatabaseException {
		List<BaseProject> projects = getUserProjects(authDb, user);
		if (version.ordinal() >= ProtocolVersion.V6_0_8.ordinal()) {
			List<Project> result = new ArrayList<>();
			for (BaseProject baseProject : projects) {
				Project project = new Project();
				project.setCode(baseProject.getCode());
				project.setName(baseProject.getName());
				result.add(project);
			}
			return result;
		} else if (version.ordinal() >= ProtocolVersion.V6_0_4.ordinal()) {
			List<ProjectV3> result = new ArrayList<>();
			for (BaseProject baseProject : projects) {
				ProjectV3 project = new ProjectV3();
				project.setCode(baseProject.getCode());
				result.add(project);
			}
			return result;
		} else if (version.ordinal() >= ProtocolVersion.V5_0_5.ordinal()) {
			List<ProjectV2> result = new ArrayList<>();
			for (BaseProject baseProject : projects) {
				ProjectV2 project = new ProjectV2();
				project.setCode(baseProject.getCode());
				if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal())
					project.setSyncUser(user.getUserid());
				else
					project.setSyncUser(user.getEmail());
				project.setSyncGroup(null);
				result.add(project);
			}
			return result;
		} else if (version.ordinal() >= ProtocolVersion.V5_0_4.ordinal()) {
			List<ProjectV1> result = new ArrayList<>();
			for (BaseProject baseProject : projects) {
				ProjectV1 project = new ProjectV1();
				project.setCode(baseProject.getCode());
				project.setSyncUser(user.getEmail());
				result.add(project);
			}
			return result;
		} else {
			List<String> result = new ArrayList<>();
			for (BaseProject project : projects) {
				result.add(project.getCode());
			}
			return result;
		}
	}

	/**
	 * Runs the list all query.
	 *
	 * @return the project codes
	 */
	public List<String> listAll() {
		List<String> result = new ArrayList<>();
		ProjectRepository projectRepo = AppComponents.get(
				ProjectRepository.class);
		List<BaseProject> projects = projectRepo.getProjects();
		for (BaseProject project : projects) {
			result.add(project.getCode());
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * Runs the query checkProject.
	 *
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param user the user who is accessing the project
	 * @param subject the user ID or email address of the user that is accessed
	 * @throws HttpException if the request is invalid
	 * @throws DatabaseException if a database error occurs
	 */
	public Object checkProject(ProtocolVersion version, Database authDb,
			User user, String subject) throws HttpException, DatabaseException {
		if (subject != null && !subject.isEmpty())
			User.findAccessibleUser(version, subject, authDb, user);
		return null;
	}

	/**
	 * Returns the projects that the specified user can access. If the user is
	 * an admin, that is all projects. Otherwise it checks the {@link
	 * UserProjectTable UserProjectTable}. The projects will be sorted by code.
	 *
	 * @param authDb the authentication database
	 * @param user the user
	 * @return the projects
	 * @throws DatabaseException if a database error occurs
	 */
	private static List<BaseProject> getUserProjects(Database authDb, User user)
			throws DatabaseException {
		List<String> projectCodes = user.findProjects(authDb);
		List<BaseProject> projects = new ArrayList<>();
		ProjectRepository projectRepo = AppComponents.get(
				ProjectRepository.class);
		for (String code : projectCodes) {
			projects.add(projectRepo.findProjectByCode(code));
		}
		return projects;
	}

	/**
	 * Runs the query addUser.
	 *
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param user the user who is logged in
	 * @param projectCode the code of the project to which the user should be
	 * added
	 * @param subject the "user" parameter containing the user ID or email
	 * address of the user to add. This is preferred to the "email" parameter.
	 * @param compatEmail the "email" parameter containing the email address of
	 * the user to add
	 * @param asRole the role as which the user should be added to the project
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Object addUser(ProtocolVersion version, Database authDb,
			User user, String projectCode, String subject, String compatEmail,
			String asRole) throws HttpException, Exception {
		User userToAdd;
		if (compatEmail != null && !compatEmail.isEmpty()) {
			userToAdd = User.findAccessibleUserByEmail(compatEmail, authDb,
					user);
		} else {
			userToAdd = User.findAccessibleUser(version, subject, authDb, user);
		}
		BaseProject project;
		if (user.getUserid().equals(userToAdd.getUserid()))
			project = findProject(projectCode);
		else
			project = findUserProject(projectCode, authDb, user);
		Role asRoleEnum;
		try {
			asRoleEnum = TypeConversion.getEnum(asRole, Role.class);
		} catch (ParseException ex) {
			String msg = "Invalid role: " + asRole;
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
			error.addFieldError(new HttpFieldError("asRole", msg));
			throw new BadRequestException(error);
		}
		int userRoleIndex = userToAdd.getRole().ordinal();
		int asRoleIndex = asRoleEnum.ordinal();
		if (asRoleIndex < userRoleIndex) {
			throw new ForbiddenException(
					"Can't add users to project as higher role than their own role");
		}
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("user", userToAdd.getUserid()),
				new DatabaseCriteria.Equal("projectCode", project.getCode()),
				new DatabaseCriteria.Equal("asRole", asRoleEnum.toString()));
		UserProject userProject = authDb.selectOne(new UserProjectTable(),
				criteria, null);
		if (userProject != null)
			return null;
		try {
			project.validateAddUser(user, userToAdd, authDb);
		} catch (ValidationException ex) {
			throw new ForbiddenException(ex.getMessage());
		}
		userProject = new UserProject();
		userProject.setUser(userToAdd.getUserid());
		userProject.setProjectCode(project.getCode());
		userProject.setAsRole(asRoleEnum);
		authDb.insert(UserProjectTable.NAME, userProject);
		UserListenerRepository.getInstance().notifyUserAddedToProject(userToAdd,
				projectCode, asRoleEnum);
		return null;
	}

	public Object removeUser(ProtocolVersion version, Database authDb,
			User user, String projectCode, String subject, String compatEmail,
			String asRole)
			throws HttpException, Exception {
		User removeUser;
		if (compatEmail != null && !compatEmail.isEmpty()) {
			removeUser = User.findAccessibleUserByEmail(compatEmail, authDb,
					user);
		} else {
			removeUser = User.findAccessibleUser(version, subject, authDb,
					user);
		}
		BaseProject project;
		if (user.getUserid().equals(removeUser.getUserid()))
			project = findProject(projectCode);
		else
			project = findUserProject(projectCode, authDb, user);
		Role asRoleEnum = null;
		if (asRole != null && !asRole.isEmpty()) {
			try {
				asRoleEnum = TypeConversion.getEnum(asRole, Role.class);
			} catch (ParseException ex) {
				String msg = "Invalid role: " + asRole;
				HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
				error.addFieldError(new HttpFieldError("asRole", msg));
				throw new BadRequestException(error);
			}
		}
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("user", removeUser.getUserid()),
				new DatabaseCriteria.Equal("projectCode", project.getCode())
		);
		List<UserProject> userProjects = authDb.select(
				new UserProjectTable(), criteria, 0, null);
		List<Role> oldRoles = new ArrayList<>();
		for (UserProject userProject : userProjects) {
			oldRoles.add(userProject.getAsRole());
		}
		List<Role> removedRoles;
		if (asRoleEnum == null)
			removedRoles = oldRoles;
		else if (oldRoles.contains(asRoleEnum))
			removedRoles = Collections.singletonList(asRoleEnum);
		else
			removedRoles = new ArrayList<>();
		if (!removedRoles.isEmpty()) {
			List<DatabaseCriteria> andList = new ArrayList<>();
			andList.add(new DatabaseCriteria.Equal("user",
					removeUser.getUserid()));
			andList.add(new DatabaseCriteria.Equal("projectCode",
					project.getCode()));
			if (asRoleEnum != null) {
				andList.add(new DatabaseCriteria.Equal("asRole",
						asRoleEnum.toString()));
			}
			criteria = new DatabaseCriteria.And(andList.toArray(
					new DatabaseCriteria[0]));
			authDb.delete(new UserProjectTable(), criteria);
			for (Role removedRole : removedRoles) {
				UserListenerRepository.getInstance()
						.notifyUserRemovedFromProject(removeUser, projectCode,
								removedRole);
			}
		}
		criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("project", project.getCode()),
				new DatabaseCriteria.Equal("subject", removeUser.getUserid())
		);
		authDb.delete(new ProjectUserAccessTable(), criteria);
		criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("user", removeUser.getUserid()),
				new DatabaseCriteria.Equal("project", project.getCode())
		);
		authDb.delete(new SyncPushRegistrationTable(), criteria);
		PushNotificationService pushService = AppComponents.get(
				PushNotificationService.class);
		pushService.removeUserProject(user.getUserid(), project.getCode());
		return null;
	}

	public List<DatabaseObject> getUsers(ProtocolVersion version,
			Database authDb, User user, BaseProject project, String forUserid,
			String roleStr, String includeInactiveStr) throws HttpException,
			Exception {
		UserController.GetSubjectListInput input =
				UserController.getSubjectListInput(version, authDb, user,
				forUserid, roleStr, includeInactiveStr);
		List<User> users = User.findProjectUsers(project.getCode(), authDb,
				input.getForUser(), input.getRole(), input.isIncludeInactive());
		return UserController.getCompatUserList(version, users);
	}

	public List<DatabaseObject> getSubjects(ProtocolVersion version,
			Database authDb, User user, BaseProject project, String forUserid,
			String includeInactiveStr) throws HttpException, Exception {
		User forUser = User.findAccessibleUser(version, forUserid, authDb,
				user);
		if (!forUser.getUserid().equals(user.getUserid()) &&
				user.getRole() != Role.ADMIN) {
			throw new ForbiddenException();
		}
		boolean includeInactive;
		try {
			includeInactive = TypeConversion.getBoolean(includeInactiveStr);
		} catch (ParseException ex) {
			throw BadRequestException.withInvalidInput(
					new HttpFieldError("includeInactive", ex.getMessage()));
		}
		List<User> subjects = User.findProjectUsers(project.getCode(), authDb,
				forUser, Role.PATIENT, includeInactive);
		return UserController.getCompatUserList(version, subjects);
	}

	/**
	 * Runs the query registerWatchSubjects.
	 *
	 * @param authDb the authentication database
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @return the registration ID
	 * @throws HttpException if the request is invalid
	 * @throws DatabaseException if a database error occurs
	 */
	public String registerWatchSubjects(Database authDb, User user,
			BaseProject project, boolean reset) throws HttpException,
			DatabaseException {
		return WatchSubjectListener.addRegistration(authDb, user,
				project.getCode(), reset);
	}

	/**
	 * Runs the query watchSubjects.
	 *
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param versionName the protocol version
	 * @param project the project code
	 * @param id the registration ID
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public void watchSubjects(final HttpServletRequest request,
			HttpServletResponse response, String versionName, String project,
			String id) throws HttpException, Exception {
		long queryStart = System.currentTimeMillis();
		long queryEnd = queryStart + HANGING_GET_TIMEOUT;
		// verify authentication and input
		WatchSubjectListener listener = QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
						parseWatchSubjectInput(authDb, id, user, baseProject),
				versionName, project, request, response);
		// watch listener
		Object currentWatch = new Object();
		final Object lock = listener.getLock();
		synchronized (lock) {
			listener.setCurrentWatch(currentWatch);
			long now = System.currentTimeMillis();
			while (now < queryEnd &&
					listener.getCurrentWatch() == currentWatch &&
					listener.getSubjectEvents().isEmpty()) {
				lock.wait(queryEnd - now);
				now = System.currentTimeMillis();
			}
			List<SubjectEvent> events;
			if (listener.getCurrentWatch() == currentWatch)
				events = listener.getSubjectEvents();
			else
				events = new ArrayList<>();
			response.setContentType("application/json;charset=UTF-8");
			try (Writer writer = new OutputStreamWriter(
					response.getOutputStream(), StandardCharsets.UTF_8)) {
				ObjectMapper mapper = new ObjectMapper();
				String json = mapper.writeValueAsString(events);
				writer.write(json);
				writer.flush();
			}
			listener.clearSubjectEvents();
		}
	}

	/**
	 * Runs the query unregisterWatchSubjects.
	 *
	 * @param authDb the authentication database
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @param regId the registration ID
	 * @throws HttpException if the request is invalid
	 * @throws DatabaseException if a database error occurs
	 */
	public Object unregisterWatchSubjects(Database authDb, User user,
			BaseProject project, String regId) throws HttpException,
			DatabaseException {
		WatchSubjectListener listener;
		try {
			listener = findWatchSubjectListener(regId, user, project);
		} catch (NotFoundException ex) {
			return null;
		}
		WatchSubjectListener.removeRegistration(authDb, listener);
		return null;
	}

	private WatchSubjectListener parseWatchSubjectInput(Database authDb,
			String regId, User user, BaseProject project) throws HttpException,
			DatabaseException {
		WatchSubjectListener listener = findWatchSubjectListener(regId, user,
				project);
		if (!WatchSubjectListener.setRegistrationWatchTime(authDb,
				listener.getRegistration())) {
			throw new NotFoundException();
		}
		return listener;
	}

	private WatchSubjectListener findWatchSubjectListener(String regId,
			User user, BaseProject project) throws NotFoundException {
		WatchSubjectListener listener = WatchSubjectListener.findListener(
				regId);
		if (listener == null)
			throw new NotFoundException();
		WatchSubjectRegistration reg = listener.getRegistration();
		// check if registration properties correspond to parameters
		if (!reg.getUser().equals(user.getUserid()))
			throw new NotFoundException();
		if (!reg.getProject().equals(project.getCode()))
			throw new NotFoundException();
		return listener;
	}

	/**
	 * Runs the query getTableList.
	 *
	 * @param project the project
	 * @return the names of the database tables
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public List<String> getTableList(BaseProject project)
			throws HttpException, Exception {
		return project.getDatabaseTableNames();
	}

	/**
	 * Runs the query getTableSpec.
	 *
	 * @param project the project
	 * @param table the table name
	 * @return the table specification
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public TableSpec getTableSpec(BaseProject project, String table)
			throws HttpException, Exception {
		DatabaseTableDef<?> tableDef = project.findTable(table);
		if (tableDef == null) {
			throw new NotFoundException(String.format(
					"Table \"%s\" not found in project \"%s\"",
					table, project.getCode()));
		}
		return TableSpec.fromDatabaseTableDef(tableDef);
	}

	/**
	 * Runs the query getRecords or getRecordsWithFilter.
	 *
	 * @param authDb the authentication database
	 * @param db the project database or null
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @param table the name of the table
	 * @param subject the user ID or email address of the subject or an empty
	 * string or null
	 * @param start the start time or an empty string or null
	 * @param end the end time or an empty string or null
	 * @param request if getRecordsWithFilter was called, this is the request
	 * with the filter in the content. Otherwise it's null.
	 * @param response the HTTP response to which the records should be written
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Object getRecords(ProtocolVersion version, Database authDb,
			Database db, User user, BaseProject project, String table,
			String subject, String start, String end,
			HttpServletRequest request, HttpServletResponse response)
			throws HttpException, Exception {
		List<? extends DatabaseObject> records = getRecords(version, authDb,
				db, user, project, table, subject, start, end, request);
		response.setContentType("application/json");
		try (Writer writer = new OutputStreamWriter(response.getOutputStream(),
				StandardCharsets.UTF_8)) {
			writer.write("[");
			DatabaseObjectMapper dbMapper = new DatabaseObjectMapper();
			ObjectMapper jsonMapper = new ObjectMapper();
			boolean first = true;
			for (DatabaseObject record : records) {
				if (!first)
					writer.write(",");
				else
					first = false;
				Map<String,Object> map = dbMapper.objectToMap(record, true);
				String json = jsonMapper.writeValueAsString(map);
				writer.write(json);
			}
			writer.write("]");
		}
		return null;
	}

	public static List<? extends DatabaseObject> getRecords(
			ProtocolVersion version, Database authDb, Database db, User user,
			BaseProject project, String table, String subject, String start,
			String end, HttpServletRequest request) throws HttpException,
			Exception {
		TableSelectCriteria tableCriteria = getTableSelectCriteria(version,
				authDb, user, project, table, subject, start, end, request,
				Arrays.asList("filter", "sort", "limit"), true);
		// TODO stream to response, add Database.selectCursor()
		List<? extends DatabaseObject> result = db.select(
				tableCriteria.tableDef, tableCriteria.criteria,
				tableCriteria.limit, tableCriteria.sort);
		setCompatUser(version, subject, tableCriteria.subjectUser, result);
		return result;
	}

	private static void setCompatUser(ProtocolVersion version,
			String subjectName, User subjectUser,
			List<? extends DatabaseObject> records) {
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal())
			return;
		for (DatabaseObject record : records) {
			List<String> fields = DatabaseFieldScanner.getDatabaseFieldNames(
					record.getClass());
			if (!fields.contains("user"))
				return;
			String compatUser;
			if (subjectUser.getUserid().contains("@"))
				compatUser = subjectUser.getUserid();
			else if (subjectName != null && !subjectName.isEmpty())
				compatUser = subjectName;
			else
				compatUser = subjectUser.getEmail();
			PropertyWriter.writeProperty(record, "user", compatUser);
		}
	}

	/**
	 * Runs the query getRecord.
	 *
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param db the project database or null
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @param table the name of the table
	 * @param recordId the record ID
	 * @param subject the user ID or email address of the subject or an empty
	 * string or null
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Map<?,?> getRecord(ProtocolVersion version, Database authDb,
			Database db, User user, BaseProject project, String table,
			String recordId, String subject) throws HttpException, Exception {
		DatabaseTableDef<?> tableDef = project.findTable(table);
		if (tableDef == null) {
			throw new NotFoundException(String.format(
					"Table \"%s\" not found in project \"%s\"",
					table, project.getCode()));
		}
		DatabaseObject record;
		DatabaseCache cache = DatabaseCache.getInstance();
		List<String> fields = cache.getTableFields(db, table);
		if (fields.contains("user")) {
			ProjectUserAccess userAccess = User.findAccessibleProjectUser(
					version, subject, project.getCode(), table, AccessMode.R,
					authDb, user);
			User subjectUser = userAccess.getUser();
			DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", subjectUser.getUserid()),
					new DatabaseCriteria.Equal("id", recordId));
			record = db.selectOne(tableDef, criteria, null);
			if (record == null) {
				throw new NotFoundException(String.format(
						"Record with ID \"%s\" not found for project %s, table %s, user %s",
						recordId, project.getCode(), table,
						subjectUser.getUserid(version)));
			}
			userAccess.checkMatchesRange(record);
		} else {
			checkPermissionWriteResourceTable(authDb, user, project.getCode(),
					table);
			DatabaseCriteria criteria = new DatabaseCriteria.Equal(
					"id", recordId);
			record = db.selectOne(tableDef, criteria, null);
			if (record == null) {
				throw new NotFoundException(String.format(
						"Record with ID \"%s\" not found for project %s, table %s",
						recordId, project.getCode(), table));
			}
		}
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		return mapper.objectToMap(record, true);
	}

	/**
	 * Result of {@link
	 * #getTableSelectCriteria(ProtocolVersion, Database, User, BaseProject, String, String, String, String, HttpServletRequest, List, boolean)
	 * getTableSelectCriteria()}.
	 */
	private static class TableSelectCriteria {
		public DatabaseTableDef<?> tableDef;
		public DatabaseCriteria criteria;
		public DatabaseSort[] sort;
		public int limit;
		public User subjectUser;

		public TableSelectCriteria(DatabaseTableDef<?> tableDef,
				DatabaseCriteria criteria, DatabaseSort[] sort, int limit,
				User subjectUser) {
			this.tableDef = tableDef;
			this.criteria = criteria;
			this.sort = sort;
			this.limit = limit;
			this.subjectUser = subjectUser;
		}
	}

	/**
	 * Returns a table definition and database criteria for a select or delete
	 * query. The specified table, subject, start and end come from user input.
	 * This method will validate them and throw an HttpException in case of
	 * invalid input.
	 *
	 * @param authDb the authentication database
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @param table the table name
	 * @param subject the user ID or email address of the subject or an empty
	 * string or null
	 * @param start the start time as an ISO date/time string, or the start date
	 * as an SQL date string, or an empty string or null
	 * @param end the end time as an ISO date/time string, or the end date as an
	 * SQL date string, or an empty string or null
	 * @param request if a select/delete query was called with a filter, this is
	 * the request with the filter in the content. Otherwise it's null.
	 * @param allowedContentParams the allowed parameters in the request body.
	 * This should be a set of "filter", "sort" and "limit".
	 * @param isRead true if the select criteria will be used for a select
	 * query, which does not require write permission for resource tables; false
	 * if the select criteria will be used for a delete query
	 * @return the table definition and database criteria
	 * @throws HttpException if the request is invalid
	 * @throws DatabaseException if a database error occurs
	 * @throws IOException if an error occurs while reading the request content
	 */
	private static TableSelectCriteria getTableSelectCriteria(
			ProtocolVersion version, Database authDb, User user,
			BaseProject project, String table, String subject, String start,
			String end, HttpServletRequest request,
			List<String> allowedContentParams, boolean isRead)
			throws HttpException, DatabaseException, IOException {
		DatabaseTableDef<?> tableDef = project.findTable(table);
		if (tableDef == null) {
			throw new NotFoundException(String.format(
					"Table \"%s\" not found in project \"%s\"",
					table, project.getCode()));
		}
		List<String> fields = DatabaseFieldScanner.getDatabaseFieldNames(
				tableDef.getDataClass());
		boolean isUserTable = fields.contains("user");
		boolean isTimeTable = Sample.class.isAssignableFrom(
				tableDef.getDataClass());
		boolean isUtcTable = UTCSample.class.isAssignableFrom(
				tableDef.getDataClass());
		List<DatabaseCriteria> andCriteria = new ArrayList<>();
		StringBuilder errorBuilder = new StringBuilder();
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		ParameterParser paramParser = new ParameterParser();
		Object startObj = null;
		if (isTimeTable && start != null && !start.isEmpty()) {
			try {
				startObj = paramParser.parseSelectDateTime(isUtcTable, start);
			} catch (ParseException ex) {
				if (!errorBuilder.isEmpty())
					errorBuilder.append("\n");
				errorBuilder.append("Invalid value for parameter \"start\": " +
						ex.getMessage());
				fieldErrors.add(new HttpFieldError("start", ex.getMessage()));
			}
		}
		LocalDateTime startTime = null;
		if (startObj instanceof ZonedDateTime && isUtcTable) {
			long startMillis = ((ZonedDateTime)startObj).toInstant()
					.toEpochMilli();
			startTime = ((ZonedDateTime)startObj).toLocalDateTime();
			andCriteria.add(new DatabaseCriteria.GreaterEqual(
					"utcTime", startMillis));
		} else if (startObj instanceof ZonedDateTime) {
			String startTimeStr = ((ZonedDateTime)startObj).toLocalDateTime()
					.format(Sample.LOCAL_TIME_FORMAT);
			startTime = ((ZonedDateTime)startObj).toLocalDateTime();
			andCriteria.add(new DatabaseCriteria.GreaterEqual(
					"localTime", startTimeStr));
		} else if (startObj != null) {
			String startTimeStr = ((LocalDateTime)startObj).format(
					Sample.LOCAL_TIME_FORMAT);
			startTime = (LocalDateTime)startObj;
			andCriteria.add(new DatabaseCriteria.GreaterEqual(
					"localTime", startTimeStr));
		}
		Object endObj = null;
		if (isTimeTable && end != null && !end.isEmpty()) {
			try {
				endObj = paramParser.parseSelectDateTime(isUtcTable, end);
			} catch (ParseException ex) {
				if (!errorBuilder.isEmpty())
					errorBuilder.append("\n");
				errorBuilder.append("Invalid value for parameter \"end\": " +
						ex.getMessage());
				fieldErrors.add(new HttpFieldError("end", ex.getMessage()));
			}
		}
		LocalDateTime endTime = null;
		if (endObj instanceof ZonedDateTime && isUtcTable) {
			long endMillis = ((ZonedDateTime)endObj).toInstant().toEpochMilli();
			endTime = ((ZonedDateTime)endObj).toLocalDateTime();
			andCriteria.add(new DatabaseCriteria.LessThan(
					"utcTime", endMillis));
		} else if (endObj instanceof ZonedDateTime) {
			String endTimeStr = ((ZonedDateTime)endObj).toLocalDateTime()
					.format(Sample.LOCAL_TIME_FORMAT);
			endTime = ((ZonedDateTime)endObj).toLocalDateTime();
			andCriteria.add(new DatabaseCriteria.LessThan(
					"localTime", endTimeStr));
		} else if (endObj != null) {
			String endTimeStr = ((LocalDateTime)endObj).format(
					Sample.LOCAL_TIME_FORMAT);
			endTime = (LocalDateTime)endObj;
			andCriteria.add(new DatabaseCriteria.LessThan(
					"localTime", endTimeStr));
		}
		if (!fieldErrors.isEmpty()) {
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
					errorBuilder.toString());
			error.setFieldErrors(fieldErrors);
			throw new BadRequestException(error);
		}
		User subjectUser = null;
		if (isUserTable) {
			ProjectUserAccess userAccess = User.findAccessibleProjectUser(
					version, subject, project.getCode(), table, AccessMode.R,
					authDb, user);
			userAccess.checkMatchesRange(startTime, endTime);
			subjectUser = userAccess.getUser();
			andCriteria.add(0, new DatabaseCriteria.Equal("user",
					subjectUser.getUserid()));
		} else if (!isRead) {
			checkPermissionWriteResourceTable(authDb, user, project.getCode(),
					table);
		}
		String content = null;
		if (request != null)
			content = HttpContentReader.readString(request);
		Object filterObj = null;
		Object sortObj = null;
		Object limitObj = null;
		if (content != null && !content.isEmpty()) {
			Map<?,?> map;
			try {
				map = JsonMapper.parse(content, Map.class);
			} catch (ParseException ex) {
				HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
						"Content is not a valid JSON object: " +
								ex.getMessage());
				throw new BadRequestException(error);
			}
			List<String> invalidKeys = new ArrayList<>();
			for (Object key : map.keySet()) {
				if (!allowedContentParams.contains((String)key))
					invalidKeys.add(key == null ? "null" : key.toString());
			}
			if (!invalidKeys.isEmpty()) {
				HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
						"Invalid parameters in JSON content: " +
								String.join(", ", invalidKeys));
				throw new BadRequestException(error);
			}
			filterObj = map.get("filter");
			sortObj = map.get("sort");
			limitObj = map.get("limit");
		}
		SelectFilterParser parser = new SelectFilterParser(tableDef);
		if (filterObj != null) {
			if (!(filterObj instanceof Map<?,?> map)) {
				throw new BadRequestException(ErrorCode.INVALID_INPUT,
						"Filter is not a JSON object");
			}
			try {
				andCriteria.add(parser.parseFilter(map));
			} catch (ParseException ex) {
				HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
						"Invalid filter: " + ex.getMessage());
				throw new BadRequestException(error);
			}
		}
		DatabaseSort[] sort = null;
		if (sortObj != null) {
			try {
				sort = parser.parseSort(sortObj);
			} catch (ParseException ex) {
				HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
						"Invalid sort: " + ex.getMessage());
				throw new BadRequestException(error);
			}
		}
		int limit = 0;
		if (limitObj != null) {
			try {
				limit = JsonMapper.convert(limitObj, Integer.class);
			} catch (ParseException ex) {
				HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
						"Invalid limit: " + ex.getMessage());
				throw new BadRequestException(error);
			}
		}
		DatabaseCriteria criteria = null;
		if (!andCriteria.isEmpty()) {
			criteria = new DatabaseCriteria.And(andCriteria.toArray(
					new DatabaseCriteria[0]));
		}
		Class<?> dataClass = tableDef.getDataClass();
		if (sort == null) {
			String sortField;
			if (UTCSample.class.isAssignableFrom(dataClass) &&
					criteria.containsColumn("utcTime")) {
				sortField = "utcTime";
			} else if (Sample.class.isAssignableFrom(dataClass) &&
					criteria.containsColumn("localTime")) {
				sortField = "localTime";
			} else if (UTCSample.class.isAssignableFrom(dataClass)) {
				sortField = "utcTime";
			} else if (Sample.class.isAssignableFrom(dataClass)) {
				sortField = "localTime";
			} else {
				sortField = "id";
			}
			sort = new DatabaseSort[] { new DatabaseSort(sortField, true) };
		}
		return new TableSelectCriteria(tableDef, criteria, sort, limit,
				subjectUser);
	}

	/**
	 * Runs the query insertRecords.
	 *
	 * @param version the protocol version
	 * @param request the HTTP request
	 * @param authDb the authentication database
	 * @param db the project database or null
	 * @param user the user who is currently logged in
	 * @param project the project code
	 * @param table the name of the table
	 * @param subject the user ID or email address of the subject or null
	 * @return the record IDs
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public List<String> insertRecords(ProtocolVersion version,
			HttpServletRequest request, Database authDb, Database db, User user,
			BaseProject project, String table, String subject)
			throws HttpException, Exception {
		DatabaseTableDef<?> tableDef = project.findTable(table);
		if (tableDef == null) {
			throw new NotFoundException(String.format(
					"Table \"%s\" not found in project \"%s\"",
					table, project.getCode()));
		}
		User subjectUser = null;
		DatabaseCache cache = DatabaseCache.getInstance();
		List<String> fields = cache.getTableFields(db, table);
		if (fields.contains("user")) {
			ProjectUserAccess userAccess = User.findAccessibleProjectUser(
					version, subject, project.getCode(), table, AccessMode.W,
					authDb, user);
			userAccess.checkMatchesRange(null, null);
			subjectUser = userAccess.getUser();
		} else {
			checkPermissionWriteResourceTable(authDb, user, project.getCode(),
					table);
		}
		List<String> idList = new ArrayList<>();
		InputStream input = request.getInputStream();
		JsonObjectStreamReader jsonReader = null;
		try {
			jsonReader = new JsonObjectStreamReader(input);
			jsonReader.readToken(JsonAtomicToken.Type.START_LIST);
			while (jsonReader.getToken().getType() !=
					JsonAtomicToken.Type.END_LIST) {
				insertRecordBatch(version, user, jsonReader, db, tableDef,
						subjectUser, idList);
			}
			return idList;
		} catch (JsonParseException ex) {
			String msg = "Invalid records: " + ex.getMessage();
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
			throw new BadRequestException(error);
		} finally {
			if (jsonReader != null)
				jsonReader.close();
			else
				input.close();
		}
	}

	private static void checkPermissionWriteResourceTable(Database authDb,
			User user, String project, String table) throws HttpException,
			DatabaseException {
		if (user.getRole() == Role.ADMIN)
			return;
		PermissionManager permMgr = PermissionManager.getInstance();
		Map<String,Object> permParams = new LinkedHashMap<>();
		permParams.put("project", project);
		permParams.put("table", table);
		permMgr.checkPermission(authDb, user.getUserid(),
				PermissionRecord.PERMISSION_WRITE_RESOURCE_TABLE,
				permParams);
	}

	/**
	 * Runs the query updateRecord.
	 *
	 * @param version the protocol version
	 * @param request the HTTP request
	 * @param authDb the authentication database
	 * @param db the project database or null
	 * @param user the user who is currently logged in
	 * @param project the project code
	 * @param table the name of the table
	 * @param recordId the record ID that is updated
	 * @param subject the user ID or email address of the subject or null
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Object updateRecord(ProtocolVersion version,
			HttpServletRequest request, Database authDb, Database db, User user,
			BaseProject project, String table, String recordId, String subject)
			throws HttpException, Exception {
		DatabaseTableDef<?> tableDef = project.findTable(table);
		if (tableDef == null) {
			throw new NotFoundException(String.format(
					"Table \"%s\" not found in project \"%s\"",
					table, project.getCode()));
		}
		ProjectUserAccess userAccess = null;
		User subjectUser = null;
		DatabaseObject record;
		DatabaseCache cache = DatabaseCache.getInstance();
		List<String> fields = cache.getTableFields(db, table);
		if (fields.contains("user")) {
			userAccess = User.findAccessibleProjectUser(version, subject,
					project.getCode(), table, AccessMode.W, authDb, user);
			subjectUser = userAccess.getUser();
			DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", subjectUser.getUserid()),
					new DatabaseCriteria.Equal("id", recordId));
			record = db.selectOne(tableDef, criteria, null);
			if (record == null) {
				throw new NotFoundException(String.format(
						"Record with ID \"%s\" not found for project %s, table %s, user %s",
						recordId, project.getCode(), table,
						subjectUser.getUserid(version)));
			}
			userAccess.checkMatchesRange(record);
		} else {
			checkPermissionWriteResourceTable(authDb, user, project.getCode(),
					table);
			DatabaseCriteria criteria = new DatabaseCriteria.Equal(
					"id", recordId);
			record = db.selectOne(tableDef, criteria, null);
			if (record == null) {
				throw new NotFoundException(String.format(
						"Record with ID \"%s\" not found for project %s, table %s",
						recordId, project.getCode(), table));
			}
		}
		Map<?,?> recordMap;
		try (InputStream input = request.getInputStream()) {
			ObjectMapper mapper = new ObjectMapper();
			recordMap = mapper.readValue(input, Map.class);
		}
		DatabaseObject updatedRecord = createUpdateRecord(version, user,
				recordMap, tableDef, recordId, subjectUser);
		if (userAccess != null)
			userAccess.checkMatchesRange(updatedRecord);
		db.update(table, updatedRecord);
		return null;
	}

	/**
	 * Runs the query deleteRecords or deleteRecordsWithFilter.
	 *
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param db the project database or null
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @param table the name of the table
	 * @param subject the user ID or email address of the subject or an empty
	 * string or null
	 * @param start the start time or an empty string or null
	 * @param end the end time or an empty string or null
	 * @param request if deleteRecordsWithFilter was called, this is the request
	 * with the filter in the content. Otherwise it's null.
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Object deleteRecords(ProtocolVersion version, Database authDb,
			Database db, User user, BaseProject project, String table,
			String subject, String start, String end,
			HttpServletRequest request) throws HttpException, Exception {
		DatabaseTableDef<?> tableDef = project.findTable(table);
		if (tableDef == null) {
			throw new NotFoundException(String.format(
					"Table \"%s\" not found in project \"%s\"",
					table, project.getCode()));
		}
		TableSelectCriteria tableCriteria = getTableSelectCriteria(version,
				authDb, user, project, table, subject, start, end, request,
				Collections.singletonList("filter"), false);
		db.delete(tableCriteria.tableDef, tableCriteria.criteria);
		return null;
	}

	/**
	 * Runs the query deleteRecord.
	 *
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param db the project database or null
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @param table the name of the table
	 * @param recordId the record ID to delete
	 * @param subject the user ID or email address of the subject or an empty
	 * string or null
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Object deleteRecord(ProtocolVersion version, Database authDb,
			Database db, User user, BaseProject project, String table,
			String recordId, String subject) throws HttpException, Exception {
		DatabaseTableDef<?> tableDef = project.findTable(table);
		if (tableDef == null) {
			throw new NotFoundException(String.format(
					"Table \"%s\" not found in project \"%s\"",
					table, project.getCode()));
		}
		DatabaseCriteria criteria;
		DatabaseCache cache = DatabaseCache.getInstance();
		List<String> fields = cache.getTableFields(db, table);
		if (fields.contains("user")) {
			ProjectUserAccess userAccess = User.findAccessibleProjectUser(version,
					subject, project.getCode(), table, AccessMode.W, authDb, user);
			User subjectUser = userAccess.getUser();
			criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", subjectUser.getUserid()),
					new DatabaseCriteria.Equal("id", recordId)
			);
			DatabaseObject record = db.selectOne(tableDef, criteria, null);
			if (record == null)
				return null;
			userAccess.checkMatchesRange(record);
		} else {
			checkPermissionWriteResourceTable(authDb, user, project.getCode(),
					table);
			criteria = new DatabaseCriteria.Equal("id", recordId);
			DatabaseObject record = db.selectOne(tableDef, criteria, null);
			if (record == null)
				return null;
		}
		db.delete(tableDef, criteria);
		return null;
	}

	/**
	 * Runs the query purgeTable.
	 *
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param db the project database or null
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @param table the name of the table
	 * @param subject the user ID or email address of the subject or an empty
	 * string or null
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Object purgeTable(ProtocolVersion version, Database authDb,
			Database db, User user, BaseProject project, String table,
			String subject) throws HttpException, Exception {
		DatabaseTableDef<?> tableDef = project.findTable(table);
		if (tableDef == null) {
			throw new NotFoundException(String.format(
					"Table \"%s\" not found in project \"%s\"",
					table,
					project.getCode()));
		}
		DatabaseCache cache = DatabaseCache.getInstance();
		List<String> fields = cache.getTableFields(db, table);
		if (fields.contains("user")) {
			User subjectUser = User.findAccessibleUser(version, subject, authDb,
					user);
			db.purgeUserTable(tableDef.getName(), subjectUser.getUserid());
		} else {
			checkPermissionWriteResourceTable(authDb, user, project.getCode(),
					table);
			db.purgeResourceTable(table);
		}
		return null;
	}

	/**
	 * Runs the query getFirstRecord(WithFilter) or getLastRecord(WithFilter).
	 *
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param db the project database or null
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @param table the name of the table
	 * @param subject the user ID or email address of the subject or an empty
	 * string or null
	 * @param start the start time as an ISO date/time string, or the start date
	 * as an SQL date string, or an empty string or null
	 * @param end the end time as an ISO date/time string, or the end date as an
	 * SQL date string, or an empty string or null
	 * @param getFirst true to get the first record, false to get the last
	 * record
	 * @param request if getFirst|LastRecordWithFilter was called, this is the
	 * request with the filter in the content. Otherwise it's null.
	 * @return the first or last record or null
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Map<?,?> getFirstLastRecord(ProtocolVersion version, Database authDb,
			Database db, User user, BaseProject project, String table,
			String subject, String start, String end, boolean getFirst,
			HttpServletRequest request) throws HttpException, Exception {
		TableSelectCriteria tableCriteria = getTableSelectCriteria(version,
				authDb, user, project, table, subject, start, end, request,
				Arrays.asList("filter", "sort"), true);
		if (!getFirst)
			tableCriteria.sort = DatabaseSort.reverse(tableCriteria.sort);
		DatabaseObject record = db.selectOne(tableCriteria.tableDef,
				tableCriteria.criteria, tableCriteria.sort);
		if (record == null)
			return null;
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		return mapper.objectToMap(record, true);
	}

	/**
	 * Reads a batch of record maps from the JSON reader, validates the maps
	 * and converts them to database objects (see {@link
	 * #createInsertRecord(ProtocolVersion, User, Map, DatabaseTableDef, User)
	 * createInsertRecord()}, and inserts them into the database. The record IDs
	 * will be added to the specified idList.
	 *
	 * @param version the protocol version
	 * @param user the calling user
	 * @param jsonReader the JSON reader, positioned at the start of a record
	 * map
	 * @param db the project database
	 * @param table the table
	 * @param subjectUser the subject user that the records should belong to, or
	 * null if the table is not a user table
	 * @param idList list with record IDs
	 * @throws JsonParseException if the JSON input is invalid
	 * @throws HttpException if a parsed record map is invalid
	 * @throws DatabaseException if a database error occurs
	 * @throws IOException if a reading error occurs
	 */
	private void insertRecordBatch(ProtocolVersion version, User user,
			JsonObjectStreamReader jsonReader, Database db,
			DatabaseTableDef<?> table, User subjectUser, List<String> idList)
			throws JsonParseException, HttpException, DatabaseException,
			IOException {
		List<Map<?,?>> recordMaps = new ArrayList<>();
		while (jsonReader.getToken().getType() !=
				JsonAtomicToken.Type.END_LIST &&
				recordMaps.size() < BATCH_SIZE) {
			recordMaps.add(jsonReader.readObject());
			if (jsonReader.getToken().getType() !=
					JsonAtomicToken.Type.END_LIST) {
				jsonReader.readToken(JsonAtomicToken.Type.LIST_ITEM_SEPARATOR);
			}
		}
		List<DatabaseObject> records = new ArrayList<>();
		while (!recordMaps.isEmpty()) {
			Map<?,?> map = recordMaps.remove(0);
			records.add(createInsertRecord(version, user, map, table,
					subjectUser));
		}
		db.insert(table.getName(), records);
		for (DatabaseObject record : records) {
			idList.add(record.getId());
		}
	}

	/**
	 * Validates a record map inserted by a user and returns a database object
	 * for the specified table. If the table is a user table, this method
	 * ensures that the field "user" is set. If the data class of the table is a
	 * {@link Sample Sample}, it also ensures that "localTime" is set. If the
	 * data class is a {@link UTCSample UTCSample}, it also ensures that
	 * "utcTime" and "timezone" are set. Furthermore it sets the ID field to
	 * null.
	 *
	 * <p>For tables with {@link LocalTimeSample LocalTimeSample}, the user must
	 * have set "localTime". For tables with {@link UTCSample UTCSample}, the
	 * user must have set "timezone", and "localTime" or "utcTime". For any of
	 * these fields that were set, this method validates the values.</p>
	 *
	 * @param version the protocol version
	 * @param user the calling user
	 * @param recordMap the inserted record map
	 * @param table the table to which the record should be written
	 * @param subjectUser the subject user that the record should belong to,
	 * or null if the table is not a user table
	 * @return the database object
	 * @throws HttpException if the record map has invalid input
	 */
	private DatabaseObject createInsertRecord(ProtocolVersion version,
			User user, Map<?,?> recordMap, DatabaseTableDef<?> table,
			User subjectUser) throws HttpException {
		DatabaseObject result = createWriteDatabaseObject(version, user,
				subjectUser, recordMap, table);
		result.setId(null);
		return result;
	}

	/**
	 * Validates a record map updated by a user and returns a database object
	 * for the specified table. If the table is a user table, this method
	 * ensures that the field "user" is set. If the data class of the table is a
	 * {@link Sample Sample}, it also ensures that "localTime" is set. If the
	 * data class is a {@link UTCSample UTCSample}, it also ensures that
	 * "utcTime" and "timezone" are set.
	 *
	 * <p>If the "id" and "user" were already in the record map, this method
	 * validates that they have not changed.</p>
	 *
	 * <p>For tables with {@link LocalTimeSample LocalTimeSample}, the user must
	 * have set "localTime". For tables with {@link UTCSample UTCSample}, the
	 * user must have set "timezone", and "localTime" or "utcTime". For any of
	 * these fields that were set, this method validates the values.</p>
	 *
	 * @param version the protocol version
	 * @param user the calling user
	 * @param recordMap the updated record map
	 * @param table the table to which the record should be written
	 * @param subjectUser the subject user that the record should belong to,
	 * or null if the table is not a user table
	 * @param recordId the record ID that is updated
	 * @return the record
	 * @throws HttpException if the record map has invalid input
	 */
	private DatabaseObject createUpdateRecord(ProtocolVersion version,
			User user, Map<?,?> recordMap, DatabaseTableDef<?> table,
			String recordId, User subjectUser) throws HttpException {
		DatabaseObject record = createWriteDatabaseObject(version, user,
				subjectUser, recordMap, table);
		if (record.getId() != null && !record.getId().equals(recordId)) {
			String msg = "Changing record ID not allowed";
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
			throw new BadRequestException(error);
		}
		record.setId(recordId);
		return record;
	}

	/**
	 * Converts the specified map from a JSON object to a DatabaseObject for
	 * the specified table. This method is called when a record is inserted or
	 * updated. It calls the DatabaseObjectMapper and verifies the data types.
	 *
	 * <p>If the table is a user table, then this method validate the "user"
	 * field. If the "user" field is specified, it should match the specified
	 * subject user. If the field is empty, this method will set the user ID of
	 * the subject user.</p>
	 *
	 * <p>Then it validates the time fields using {@link
	 * CommonCrudController#validateWriteRecordTime(ZoneId, DatabaseObject, Map)
	 * CommonCrudController.validateWriteRecordTime()}.</p>
	 *
	 * <p>If the mapping or user and time validation fails, this method throws a
	 * BadRequestException.</p>
	 *
	 * @param version the protocol version
	 * @param user the calling user
	 * @param subject the subject user, or null if the table is not a user table
	 * @param map the map
	 * @param table the database table
	 * @return the database object
	 * @throws HttpException if the mapping fails
	 */
	private <T extends DatabaseObject> T createWriteDatabaseObject(
			ProtocolVersion version, User user, User subject, Map<?,?> map,
			DatabaseTableDef<T> table) throws HttpException {
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		T result;
		try {
			result = mapper.mapToObject(map, table.getDataClass(), true);
		} catch (DatabaseFieldException ex) {
			String msg = String.format(
					"Invalid value in field \"%s\" for table \"%s\": ",
					ex.getField(), table.getName()) + map;
			Logger logger = AppComponents.getLogger(SenSeeActContext.LOGTAG);
			logger.error(msg + ": " + ex.getMessage());
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
			String fieldMsg = String.format("Invalid value for table \"%s\": ",
					table.getName()) + map.get(ex.getField());
			error.addFieldError(new HttpFieldError(ex.getField(), fieldMsg));
			throw new BadRequestException(error);
		}
		if (subject != null) {
			String subjectName = (String)PropertyReader.readProperty(result,
					"user");
			if (subjectName == null) {
				PropertyWriter.writeProperty(result, "user",
						subject.getUserid());
			} else if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal()) {
				if (!subjectName.equals(subject.getUserid())) {
					String msg = String.format(
							"Record user \"%s\" does not match query subject \"%s\"",
							subjectName, subject.getUserid()) + ": " + result;
					HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
							msg);
					throw new BadRequestException(error);
				}
			} else {
				if (!subjectName.toLowerCase().equals(subject.getEmail())) {
					String msg = String.format(
							"Record user \"%s\" does not match query subject \"%s\"",
							subjectName, subject.getEmail()) + ": " + result;
					HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
							msg);
					throw new BadRequestException(error);
				}
				PropertyWriter.writeProperty(result, "user",
						subject.getUserid());
			}
		}
		ZoneId defaultTz = subject != null ? subject.toTimeZone() :
				user.toTimeZone();
		CommonCrudController.validateWriteRecordTime(defaultTz, result, map);
		return result;
	}

	/**
	 * Runs the query registerWatchTable.
	 *
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param projectDb the project database
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @param table the name of the project table
	 * @param subject the user ID or email address of the subject or an empty
	 * string or null (ignored if "anySubject" is set)
	 * @param anySubject true if database actions for any subject should be
	 * watched, false if only the actions for the specified subject should be
	 * watched
	 * @return the registration ID
	 * @throws HttpException if the request is invalid
	 * @throws DatabaseException if a database error occurs
	 */
	public String registerWatchTable(ProtocolVersion version,
			Database authDb, Database projectDb, User user, BaseProject project,
			String table, String subject, boolean anySubject,
			String callbackUrl, boolean reset)
			throws HttpException, DatabaseException {
		DatabaseTableDef<?> tableDef = project.findTable(table);
		if (tableDef == null) {
			String msg = "Table \"%s\" not found in project \"%s\"";
			throw new NotFoundException(String.format(msg, table,
					project.getCode()));
		}
		String subjectUserid = null;
		if (anySubject) {
			if (user.getRole() != Role.ADMIN) {
				throw new ForbiddenException(
						"Watch table for any user not allowed");
			}
		} else {
			ProjectUserAccess userAccess = User.findAccessibleProjectUser(
					version, subject, project.getCode(), table, AccessMode.R,
					authDb, user);
			userAccess.checkMatchesRange(null, null);
			subjectUserid = userAccess.getUser().getUserid();
		}
		if (callbackUrl != null && !callbackUrl.isEmpty()) {
			try {
				callbackUrl = new URI(callbackUrl).toURL().toString();
			} catch (URISyntaxException | MalformedURLException ex) {
				throw BadRequestException.withInvalidInput(new HttpFieldError(
						"callbackUrl", "Invalid value: " + callbackUrl));
			}
		} else {
			callbackUrl = null;
		}
		return WatchTableListener.addRegistration(authDb, projectDb,
				user.getUserid(), project.getCode(), table, callbackUrl, reset,
				subjectUserid);
	}

	/**
	 * Runs the query watchTable.
	 *
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param versionName the protocol version
	 * @param project the project code
	 * @param table the table name
	 * @param id the registration ID
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public void watchTable(final HttpServletRequest request,
			HttpServletResponse response, String versionName, String project,
			String table, String id) throws HttpException, Exception {
		long queryStart = System.currentTimeMillis();
		long queryEnd = queryStart + HANGING_GET_TIMEOUT;
		// verify authentication and input
		WatchTableListener listener = QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				parseWatchTableInput(authDb, projectDb, id, user, baseProject,
						table),
				versionName, project, request, response);
		// watch listener
		Object currentWatch = new Object();
		final Object lock = listener.getLock();
		List<String> triggeredSubjects;
		synchronized (lock) {
			listener.setCurrentWatch(currentWatch);
			long now = System.currentTimeMillis();
			while (now < queryEnd &&
					listener.getCurrentWatch() == currentWatch &&
					listener.getTriggeredSubjects().isEmpty()) {
				lock.wait(queryEnd - now);
				now = System.currentTimeMillis();
			}
			if (listener.getCurrentWatch() == currentWatch) {
				triggeredSubjects = new ArrayList<>(
						listener.getTriggeredSubjects());
			} else {
				triggeredSubjects = new ArrayList<>();
			}
			listener.clearTriggeredSubjects();
		}
		List<String> result = QueryRunner.runAuthQuery(
				(version, authDb, user, authDetails) -> {
					if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal()) {
						return triggeredSubjects;
					} else {
						UserCache userCache = UserCache.getInstance();
						List<String> users = new ArrayList<>();
						for (String subject : triggeredSubjects) {
							users.add(userCache.findByUserid(subject)
									.getEmail());
						}
						return users;
					}
				},
				versionName, request, response);
		response.setContentType("application/json;charset=UTF-8");
		try (Writer writer = new OutputStreamWriter(
				response.getOutputStream(), StandardCharsets.UTF_8)) {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(result);
			writer.write(json);
			writer.flush();
		}
	}

	/**
	 * Runs the query unregisterWatchTable.
	 *
	 * @param authDb the authentication database
	 * @param projectDb the project database
	 * @param user the user who is currently logged in
	 * @param project the project
	 * @param table the name of the project table
	 * @param regId the registration ID
	 * @throws HttpException if the request is invalid
	 * @throws DatabaseException if a database error occurs
	 */
	public Object unregisterWatchTable(Database authDb, Database projectDb,
			User user, BaseProject project, String table, String regId)
			throws HttpException, DatabaseException {
		WatchTableListener listener;
		try {
			listener = findWatchTableListener(projectDb, regId, user, project,
					table);
		} catch (NotFoundException ex) {
			return null;
		}
		WatchTableListener.removeRegistration(authDb, projectDb, listener);
		return null;
	}

	private WatchTableListener parseWatchTableInput(Database authDb,
			Database projectDb, String regId, User user, BaseProject project,
			String table) throws HttpException, DatabaseException {
		WatchTableListener listener = findWatchTableListener(projectDb, regId,
				user, project, table);
		WatchTableRegistration reg = listener.getRegistration();
		// check if user can still access the subject specified in the
		// registration
		if (reg.getSubject() == null) {
			if (user.getRole() != Role.ADMIN) {
				throw new ForbiddenException(
						"Watch table for any user not allowed");
			}
		} else {
			ProjectUserAccess userAccess =
					User.findAccessibleProjectUserByUserid(reg.getSubject(),
							project.getCode(), table, AccessMode.R, authDb, user);
			userAccess.checkMatchesRange(null, null);
		}
		if (!WatchTableListener.setRegistrationWatchTime(authDb,
				projectDb, reg)) {
			throw new NotFoundException();
		}
		return listener;
	}

	private WatchTableListener findWatchTableListener(Database projectDb,
			String regId, User user, BaseProject project, String table)
			throws NotFoundException {
		WatchTableListener listener = WatchTableListener.findListener(projectDb,
				regId);
		if (listener == null)
			throw new NotFoundException();
		WatchTableRegistration reg = listener.getRegistration();
		// check if registration properties correspond to parameters
		if (!reg.getUser().equals(user.getUserid()))
			throw new NotFoundException();
		if (!reg.getTable().equals(table))
			throw new NotFoundException();
		// check if the table in the registration still exists
		DatabaseTableDef<?> tableDef = project.findTable(table);
		if (tableDef == null) {
			String msg = "Table \"%s\" not found in project \"%s\"";
			throw new NotFoundException(String.format(msg, table,
					project.getCode()));
		}
		return listener;
	}

	/**
	 * Finds the {@link BaseProject BaseProject} for the specified project code.
	 * It checks if the user can access the project. If the user is an admin,
	 * that is all projects. Otherwise it checks the {@link UserProjectTable
	 * UserProjectTable}. If the project does not exist or the user can't access
	 * it, this method will throw a {@link NotFoundException NotFoundException}.
	 *
	 * @param projectCode the project code
	 * @param authDb the authentication database
	 * @param user the user
	 * @return the project
	 * @throws HttpException if the project does not exist or the user can't
	 * access it
	 * @throws Exception if any other error occurs
	 */
	public static BaseProject findUserProject(String projectCode,
			Database authDb, User user) throws HttpException, Exception {
		List<BaseProject> projects = getUserProjects(authDb, user);
		for (BaseProject project : projects) {
			if (project.getCode().equals(projectCode))
				return project;
		}
		throw new NotFoundException(String.format(
				"Project \"%s\" not found or not accessible", projectCode));
	}

	/**
	 * Finds the {@link BaseProject BaseProject} for the specified project code.
	 * If the project does not exist, this method will throw a {@link
	 * NotFoundException NotFoundException}. If you want to know whether a user
	 * can access a project, use {@link #findUserProject(String, Database, User)
	 * findUserProject()}.
	 *
	 * @param projectCode the project code
	 * @return the project
	 * @throws HttpException if the project does not exist
	 */
	private BaseProject findProject(String projectCode) throws HttpException {
		ProjectRepository projects = AppComponents.get(ProjectRepository.class);
		BaseProject project = projects.findProjectByCode(projectCode);
		if (project != null)
			return project;
		throw new NotFoundException(String.format("Project \"%s\" not found",
				projectCode));
	}
}
