package nl.rrd.senseeact.service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.HttpFieldError;
import nl.rrd.senseeact.client.model.ListUser;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.client.model.compat.*;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.*;
import nl.rrd.senseeact.service.*;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.exception.NotFoundException;
import nl.rrd.senseeact.service.mail.EmailSender;
import nl.rrd.senseeact.service.mail.EmailTemplate;
import nl.rrd.senseeact.service.mail.EmailTemplateCollection;
import nl.rrd.senseeact.service.mail.EmailTemplateRepository;
import nl.rrd.senseeact.service.model.UserTable;
import nl.rrd.senseeact.service.model.*;
import nl.rrd.senseeact.service.validation.ModelValidation;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.beans.PropertyReader;
import nl.rrd.utils.beans.PropertyWriter;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.validation.TypeConversion;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.*;

@RestController
@RequestMapping("/v{version}/user")
public class UserController {
	public static final List<String> CHANGE_FORBIDDEN_FIELDS = List.of(
			"userid",
			"emailVerified",
			"emailPendingVerification",
			"hasTemporaryEmail",
			"hasTemporaryPassword",
			"role",
			"active",
			"created",
			"lastActive"
	);

	@RequestMapping(value="/list", method=RequestMethod.GET)
	public List<ListUser> getUserList(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName) throws HttpException, Exception {
		return QueryRunner.runAuthQuery((version, authDb, user) ->
				doGetUserList(version, user),
				versionName, request, response);
	}

	@RequestMapping(value="/", method=RequestMethod.GET)
	public DatabaseObject getUser(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user", required=false, defaultValue="")
			final String userId,
			@RequestParam(value="email", required=false, defaultValue="")
			final String email) throws HttpException, Exception {
		return QueryRunner.runAuthQuery(
				(version, authDb, user) -> doGetUser(version, authDb, user,
						userId, email),
				versionName, request, response);
	}
	
	@RequestMapping(value="/", method=RequestMethod.PUT)
	@RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public DatabaseObject setUser(
			final HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user", required=false, defaultValue="")
			final String userId,
			@Parameter(hidden = true)
			@RequestParam(value="email", required=false, defaultValue="")
			final String compatEmail,
			@RequestParam(value="emailTemplate", required=false, defaultValue="")
			String emailTemplate) throws HttpException, Exception {
		return QueryRunner.runAuthQuery(
				(version, authDb, user) -> doSetUser(version, authDb, user,
						userId, compatEmail, emailTemplate, request),
				versionName, request, response);
	}
	
	@RequestMapping(value="/", method=RequestMethod.DELETE)
	public void deleteUser(
			final HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user", required=false, defaultValue="")
			final String userId,
			@Parameter(hidden = true)
			@RequestParam(value="email", required=false, defaultValue="")
			final String compatEmail) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, user) ->
				doDeleteUser(version, authDb, user, userId, compatEmail),
				versionName, request, response);
	}
	
	@RequestMapping(value="/role", method=RequestMethod.PUT)
	public void setRole(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user")
			final String user,
			@RequestParam(value="role")
			final String role) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, currUser) ->
				doSetRole(version, authDb, currUser, user, role),
				versionName, request, response);
	}
	
	@RequestMapping(value="/active", method=RequestMethod.PUT)
	public void setActive(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user")
			final String user,
			@RequestParam(value="active")
			final String active) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, currUser) ->
				doSetActive(version, authDb, currUser, user, active),
				versionName, request, response);
	}

	@RequestMapping(value="/groups", method=RequestMethod.GET)
	public List<String> getGroups(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user", required=false, defaultValue="")
			final String user) throws HttpException, Exception {
		return QueryRunner.runAuthQuery(
				(version, authDb, currUser) ->
				doGetGroups(version, authDb, currUser, user),
				versionName, request, response);
	}

	private Object doSetRole(ProtocolVersion version, Database authDb,
			User currUser, String setUserId, String role)
			throws HttpException, Exception {
		if (currUser.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		UserCache userCache = UserCache.getInstance();
		User setUser = userCache.find(version, setUserId);
		if (setUser == null) {
			throw new NotFoundException(String.format("User %s not found",
					setUserId));
		}
		if (setUser.getUserid().equals(currUser.getUserid()))
			throw new ForbiddenException("Can't set role of yourself");
		Role roleEnum;
		try {
			roleEnum = TypeConversion.getEnum(role, Role.class);
		} catch (ParseException ex) {
			String msg = "Invalid role: " + role;
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
			error.addFieldError(new HttpFieldError("role", msg));
			throw new BadRequestException(error);
		}
		DatabaseCriteria criteria = new DatabaseCriteria.Equal("user",
				setUser.getUserid());
		List<UserProject> userProjects = authDb.select(new UserProjectTable(),
				criteria, 0, null);
		List<String> downgradeProjects = new ArrayList<>();
		for (UserProject userProject : userProjects) {
			if (userProject.getAsRole().ordinal() < roleEnum.ordinal() &&
					!downgradeProjects.contains(userProject.getProjectCode())) {
				downgradeProjects.add(userProject.getProjectCode());
			}
		}
		if (!downgradeProjects.isEmpty()) {
			Collections.sort(downgradeProjects);
			throw new ForbiddenException("Can't downgrade to role " + roleEnum +
					", because the user is a member with a higher role in the following projects: " +
					String.join(", ", downgradeProjects));
		}
		Role oldRole = setUser.getRole();
		if (oldRole.ordinal() < Role.PATIENT.ordinal() &&
				roleEnum == Role.PATIENT) {
			// role downgraded to patient
			List<String> subjects = setUser.findGroupUserids(authDb,
					Group.Type.USER_ACCESS);
			if (!subjects.isEmpty()) {
				throw new ForbiddenException(
						"Can't downgrade role to PATIENT because the user has subjects");
			}
		}
		setUser.setRole(roleEnum);
		userCache.updateUser(authDb, setUser);
		UserListenerRepository.getInstance().notifyUserRoleChanged(setUser,
				oldRole);
		return null;
	}
	
	private Object doSetActive(ProtocolVersion version, Database authDb,
			User currUser, String setUserId, String activeStr)
			throws HttpException, Exception {
		if (currUser.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		UserCache userCache = UserCache.getInstance();
		User setUser = userCache.find(version, setUserId);
		if (setUser == null) {
			throw new NotFoundException(String.format("User %s not found",
					setUserId));
		}
		if (setUser.getUserid().equals(currUser.getUserid()))
			throw new ForbiddenException("Can't set active status for yourself");
		boolean active;
		try {
			active = TypeConversion.getBoolean(activeStr);
		} catch (ParseException ex) {
			throw BadRequestException.withInvalidInput(
					new HttpFieldError("active", ex.getMessage()));
		}
		if (setUser.isActive() == active)
			return null;
		ZonedDateTime now = DateTimeUtils.nowMs();
		setUser.setActive(active);
		userCache.updateUser(authDb, setUser);
		UserActiveChange change = new UserActiveChange(setUser.getUserid(), now,
				active);
		authDb.insert(UserActiveChangeTable.NAME, change);
		UserListenerRepository.getInstance().notifyUserActiveChanged(setUser);
		return null;
	}

	private List<ListUser> doGetUserList(
			ProtocolVersion version, User user) throws HttpException,
			Exception {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		UserCache userCache = UserCache.getInstance();
		List<User> users = userCache.getUsers(null, Comparator.comparing(
				User::getEmail));
		List<ListUser> result = new ArrayList<>();
		for (User getUser : users) {
			result.add(ListUser.fromUser(getUser));
		}
		return result;
	}
	
	private DatabaseObject doGetUser(ProtocolVersion version, Database authDb,
			User user, String getUserId, String email)
			throws HttpException, Exception {
		User getUser;
		if (getUserId != null && !getUserId.isEmpty())
			getUser = User.findAccessibleUser(version, getUserId, authDb, user);
		else
			getUser = User.findAccessibleUserByEmail(email, authDb, user);
		return getCompatUser(version, getUser);
	}

	public static DatabaseObject getCompatUser(ProtocolVersion version,
			User user) {
		if (version.ordinal() >= ProtocolVersion.V6_0_7.ordinal())
			return user;
		else if (version.ordinal() >= ProtocolVersion.V6_0_6.ordinal())
			return UserV10.fromUser(user);
		else if (version.ordinal() >= ProtocolVersion.V6_0_5.ordinal())
			return UserV9.fromUser(user);
		else if (version.ordinal() >= ProtocolVersion.V6_0_4.ordinal())
			return UserV8.fromUser(user);
		else if (version.ordinal() >= ProtocolVersion.V6_0_3.ordinal())
			return UserV7.fromUser(user);
		else if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal())
			return UserV6.fromUser(user);
		else if (version.ordinal() >= ProtocolVersion.V5_1_3.ordinal())
			return UserV5.fromUser(user);
		else if (version.ordinal() >= ProtocolVersion.V5_1_2.ordinal())
			return UserV4.fromUser(user);
		else if (version.ordinal() >= ProtocolVersion.V5_1_1.ordinal())
			return UserV3.fromUser(user);
		else if (version.ordinal() >= ProtocolVersion.V5_0_6.ordinal())
			return UserV2.fromUser(user);
		else if (version.ordinal() >= ProtocolVersion.V5_0_4.ordinal())
			return UserV1.fromUser(user);
		else
			return UserV0.fromUser(user);
	}

	public static List<DatabaseObject> getCompatUserList(
			ProtocolVersion version, List<User> users) {
		List<DatabaseObject> result = new ArrayList<>();
		for (User user : users) {
			result.add(getCompatUser(version, user));
		}
		return result;
	}
	
	private DatabaseObject doSetUser(ProtocolVersion version, Database authDb,
			User user, String setUserId, String compatEmail,
			String emailTemplate, HttpServletRequest request)
			throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return doSetUserLock(version, authDb, user, setUserId, compatEmail,
					emailTemplate, request);
		}
	}

	private DatabaseObject doSetUserLock(ProtocolVersion version,
			Database authDb, User user, String setUserId, String compatEmail,
			String emailTemplateName, HttpServletRequest request)
			throws HttpException, Exception {
		User setUser;
		if (compatEmail != null && !compatEmail.isEmpty())
			setUser = User.findAccessibleUserByEmail(compatEmail, authDb, user);
		else
			setUser = User.findAccessibleUser(version, setUserId, authDb, user);
		Map<String,?> inputMap = readSetUserInput(request);
		validateUserMapKeys(version, inputMap);
		// inputMap contains only fields that occur in client model of User
		// for the specified protocol version
		ParsedSetUserInput parsedInput = parseSetUserInput(version, inputMap);
		nl.rrd.senseeact.client.model.User inputUser = parsedInput.user;
		Set<String> setFields = parsedInput.setFields;
		UserCache userCache = UserCache.getInstance();
		// if inputMap contains fields that cannot be changed, their values
		// have not changed
		User oldProfile = new User();
		oldProfile.copyFrom(setUser);
		for (String field : setFields) {
			if (CHANGE_FORBIDDEN_FIELDS.contains(field) ||
					field.equals("email")) {
				continue;
			}
			PropertyWriter.writeProperty(setUser, field,
					PropertyReader.readProperty(inputUser, field));
		}
		SetUserEmailResult emailResult = setUserEmail(setUser, inputUser,
				emailTemplateName);
		ModelValidation.validate(setUser);
		userCache.updateUser(authDb, setUser);
		if (!setUser.equals(oldProfile)) {
			UserListenerRepository.getInstance().notifyUserProfileUpdated(
					setUser, oldProfile);
		}
		sendEmailChanged(emailResult, request, setUser, oldProfile, authDb);
		return getCompatUser(version, setUser);
	}

	/**
	 * Tries to read a JSON object from the specified HTTP request body for
	 * setUser(). If the body content is invalid, it logs an error message and
	 * throws a BadRequestException.
	 *
	 * @param request the HTTP request
	 * @return the JSON object as a map
	 * @throws BadRequestException if the body content is invalid
	 * @throws IOException if a reading error occurs
	 */
	private Map<String,?> readSetUserInput(HttpServletRequest request)
			throws BadRequestException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		InputStream input = request.getInputStream();
		Exception jsonException;
		try {
			return mapper.readValue(request.getInputStream(),
					new TypeReference<>() {});
		} catch (JsonProcessingException ex) {
			jsonException = ex;
		} finally {
			input.close();
		}
		Logger logger = AppComponents.getLogger(SenSeeActContext.LOGTAG);
		String msg = "Content is not a valid JSON object";
		logger.error("Invalid input in PUT /user/: " + msg + ": " +
				jsonException.getMessage());
		HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
		throw new BadRequestException(error);
	}

	/**
	 * Checks whether the keys in the specified user map occur in the client
	 * model fields of User for the specified protocol version. The user map
	 * should be the input from setUser(). If any invalid key is found, it
	 * throws a BadRequestException.
	 *
	 * @param version the protocol version
	 * @param userMap the user map
	 * @throws BadRequestException if an invalid key is found
	 */
	private void validateUserMapKeys(ProtocolVersion version,
			Map<String,?> userMap) throws BadRequestException {
		List<String> userFields = getUserModelFields(version);
		List<String> invalidKeys = new ArrayList<>();
		for (String key : userMap.keySet()) {
			if (!userFields.contains(key))
				invalidKeys.add(key);
		}
		if (!invalidKeys.isEmpty()) {
			StringBuilder invalidKeysStr = new StringBuilder();
			for (String key : invalidKeys) {
				if (!invalidKeysStr.isEmpty())
					invalidKeysStr.append(", ");
				invalidKeysStr.append(key);
			}
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
					"Invalid user fields: " + invalidKeysStr);
			throw new BadRequestException(error);
		}
	}

	/**
	 * Returns all fields in the client model of User for the specified protocol
	 * version.
	 *
	 * @param version the version
	 * @return the user model fields
	 */
	private List<String> getUserModelFields(ProtocolVersion version) {
		if (version.ordinal() >= ProtocolVersion.V6_0_7.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(
					nl.rrd.senseeact.client.model.User.class);
		} else if (version.ordinal() >= ProtocolVersion.V6_0_6.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV10.class);
		} else if (version.ordinal() >= ProtocolVersion.V6_0_5.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV9.class);
		} else if (version.ordinal() >= ProtocolVersion.V6_0_4.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV8.class);
		} else if (version.ordinal() >= ProtocolVersion.V6_0_3.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV7.class);
		} else if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV6.class);
		} else if (version.ordinal() >= ProtocolVersion.V5_1_3.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV5.class);
		} else if (version.ordinal() >= ProtocolVersion.V5_1_2.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV4.class);
		} else if (version.ordinal() >= ProtocolVersion.V5_1_1.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV3.class);
		} else if (version.ordinal() >= ProtocolVersion.V5_0_6.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV2.class);
		} else if (version.ordinal() >= ProtocolVersion.V5_0_4.ordinal()) {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV1.class);
		} else {
			return DatabaseFieldScanner.getDatabaseFieldNames(UserV0.class);
		}
	}

	/**
	 * Parses the values of the specified user map, which should be the input
	 * from setUser(). The keys in the map should already have been validated
	 * with validateUserMapKeys(). This method converts the map to the client
	 * model of User for the specified protocol version. Then it converts that
	 * model to the current version. It also converts the set of keys in the
	 * user map to the corresponding names in the latest version. If any value
	 * is invalid, this method throws a BadRequestException.
	 *
	 * @param version the version
	 * @param userMap the user map
	 * @return the user model and list of set fields in the latest version
	 * @throws BadRequestException if any value in the map is invalid
	 */
	private ParsedSetUserInput parseSetUserInput(ProtocolVersion version,
			Map<String,?> userMap) throws BadRequestException {
		ParsedSetUserInput result = new ParsedSetUserInput();
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		ObjectMapper mapper = new ObjectMapper();
		try {
			if (version.ordinal() >= ProtocolVersion.V6_0_7.ordinal()) {
				result.user = mapper.convertValue(userMap,
						nl.rrd.senseeact.client.model.User.class);
				result.setFields = userMap.keySet();
			} else if (version.ordinal() >= ProtocolVersion.V6_0_6.ordinal()) {
				UserV10 userCompat = mapper.convertValue(userMap,
						UserV10.class);
				result.user = userCompat.toUser();
				result.setFields = userMap.keySet();
			} else if (version.ordinal() >= ProtocolVersion.V6_0_5.ordinal()) {
				UserV9 userCompat = mapper.convertValue(userMap, UserV9.class);
				result.user = userCompat.toUser();
				result.setFields = userMap.keySet();
			} else if (version.ordinal() >= ProtocolVersion.V6_0_4.ordinal()) {
				UserV8 userCompat = mapper.convertValue(userMap, UserV8.class);
				result.user = userCompat.toUser();
				result.setFields = userMap.keySet();
			} else if (version.ordinal() >= ProtocolVersion.V6_0_3.ordinal()) {
				UserV7 userCompat = mapper.convertValue(userMap, UserV7.class);
				result.user = userCompat.toUser();
				result.setFields = userCompat.convertFields(userMap.keySet());
			} else if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal()) {
				UserV6 userCompat = mapper.convertValue(userMap, UserV6.class);
				result.user = userCompat.toUser();
				result.setFields = userCompat.convertFields(userMap.keySet());
			} else if (version.ordinal() >= ProtocolVersion.V5_1_3.ordinal()) {
				UserV5 userCompat = mapper.convertValue(userMap, UserV5.class);
				result.user = userCompat.toUser();
				result.setFields = userCompat.convertFields(userMap.keySet());
			} else if (version.ordinal() >= ProtocolVersion.V5_1_2.ordinal()) {
				UserV4 userCompat = mapper.convertValue(userMap, UserV4.class);
				result.user = userCompat.toUser();
				result.setFields = userCompat.convertFields(userMap.keySet());
			} else if (version.ordinal() >= ProtocolVersion.V5_1_1.ordinal()) {
				UserV3 userCompat = mapper.convertValue(userMap, UserV3.class);
				result.user = userCompat.toUser();
				result.setFields = userCompat.convertFields(userMap.keySet());
			} else if (version.ordinal() >= ProtocolVersion.V5_0_6.ordinal()) {
				UserV2 userCompat = mapper.convertValue(userMap, UserV2.class);
				result.user = userCompat.toUser();
				result.setFields = userCompat.convertFields(userMap.keySet());
			} else if (version.ordinal() >= ProtocolVersion.V5_0_4.ordinal()) {
				UserV1 userCompat = mapper.convertValue(userMap, UserV1.class);
				result.user = userCompat.toUser();
				result.setFields = userCompat.convertFields(userMap.keySet());
			} else {
				UserV0 userCompat = mapper.convertValue(userMap, UserV0.class);
				result.user = userCompat.toUser();
				result.setFields = userCompat.convertFields(userMap.keySet());
			}
			return result;
		} catch (IllegalArgumentException ex) {
			String msg = "Content is invalid for a user object";
			logger.error("Invalid input in PUT /user/: " + msg + ": " +
					ex.getMessage());
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
			throw new BadRequestException(error);
		}
	}

	private static class ParsedSetUserInput {
		public nl.rrd.senseeact.client.model.User user;
		public Set<String> setFields;
	}

	private SetUserEmailResult setUserEmail(User setUser,
			nl.rrd.senseeact.client.model.User inputUser, String templateName)
			throws HttpException {
		SetUserEmailResult result = new SetUserEmailResult();
		String newEmail = inputUser.getEmail();
		if (newEmail == null)
			return result;
		newEmail = newEmail.toLowerCase();
		EmailChangedState state = setUserEmailGetState(newEmail, setUser);
		if (state == EmailChangedState.NOT_CHANGED)
			return result;
		UserCache userCache = UserCache.getInstance();
		if (userCache.emailExists(newEmail)) {
			String message = "User with email " + newEmail + " already exists";
			HttpError error = new HttpError(ErrorCode.USER_ALREADY_EXISTS,
					message);
			error.addFieldError(new HttpFieldError("email", message));
			throw new ForbiddenException(error);
		}
		result.state = state;
		result.newEmail = newEmail;
		result.templates = getEmailChangedTemplates(templateName);
		return result;
	}

	private EmailChangedState setUserEmailGetState(String newEmail,
			User setUser) {
		if (setUser.getEmailPendingVerification() != null) {
			if (newEmail.equals(setUser.getEmailPendingVerification()))
				return EmailChangedState.NOT_CHANGED;
			if (newEmail.equals(setUser.getEmail())) {
				setUser.setEmailPendingVerification(null);
				setUser.setVerifyEmailRequestCode(null);
				setUser.setVerifyEmailRequestTime(null);
				return EmailChangedState.NOT_CHANGED;
			}
			setUser.setEmailPendingVerification(newEmail);
			return EmailChangedState.PENDING_VERIFICATION_CHANGED;
		} else {
			if (newEmail.equals(setUser.getEmail()))
				return EmailChangedState.NOT_CHANGED;
			if (setUser.isHasTemporaryEmail()) {
				setUser.setEmail(newEmail);
				setUser.setHasTemporaryEmail(false);
				return EmailChangedState.TEMPORARY_CHANGED;
			} else if (setUser.isEmailVerified()) {
				setUser.setEmailPendingVerification(newEmail);
				return EmailChangedState.VERIFIED_CHANGED;
			} else {
				setUser.setEmail(newEmail);
				return EmailChangedState.UNVERIFIED_CHANGED;
			}
		}
	}

	private static class SetUserEmailResult {
		public EmailChangedState state = EmailChangedState.NOT_CHANGED;
		public String newEmail = null;
		public EmailChangedTemplates templates = null;
	}

	private enum EmailChangedState {
		NOT_CHANGED,
		TEMPORARY_CHANGED,
		VERIFIED_CHANGED,
		UNVERIFIED_CHANGED,
		PENDING_VERIFICATION_CHANGED
	}

	private EmailChangedTemplates getEmailChangedTemplates(
			String templateName) throws BadRequestException {
		EmailChangedTemplates result = new EmailChangedTemplates();
		List<HttpFieldError> errors = new ArrayList<>();
		EmailTemplateRepository repo = AppComponents.get(
				EmailTemplateRepository.class);
		result.newUser = findEmailTemplate(repo.getNewUserTemplates(),
				templateName, errors);
		result.emailChangedOldVerified = findEmailTemplate(
				repo.getEmailChangedOldVerifiedTemplates(), templateName,
				errors);
		result.emailChangedOldUnverified = findEmailTemplate(
				repo.getEmailChangedOldUnverifiedTemplates(), templateName,
				errors);
		result.emailChangedNewVerified = findEmailTemplate(
				repo.getEmailChangedNewVerifiedTemplates(), templateName,
				errors);
		result.emailChangedNewUnverified = findEmailTemplate(
				repo.getEmailChangedNewUnverifiedTemplates(), templateName,
				errors);
		if (!errors.isEmpty())
			throw BadRequestException.withInvalidInput(errors.get(0));
		return result;
	}

	private static class EmailChangedTemplates {
		public EmailTemplate newUser;
		public EmailTemplate emailChangedOldVerified;
		public EmailTemplate emailChangedOldUnverified;
		public EmailTemplate emailChangedNewVerified;
		public EmailTemplate emailChangedNewUnverified;
	}

	private void sendEmailChanged(SetUserEmailResult setResult,
			HttpServletRequest request, User user, User oldProfile,
			Database authDb) throws HttpException, Exception {
		EmailChangedState state = setResult.state;
		if (state == EmailChangedState.NOT_CHANGED)
			return;
		if (state == EmailChangedState.TEMPORARY_CHANGED) {
			AuthControllerExecution.sendNewUserMail(request, user,
					setResult.templates.newUser, authDb);
			return;
		}
		UserCache userCache = UserCache.getInstance();
		ZonedDateTime now = DateTimeUtils.nowMs();
		String code = AuthControllerExecution.createEmailVerificationCode(user,
				now);
		user.setVerifyEmailRequestCode(code);
		user.setVerifyEmailRequestTime(now);
		userCache.updateUser(authDb, user);
		String oldTo;
		EmailTemplate oldTemplate;
		Map<String,Object> oldParams = new LinkedHashMap<>();
		String newTo;
		EmailTemplate newTemplate;
		Map<String,Object> newParams = new LinkedHashMap<>();
		newParams.put("code", code);
		if (state == EmailChangedState.VERIFIED_CHANGED ||
				state == EmailChangedState.PENDING_VERIFICATION_CHANGED) {
			oldTo = user.getEmail();
			oldTemplate = setResult.templates.emailChangedOldVerified;
			oldParams.put("new_email", user.getEmailPendingVerification());
			newTo = user.getEmailPendingVerification();
			newTemplate = setResult.templates.emailChangedNewVerified;
			newParams.put("old_email", user.getEmail());
			newParams.put("new_email", user.getEmailPendingVerification());
		} else {
			// state == EmailChangedState.UNVERIFIED_CHANGED
			oldTemplate = setResult.templates.emailChangedOldUnverified;
			oldTo = oldProfile.getEmail();
			newTo = user.getEmail();
			newTemplate = setResult.templates.emailChangedNewUnverified;
			newParams.put("old_email", oldProfile.getEmail());
			newParams.put("new_email", user.getEmail());
		}
		getEmailSender().trySendThread(oldTemplate, request, user, oldTo, oldParams);
		getEmailSender().trySendThread(newTemplate, request, user, newTo, newParams);
	}

	private EmailSender getEmailSender() {
		Configuration config = AppComponents.get(Configuration.class);
		return new EmailSender(config.toEmailConfig());
	}

	private EmailTemplate findEmailTemplate(
			EmailTemplateCollection templates, String template,
			List<HttpFieldError> errors) {
		if (template != null && !template.isEmpty()) {
			try {
				return templates.find(template);
			} catch (IllegalArgumentException ex) {
				errors.add(new HttpFieldError("emailTemplate",
						"Invalid value for parameter \"emailTemplate\""));
			}
		}
		return templates.getDefault();
	}

	private Object doDeleteUser(ProtocolVersion version, Database authDb,
			User user, String userId, String compatEmail) throws HttpException,
			DatabaseException, IOException {
		UserCache userCache = UserCache.getInstance();
		boolean hasUserId = userId != null && !userId.isEmpty();
		boolean hasEmail = compatEmail != null && !compatEmail.isEmpty();
		User delUser;
		if (hasUserId && user.getRole() == Role.ADMIN) {
			delUser = userCache.findByUserid(userId);
		} else if (hasUserId) {
			delUser = User.findAccessibleUser(version, userId, authDb, user);
		} else if (hasEmail && user.getRole() == Role.ADMIN) {
			delUser = userCache.findByEmail(compatEmail);
		} else if (hasEmail) {
			delUser = User.findAccessibleUserByEmail(compatEmail, authDb, user);
		} else {
			delUser = user;
		}
		if (delUser == null) {
			// specified user not found as admin: do nothing
			return null;
		}
		if (user.getRole() != Role.ADMIN && !delUser.getUserid().equals(
				user.getUserid())) {
			throw new ForbiddenException("You cannot delete other users");
		}
		deleteUser(authDb, delUser);
		return null;
	}

	public static void deleteUser(Database authDb, User user)
			throws HttpException, DatabaseException, IOException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal(
				"user", user.getUserid());
		List<UserProject> oldUserProjects = authDb.select(
				new UserProjectTable(), criteria, 0, null);
		authDb.delete(new UserProjectTable(), criteria);
		for (UserProject oldUserProject : oldUserProjects) {
			UserListenerRepository.getInstance().notifyUserRemovedFromProject(
					user, oldUserProject.getProjectCode(),
					oldUserProject.getAsRole());
		}
		List<Group> groups = Group.findGroupsForUser(authDb, user.getUserid(),
				null);
		for (Group group : groups) {
			if (group.getGroupType() == Group.Type.USER_ACCESS) {
				GroupController.deleteGroup(authDb, group.getName());
			} else {
				GroupController.deleteMember(authDb, group.getName(), user);
			}
		}
		criteria = new DatabaseCriteria.Or(
				new DatabaseCriteria.Equal("grantee", user.getUserid()),
				new DatabaseCriteria.Equal("subject", user.getUserid())
		);
		authDb.delete(new ProjectUserAccessTable(), criteria);
		criteria = new DatabaseCriteria.Equal("user", user.getUserid());
		List<String> excludeNames = Arrays.asList(
				UserTable.NAME,
				GroupTable.NAME,
				GroupMemberTable.NAME,
				UserProjectTable.NAME
		);
		List<DatabaseTableDef<?>> authTables = DatabaseLoader.getAuthDbTables();
		authTables.removeIf(table -> {
			if (excludeNames.contains(table.getName()))
				return true;
			List<String> fields = DatabaseFieldScanner.getDatabaseFieldNames(
					table.getDataClass());
			return !fields.contains("user");
		});
		for (DatabaseTableDef<?> table : authTables) {
			authDb.delete(table, criteria);
		}
		authDb.delete(new UserTableKeyTable(), criteria);
		DatabaseLoader dbLoader = DatabaseLoader.getInstance();
		DatabaseConnection dbConn = dbLoader.openConnection();
		ProjectRepository projects = AppComponents.get(ProjectRepository.class);
		try {
			for (BaseProject project : projects.getProjects()) {
				Database db = dbLoader.initProjectDatabase(dbConn,
						project.getCode());
				if (db != null)
					db.purgeUser(user.getUserid());
			}
		} finally {
			dbConn.close();
		}
		UserCache userCache = UserCache.getInstance();
		userCache.deleteUser(authDb, user.getUserid());
	}
	
	private List<String> doGetGroups(ProtocolVersion version, Database authDb,
			User currUser, String getUserId) throws DatabaseException,
			HttpException {
		User getUser = User.findAccessibleUser(version, getUserId, authDb,
				currUser);
		if (currUser.getRole() != Role.ADMIN &&
				!getUser.getUserid().equals(currUser.getUserid())) {
			throw new ForbiddenException(
					"Getting groups of another user not allowed");
		}
		List<Group> groups = Group.findGroupsForUser(authDb,
				getUser.getUserid(), Group.Type.MULTI_USER);
		List<String> groupNames = new ArrayList<>();
		for (Group group : groups) {
			groupNames.add(group.getName());
		}
		return groupNames;
	}
}
