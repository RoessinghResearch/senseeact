package nl.rrd.senseeact.service.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.HttpFieldError;
import nl.rrd.senseeact.client.model.*;
import nl.rrd.senseeact.client.model.compat.LoginUsernameResultV0;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.*;
import nl.rrd.senseeact.service.controller.model.ChangePasswordParams;
import nl.rrd.senseeact.service.controller.model.ResetPasswordParams;
import nl.rrd.senseeact.service.exception.*;
import nl.rrd.senseeact.service.mail.EmailSender;
import nl.rrd.senseeact.service.mail.EmailTemplate;
import nl.rrd.senseeact.service.mail.EmailTemplateCollection;
import nl.rrd.senseeact.service.mail.EmailTemplateRepository;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.*;
import nl.rrd.senseeact.service.validation.ModelValidation;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpClient2;
import nl.rrd.utils.http.HttpClientException;
import nl.rrd.utils.http.URLParameters;
import nl.rrd.utils.io.FileUtils;
import nl.rrd.utils.json.JsonMapper;
import nl.rrd.utils.validation.MapReader;
import nl.rrd.utils.validation.Validation;
import nl.rrd.utils.validation.ValidationException;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.*;

@RestController
@RequestMapping("/v{version}/auth")
public class AuthControllerExecution {
	public static final Object AUTH_LOCK = new Object();
	private static final int MAX_FAILED_LOGINS = 10;
	private static final int ACCOUNT_BLOCK_DURATION = 60; // seconds
	private static final int EMAIL_CODE_VALID_DURATION = 24; // hours

	private static final int MAX_MFA_SMS_COUNT = 2;
	private static final int MAX_MFA_TOTP_COUNT = 1;
	private static final int MAX_MFA_ADD_VERIFY = 5;
	private static final int MAX_MFA_ADD_COUNT = 10;
	private static final int MAX_MFA_ADD_MINUTES = 60;
	private static final int MAX_MFA_VERIFY_COUNT = 10;
	private static final int MAX_MFA_VERIFY_MINUTES = 15;

	/**
	 * Runs the signup query.
	 *
	 * @param version the protocol version
	 * @param email the email address
	 * @param password the password
	 * @param project the project
	 * @param authDb the authentication database
	 * @return the JWT token
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Object signup(ProtocolVersion version, HttpServletRequest request,
			HttpServletResponse response, String email, String password,
			String project, boolean cookie, boolean autoExtendCookie,
			String emailTemplate, SignupParams signupParams, Database authDb)
			throws HttpException, Exception {
		return signup(version, request, response, email, password, project,
				cookie, autoExtendCookie, emailTemplate, signupParams, authDb,
				false, true);
	}

	private Object signup(ProtocolVersion version, HttpServletRequest request,
			HttpServletResponse response, String email, String password,
			String project, boolean cookie, boolean autoExtendCookie,
			String emailTemplate, SignupParams signupParams, Database authDb,
			boolean isTempUser, boolean sendEmail) throws HttpException,
			Exception {
		validateForbiddenQueryParams(request, "email", "password");
		Integer expireMinutes = LoginParams.DEFAULT_EXPIRATION;
		if (signupParams != null) {
			email = signupParams.getEmail();
			password = signupParams.getPassword();
			project = signupParams.getProject();
			if (signupParams.getTokenExpiration() == null ||
					signupParams.getTokenExpiration() != 0) {
				expireMinutes = signupParams.getTokenExpiration();
			}
			cookie = signupParams.isCookie();
			autoExtendCookie = signupParams.isAutoExtendCookie();
			emailTemplate = signupParams.getEmailTemplate();
		}
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		if (email == null || email.isEmpty()) {
			fieldErrors.add(new HttpFieldError("email",
					"Parameter \"email\" not defined"));
		}
		if (password == null || password.isEmpty()) {
			fieldErrors.add(new HttpFieldError("password",
					"Parameter \"password\" not defined"));
		}
		if (!fieldErrors.isEmpty())
			throw BadRequestException.withInvalidInput(fieldErrors);
		TokenResult token = signup(version, request, response, email, password,
				project, expireMinutes, cookie, autoExtendCookie, emailTemplate,
				authDb, isTempUser, sendEmail);
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal())
			return token;
		else
			return token.getToken();
	}

	public static TokenResult signupSSO(ProtocolVersion version,
			HttpServletResponse response, String email, String password,
			String project, Database authDb) throws HttpException, Exception {
		return signup(version, null, response, email, password, project,
				null, false, false, null, authDb, false, false);
	}

	/**
	 * Creates a new user.
	 *
	 * @param version the protocol version
	 * @param request the request (only used to detect the language if an email
	 * needs to be sent)
	 * @param response the response
	 * @param email the email address
	 * @param password the password
	 * @param projectCode the project code or null
	 * @param expireMinutes the number of minutes after which the token should
	 * expire
	 * @param cookie true if a HttpOnly cookie should be set
	 * @param autoExtendCookie true if the cookie should be extended with
	 * expireMinutes at every request
	 * @param emailTemplateName the email template name or null (only used if
	 * an email needs to be sent)
	 * @param authDb the authentication database
	 * @param isTempUser if the new user is a temporary user
	 * @param sendEmail if an email with email confirmation link should be sent
	 * @return the token result
	 * @throws HttpException if an HTTP error response should be generated
	 * @throws Exception if an unexpected error occurs
	 */
	private static TokenResult signup(ProtocolVersion version,
			HttpServletRequest request, HttpServletResponse response,
			String email, String password, String projectCode,
			Integer expireMinutes, boolean cookie, boolean autoExtendCookie,
			String emailTemplateName, Database authDb, boolean isTempUser,
			boolean sendEmail) throws HttpException, Exception {
		email = email.toLowerCase();
		UserCache userCache = UserCache.getInstance();
		if (userCache.emailExists(email)) {
			String message = "User with email " + email + " already exists";
			HttpError error = new HttpError(ErrorCode.USER_ALREADY_EXISTS,
					message);
			error.addFieldError(new HttpFieldError("email", message));
			throw new ForbiddenException(error);
		}
		User user = new User();
		user.setUserid(UUID.randomUUID().toString().toLowerCase()
				.replaceAll("-", ""));
		user.setEmail(email);
		user.setHasTemporaryEmail(isTempUser);
		ZonedDateTime now = DateTimeUtils.nowMs();
		user.setCreated(now);
		user.setLastActive(now);
		user.setRole(Role.PATIENT);
		BadRequestException badReqEx = null;
		try {
			ModelValidation.validate(user);
		} catch (BadRequestException ex) {
			badReqEx = ex;
		}
		try {
			setPassword(user, password, "password", isTempUser);
		} catch (BadRequestException ex) {
			if (badReqEx == null)
				badReqEx = ex;
			else
				badReqEx = badReqEx.appendInvalidInput(ex);
		}
		BaseProject project = null;
		try {
			project = validateProject(version, projectCode);
		} catch (BadRequestException ex) {
			if (badReqEx == null)
				badReqEx = ex;
			else
				badReqEx = badReqEx.appendInvalidInput(ex);
		}
		if (badReqEx != null)
			throw badReqEx;
		EmailTemplate emailTemplate = null;
		if (sendEmail) {
			List<HttpFieldError> errors = new ArrayList<>();
			EmailTemplateRepository templateRepo = AppComponents.get(
					EmailTemplateRepository.class);
			emailTemplate = findEmailTemplate(
					templateRepo.getNewUserTemplates(),
					emailTemplateName, "emailTemplate", errors);
			if (!errors.isEmpty())
				throw BadRequestException.withInvalidInput(errors);
		}
		if (project != null) {
			try {
				project.validateAddUser(user, user, authDb);
			} catch (ValidationException ex) {
				throw new ForbiddenException(ex.getMessage());
			}
		}
		userCache.createUser(authDb, user);
		if (project != null) {
			UserProject userProject = new UserProject();
			userProject.setUser(user.getUserid());
			userProject.setProjectCode(project.getCode());
			userProject.setAsRole(Role.PATIENT);
			authDb.insert(UserProjectTable.NAME, userProject);
			UserListenerRepository.getInstance().notifyUserAddedToProject(user,
					project.getCode(), Role.PATIENT);
		}
		if (sendEmail)
			sendNewUserMail(request, user, emailTemplate, authDb);
		Logger logger = AppComponents.getLogger(
				AuthControllerExecution.class.getSimpleName());
		logger.info("User signed up: userid: {}, email: {}", user.getUserid(),
				user.getEmail());
		String token = AuthToken.createToken(version, user, false, null, now,
				expireMinutes, cookie, autoExtendCookie, response);
		return new TokenResult(user.getUserid(), token);
	}

	public Object signupTemporaryUser(ProtocolVersion version,
			HttpServletRequest request, HttpServletResponse response,
			String projectCode, boolean cookie, boolean autoExtendCookie,
			SignupTemporaryUserParams signupParams, Database authDb)
			throws HttpException, Exception {
		validateForbiddenQueryParams(request, "email", "password");
		Integer expiration = LoginParams.DEFAULT_EXPIRATION;
		if (signupParams != null) {
			projectCode = signupParams.getProject();
			if (signupParams.getTokenExpiration() == null ||
					signupParams.getTokenExpiration() != 0) {
				expiration = signupParams.getTokenExpiration();
			}
			cookie = signupParams.isCookie();
			autoExtendCookie = signupParams.isAutoExtendCookie();
		}
		BaseProject project = validateProject(version, projectCode);
		SignupParams delegateParams = new SignupParams();
		String email = UUID.randomUUID().toString().toLowerCase()
				.replaceAll("-", "") + "@temp.senseeact.com";
		delegateParams.setEmail(email);
		String password = UUID.randomUUID().toString().toLowerCase()
				.replaceAll("-", "");
		delegateParams.setPassword(password);
		delegateParams.setProject(project.getCode());
		delegateParams.setTokenExpiration(expiration);
		delegateParams.setCookie(cookie);
		delegateParams.setAutoExtendCookie(autoExtendCookie);
		return signup(version, request, response, null, null, null, false,
				false, null, delegateParams, authDb, true, false);
	}

	private static BaseProject validateProject(ProtocolVersion version,
			String projectCode) throws BadRequestException {
		ProjectRepository projects = AppComponents.get(ProjectRepository.class);
		if (projectCode == null || projectCode.isEmpty()) {
			if (version.ordinal() >= ProtocolVersion.V5_0_7.ordinal())
				return null;
			else
				return projects.findProjectByCode("default");
		}
		BaseProject project = projects.findProjectByCode(projectCode);
		if (project != null) {
			return project;
		} else {
			HttpFieldError error = new HttpFieldError("project",
					"Project not found: " + projectCode);
			throw BadRequestException.withInvalidInput(error);
		}
	}

	public Object requestVerifyEmail(HttpServletRequest request, User user,
			String templateName, Database authDb) throws HttpException,
			Exception {
		if (user.isHasTemporaryEmail())
			return null;
		List<HttpFieldError> errors = new ArrayList<>();
		EmailTemplateRepository repo = AppComponents.get(
				EmailTemplateRepository.class);
		EmailTemplate template = findEmailTemplate(repo.getNewUserTemplates(),
				templateName, "template", errors);
		if (!errors.isEmpty())
			throw BadRequestException.withInvalidInput(errors);
		sendNewUserMail(request, user, template, authDb);
		return null;
	}

	public Object verifyEmail(HttpServletRequest request, String userId,
			String code, Database authDb) throws HttpException, Exception {
		ZonedDateTime now = DateTimeUtils.nowMs();
		UserCache userCache = UserCache.getInstance();
		User user = userCache.findByUserid(userId);
		if (user == null) {
			throw new NotFoundException(String.format(
					"User \"%s\" not found", userId));
		}
		HttpFieldError verifyCodeError = new HttpFieldError("code",
				"The confirmation code is invalid or no longer valid");
		if (user.getVerifyEmailRequestCode() == null)
			throw BadRequestException.withInvalidInput(verifyCodeError);
		ZonedDateTime validUntil = user.getVerifyEmailRequestTime().plusHours(
				EMAIL_CODE_VALID_DURATION);
		if (!user.getVerifyEmailRequestCode().equals(code) ||
				now.isAfter(validUntil)) {
			user.setVerifyEmailRequestCode(null);
			user.setVerifyEmailRequestTime(null);
			userCache.updateUser(authDb, user);
			throw BadRequestException.withInvalidInput(verifyCodeError);
		}
		user.setEmailVerified(true);
		if (user.getEmailPendingVerification() != null) {
			user.setEmail(user.getEmailPendingVerification());
			user.setEmailPendingVerification(null);
		}
		user.setVerifyEmailRequestCode(null);
		user.setVerifyEmailRequestTime(null);
		userCache.updateUser(authDb, user);
		return null;
	}

	public Object login(ProtocolVersion version, HttpServletRequest request,
			HttpServletResponse response, String email, String password,
			boolean cookie, boolean autoExtendCookie, LoginParams loginParams,
			Database authDb) throws HttpException, Exception {
		Integer expiration = LoginParams.DEFAULT_EXPIRATION;
		if (loginParams != null) {
			email = loginParams.getEmail();
			password = loginParams.getPassword();
			if (loginParams.getTokenExpiration() == null ||
					loginParams.getTokenExpiration() != 0) {
				expiration = loginParams.getTokenExpiration();
			}
			cookie = loginParams.isCookie();
			autoExtendCookie = loginParams.isAutoExtendCookie();
		}
		LoginResult loginResult = loginByEmail(version, request, response,
				email, password, expiration, cookie, autoExtendCookie, authDb);
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		if (version.ordinal() >= ProtocolVersion.V6_1_0.ordinal()) {
			return loginResult;
		}
		if (loginResult.getMfaRecord() != null) {
			logger.info("Failed login attempt for user {}: protocol version {} does not support MFA",
					loginResult.getEmail(), version.versionName());
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal()) {
			return new TokenResult(loginResult.getUser(),
					loginResult.getToken());
		} else {
			return loginResult.getToken();
		}
	}

	private LoginResult loginByEmail(ProtocolVersion version,
			HttpServletRequest request, HttpServletResponse response,
			String email, String password, Integer expireMinutes,
			boolean cookie, boolean autoExtendCookie, Database authDb)
			throws HttpException, Exception {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		validateForbiddenQueryParams(request, "email", "password");
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		if (email == null || email.isEmpty()) {
			fieldErrors.add(new HttpFieldError("email",
					"Parameter \"email\" not defined"));
		}
		if (password == null || password.isEmpty()) {
			fieldErrors.add(new HttpFieldError("password",
					"Parameter \"password\" not defined"));
		}
		if (!fieldErrors.isEmpty()) {
			logger.info("Failed login attempt: " + fieldErrors);
			throw BadRequestException.withInvalidInput(fieldErrors);
		}
		email = email.toLowerCase();
		UserCache userCache = UserCache.getInstance();
		User user = userCache.findByEmail(email);
		String invalidError = "Email or password is invalid";
		if (user == null) {
			logger.info("Failed login attempt for user {}: user unknown",
					email);
			throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS,
					invalidError);
		}
		cleanMfaRecords(authDb, user);
		ZonedDateTime now = DateTimeUtils.nowMs();
		ZonedDateTime blocked = user.getAccountBlockedUntil();
		if (blocked != null && !now.isAfter(blocked)) {
			logger.info("Failed login attempt for user {}: account temporarily blocked",
					user.getEmail());
			throw new UnauthorizedException(ErrorCode.ACCOUNT_BLOCKED,
					"Account temporarily blocked because of too many failed login attempts");
		}
		byte[] salt = Base64.getDecoder().decode(user.getSalt());
		String hash = hashPassword(password, salt);
		String hashDeprecated = hashPasswordDeprecated(password, salt);
		if (!hash.equals(user.getPassword()) && hashDeprecated.equals(
				user.getPassword())) {
			user.setPassword(hash);
			userCache.updateUser(authDb, user);
		} else if (!hash.equals(user.getPassword())) {
			int failedLogins = user.getFailedLogins();
			if (failedLogins < MAX_FAILED_LOGINS) {
				failedLogins++;
				user.setFailedLogins(failedLogins);
			}
			if (failedLogins >= MAX_FAILED_LOGINS) {
				user.setAccountBlockedUntil(now.plusSeconds(
						ACCOUNT_BLOCK_DURATION));
			}
			userCache.updateUser(authDb, user);
			logger.info("Failed login attempt for user {}: invalid credentials",
					user.getEmail());
			throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS,
					invalidError);
		}
		if (user.getFailedLogins() != 0) {
			user.setFailedLogins(0);
			user.setAccountBlockedUntil(null);
			userCache.updateUser(authDb, user);
		}
		if (!user.isActive()) {
			logger.info("Failed login attempt for user {}: account inactive",
					user.getEmail());
			throw new UnauthorizedException(ErrorCode.ACCOUNT_INACTIVE,
					"Account has been deactivated");
		}
		PrivateMfaRecord mfaRecord = user.findDefaultMfaRecord();
		logger.info("User logged in: userid: {}, email: {}, pendingMfa: {}",
				user.getUserid(), user.getEmail(), mfaRecord != null);
		String token = AuthToken.createToken(version, user, mfaRecord != null,
				null, now, expireMinutes, cookie, autoExtendCookie, response);
		LoginResult result = new LoginResult();
		result.setStatus(mfaRecord == null ? LoginResult.Status.COMPLETE :
				LoginResult.Status.REQUIRES_MFA);
		result.setUser(user.getUserid());
		result.setEmail(email);
		result.setToken(token);
		result.setMfaRecord(mfaRecord == null ? null :
				mfaRecord.toPublicMfaRecord());
		if (mfaRecord != null)
			requestMfaVerification(authDb, user, mfaRecord);
		return result;
	}

	public static void validateForbiddenQueryParams(HttpServletRequest request,
			String... paramNames) throws BadRequestException {
		if (request.getQueryString() == null)
			return;
		Map<String,String> params;
		try {
			params = URLParameters.parseParameterString(
					request.getQueryString());
		} catch (ParseException ex) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT,
					ex.getMessage());
		}
		for (String name : paramNames) {
			if (params.containsKey(name)) {
				throw new BadRequestException(ErrorCode.INVALID_INPUT,
						"Query parameters not accepted, parameters must be set in the request body");
			}
		}
	}

	/**
	 * Runs the loginUsername query.
	 *
	 * @param version the protocol version
	 * @param username the email address or username
	 * @param password the password
	 * @param authDb the authentication database
	 * @return the login result
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Object loginUsername(ProtocolVersion version,
			HttpServletRequest request, HttpServletResponse response,
			String username, String password, boolean cookie,
			boolean autoExtendCookie, LoginUsernameParams loginParams,
			Database authDb) throws HttpException, Exception {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		validateForbiddenQueryParams(request, "username", "password");
		Integer expiration = LoginParams.DEFAULT_EXPIRATION;
		if (loginParams != null) {
			username = loginParams.getUsername();
			password = loginParams.getPassword();
			if (loginParams.getTokenExpiration() == null ||
					loginParams.getTokenExpiration() != 0) {
				expiration = loginParams.getTokenExpiration();
			}
			cookie = loginParams.isCookie();
			autoExtendCookie = loginParams.isAutoExtendCookie();
		}
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		if (username == null || username.isEmpty()) {
			fieldErrors.add(new HttpFieldError("username",
					"Parameter \"username\" not defined"));
		}
		if (password == null || password.isEmpty()) {
			fieldErrors.add(new HttpFieldError("password",
					"Parameter \"password\" not defined"));
		}
		if (!fieldErrors.isEmpty()) {
			logger.info("Failed login attempt: " + fieldErrors);
			throw BadRequestException.withInvalidInput(fieldErrors);
		}
		username = username.toLowerCase();
		if (username.contains("@")) {
			LoginResult loginResult = loginByEmail(version, request, response,
					username, password, expiration, cookie, autoExtendCookie,
					authDb);
			return getLoginUsernameResultForVersion(version, username,
					loginResult);
		}
		List<String> validDomains = new ArrayList<>();
		ProjectRepository projectRepo = AppComponents.get(
				ProjectRepository.class);
		List<BaseProject> projects = projectRepo.getProjects();
		for (BaseProject project : projects) {
			String domain = project.getUsernameDomain();
			if (domain != null && !validDomains.contains(domain))
				validDomains.add(domain);
		}
		UserCache userCache = UserCache.getInstance();
		List<User> users = userCache.findByEmailLocal(username);
		users = filterUsersByDomain(users, validDomains);
		if (users.size() != 1) {
			String invalidError = "Username or password is invalid, or there are more users with the same username in different domains";
			if (users.isEmpty()) {
				logger.info("Failed login attempt for username {}: user unknown",
						username);
			} else {
				logger.info("Failed login attempt for username {}: multiple users found with the same username",
						username);
			}
			throw new UnauthorizedException(
					ErrorCode.INVALID_CREDENTIALS, invalidError);
		}
		User user = users.get(0);
		LoginResult loginResult = loginByEmail(version, request, response,
				user.getEmail(), password, expiration, cookie,
				autoExtendCookie, authDb);
		return getLoginUsernameResultForVersion(version, username, loginResult);
	}

	private Object getLoginUsernameResultForVersion(ProtocolVersion version,
			String username, LoginResult loginResult)
			throws UnauthorizedException {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		if (version.ordinal() >= ProtocolVersion.V6_1_0.ordinal()) {
			return loginResult;
		}
		if (loginResult.getMfaRecord() != null) {
			logger.info("Failed login attempt for username {}: protocol version {} does not support MFA",
					username, version.versionName());
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal()) {
			return new LoginUsernameResult(loginResult.getUser(),
					loginResult.getToken(), username);
		} else {
			return new LoginUsernameResultV0(username,
					loginResult.getToken());
		}
	}

	/**
	 * Runs the query loginAs.
	 *
	 * @param version the protocol version
	 * @param asUserEmail email address of the user to log in as
	 * @param authDb the authentication database
	 * @param user the user who is logged in
	 * @return the authentication token
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public Object loginAs(ProtocolVersion version, String asUserEmail,
			Database authDb, User user) throws HttpException, Exception {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		ZonedDateTime now = DateTimeUtils.nowMs();
		asUserEmail = asUserEmail.toLowerCase();
		User asUser = User.findAccessibleUserByEmail(asUserEmail, authDb, user);
		if (!asUser.isActive()) {
			throw new ForbiddenException(String.format(
					"Account of user \"%s\" has been deactivated",
					asUserEmail));
		}
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("User {} logged in as: userid: {}, email: {}",
				user.getUserid(), asUser.getUserid(), asUser.getEmail());
		PrivateMfaRecord mfaRecord = asUser.findDefaultMfaRecord();
		String mfaId = mfaRecord == null ? null : mfaRecord.getId();
		String token = AuthToken.createToken(version, asUser, false, mfaId, now,
				LoginParams.DEFAULT_EXPIRATION, false, false, null);
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal())
			return new TokenResult(asUser.getUserid(), token);
		else
			return token;
	}
	
	/**
	 * Filters the specified list of users by domain. It only returns the users
	 * whose email address is in one of the specified domains.
	 * 
	 * @param users the users
	 * @param domains the valid domains (without @)
	 * @return the filtered users
	 */
	private List<User> filterUsersByDomain(List<User> users,
			List<String> domains) {
		List<User> result = new ArrayList<>();
		for (User user : users) {
			int sep = user.getEmail().indexOf('@');
			String userDomain = user.getEmail().substring(sep + 1);
			if (domains.contains(userDomain))
				result.add(user);
		}
		return result;
	}
	
	public String changePassword(ProtocolVersion version,
			HttpServletRequest request, HttpServletResponse response,
			String email, String oldPassword, String newPassword,
			ChangePasswordParams params, Database authDb, User user,
			AuthDetails authDetails) throws HttpException, Exception {
		validateForbiddenQueryParams(request, "email", "oldPassword",
				"newPassword");
		String userid = null;
		if (params != null) {
			userid = params.getUser();
			email = params.getEmail();
			oldPassword = params.getOldPassword();
			newPassword = params.getNewPassword();
		}
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		if (newPassword == null || newPassword.isEmpty()) {
			fieldErrors.add(new HttpFieldError("newPassword",
					"Parameter \"newPassword\" not defined"));
		}
		if (!fieldErrors.isEmpty())
			throw BadRequestException.withInvalidInput(fieldErrors);
		if (email != null)
			email = email.toLowerCase();
		UserCache userCache = UserCache.getInstance();
		User changeUser;
		if (userid != null && !userid.isEmpty()) {
			changeUser = userCache.findByUserid(userid);
			if (changeUser == null) {
				throw new NotFoundException(String.format(
						"User \"%s\" not found", userid));
			}
		} else if (email != null && !email.isEmpty()) {
			changeUser = userCache.findByEmail(email);
			if (changeUser == null) {
				throw new NotFoundException(String.format(
						"User \"%s\" not found", email));
			}
		} else {
			changeUser = user;
		}
		if (user.getRole() != Role.ADMIN &&
				!user.getUserid().equals(changeUser.getUserid())) {
			throw new ForbiddenException(
					"You can only change your own password");
		}
		boolean requireOldPassword = user.getRole() != Role.ADMIN &&
				!changeUser.isHasTemporaryPassword();
		if (requireOldPassword && (oldPassword == null ||
				oldPassword.isEmpty())) {
			throw BadRequestException.withInvalidInput(new HttpFieldError(
					"oldPassword", "Parameter \"oldPassword\" not defined"));
		}
		if (requireOldPassword) {
			byte[] salt = Base64.getDecoder().decode(changeUser.getSalt());
			String oldHash = hashPassword(oldPassword, salt);
			String oldHashDeprecated = hashPasswordDeprecated(oldPassword, salt);
			if (!oldHash.equals(changeUser.getPassword()) &&
					!oldHashDeprecated.equals(changeUser.getPassword())) {
				String msg = "Old password does not match";
				HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
				error.addFieldError(new HttpFieldError("oldPassword", msg));
				throw new BadRequestException(error);
			}
		}
		setPassword(changeUser, newPassword, "newPassword", false);
		userCache.updateUser(authDb, changeUser);
		ZonedDateTime now = DateTimeUtils.nowMs();
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("User {} changed password", changeUser.getUserid());
		String mfaId = null;
		Integer expireMinutes = LoginParams.DEFAULT_EXPIRATION;
		boolean cookie = false;
		boolean autoExtendCookie = false;
		if (authDetails != null) {
			mfaId = authDetails.getMfaId();
			expireMinutes = authDetails.toExpireMinutes();
			cookie = authDetails.isCookie();
			autoExtendCookie = authDetails.isAutoExtendCookie();
		}
		return AuthToken.createToken(version, changeUser, false, mfaId, now,
				expireMinutes, cookie, autoExtendCookie, response);
	}

	public void clearAuthTokenCookie(HttpServletResponse response) {
		Configuration config = AppComponents.get(Configuration.class);
		String baseUrl = config.get(Configuration.BASE_URL);
		SenSeeActCookie.clearAuthTokenCookie(baseUrl, response);
	}
	
	public Object requestResetPassword(HttpServletRequest request,
			String email, String templateName, Database authDb)
			throws HttpException, Exception {
		List<HttpFieldError> errors = new ArrayList<>();
		try {
			Validation.validateEmail(email);
		} catch (ValidationException ex) {
			errors.add(new HttpFieldError("email",
					"Invalid value for parameter \"email\": " +
					ex.getMessage()));
		}
		EmailTemplateRepository templateRepo = AppComponents.get(
				EmailTemplateRepository.class);
		EmailTemplate template = findEmailTemplate(
				templateRepo.getResetPasswordTemplates(),
				templateName, "template", errors);
		if (!errors.isEmpty())
			throw BadRequestException.withInvalidInput(errors);
		email = email.toLowerCase();
		UserCache userCache = UserCache.getInstance();
		User user = userCache.findByEmail(email);
		if (user == null)
			return null;
		sendResetPasswordMail(request, user, template, authDb);
		return null;
	}

	public Object resetPassword(HttpServletRequest request, String email,
			String resetCode, String password, ResetPasswordParams params,
			Database authDb) throws HttpException, Exception {
		validateForbiddenQueryParams(request, "email", "code", "password");
		if (params != null) {
			email = params.getEmail();
			resetCode = params.getCode();
			password = params.getPassword();
		}
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		if (email == null || email.isEmpty()) {
			fieldErrors.add(new HttpFieldError("email",
					"Parameter \"email\" not defined"));
		}
		if (resetCode == null || resetCode.isEmpty()) {
			fieldErrors.add(new HttpFieldError("code",
					"Parameter \"code\" not defined"));
		}
		if (password == null || password.isEmpty()) {
			fieldErrors.add(new HttpFieldError("password",
					"Parameter \"password\" not defined"));
		}
		if (!fieldErrors.isEmpty())
			throw BadRequestException.withInvalidInput(fieldErrors);
		email = email.toLowerCase();
		ZonedDateTime now = DateTimeUtils.nowMs();
		try {
			Validation.validateEmail(email);
		} catch (ValidationException ex) {
			HttpFieldError error = new HttpFieldError("email",
					"Invalid value for parameter \"email\": " +
					ex.getMessage());
			throw BadRequestException.withInvalidInput(error);
		}
		UserCache userCache = UserCache.getInstance();
		User user = userCache.findByEmail(email);
		if (user == null) {
			throw new NotFoundException(String.format(
					"User with email address \"%s\" not found", email));
		}
		HttpFieldError resetCodeError = new HttpFieldError("code",
				"The reset password code is invalid or no longer valid");
		if (user.getResetPasswordRequestCode() == null)
			throw BadRequestException.withInvalidInput(resetCodeError);
		ZonedDateTime validUntil = user.getResetPasswordRequestTime().plusHours(
				EMAIL_CODE_VALID_DURATION);
		if (!user.getResetPasswordRequestCode().equals(resetCode) ||
				now.isAfter(validUntil)) {
			user.setResetPasswordRequestCode(null);
			user.setResetPasswordRequestTime(null);
			userCache.updateUser(authDb, user);
			throw BadRequestException.withInvalidInput(resetCodeError);
		}
		setPassword(user, password, "password", false);
		user.setResetPasswordRequestCode(null);
		user.setResetPasswordRequestTime(null);
		userCache.updateUser(authDb, user);
		return null;
	}

	public MfaRecord addMfaRecord(HttpServletRequest request, String type,
			Database authDb, User user) throws HttpException, Exception {
		cleanMfaRecords(authDb, user);
		checkMaxMfaAddCount(user);
		if (type.equals(MfaRecord.Constants.TYPE_TOTP)) {
			return addMfaRecordTotp(authDb, user);
		} else if (type.equals(MfaRecord.Constants.TYPE_SMS)) {
			return addMfaRecordSms(request, authDb, user);
		} else {
			HttpFieldError error = new HttpFieldError("type",
					"Unknown MFA type: " + type);
			throw BadRequestException.withInvalidInput(error);
		}
	}

	private MfaRecord addMfaRecordTotp(Database authDb, User user)
			throws HttpException, Exception {
		checkMaxMfaType(user, MfaRecord.Constants.TYPE_TOTP,
				MAX_MFA_TOTP_COUNT);
		ZonedDateTime now = DateTimeUtils.nowMs();
		TwilioTotpData totpData = twilioCreateTotpFactor(user);
		PrivateMfaRecord record = new PrivateMfaRecord();
		record.setId(UUID.randomUUID().toString().toLowerCase()
				.replaceAll("-", ""));
		record.setType(MfaRecord.Constants.TYPE_TOTP);
		record.setCreated(now);
		Map<String,Object> publicData = new LinkedHashMap<>();
		publicData.put(PrivateMfaRecord.Constants.KEY_TOTP_BINDING_URI,
				totpData.bindingUri);
		record.setPublicData(publicData);
		Map<String,Object> privateData = new LinkedHashMap<>(publicData);
		privateData.put(PrivateMfaRecord.Constants.KEY_TOTP_FACTOR_SID,
				totpData.factorSid);
		record.setPrivateData(privateData);
		user.getMfaList().add(record);
		UserCache cache = UserCache.getInstance();
		cache.updateUser(authDb, user);
		return record.toPublicMfaRecord();
	}

	private MfaRecord addMfaRecordSms(HttpServletRequest request,
			Database authDb, User user) throws HttpException, Exception {
		checkMaxMfaType(user, MfaRecord.Constants.TYPE_SMS, MAX_MFA_SMS_COUNT);
		MfaRecordSmsInput input = readMfaRecordSmsInput(request);
		checkExistingMfaSms(user, input.phone);
		ZonedDateTime now = DateTimeUtils.nowMs();
		boolean verifyResult = twilioRequestSmsVerification(input.phone);
		if (!verifyResult) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT,
					"Invalid phone number: " + input.phone);
		}
		PrivateMfaRecord record = new PrivateMfaRecord();
		record.setId(UUID.randomUUID().toString().toLowerCase()
				.replaceAll("-", ""));
		record.setType(MfaRecord.Constants.TYPE_SMS);
		record.setCreated(now);
		Map<String,Object> data = new LinkedHashMap<>();
		data.put(PrivateMfaRecord.Constants.KEY_SMS_PHONE_NUMBER, input.phone);
		record.setPublicData(data);
		record.setPrivateData(new LinkedHashMap<>(data));
		user.getMfaList().add(record);
		UserCache cache = UserCache.getInstance();
		cache.updateUser(authDb, user);
		return record.toPublicMfaRecord();
	}

	private static class MfaRecordSmsInput {
		public String phone;
	}

	private MfaRecordSmsInput readMfaRecordSmsInput(HttpServletRequest request)
			throws BadRequestException, IOException {
		String json;
		try (InputStream input = request.getInputStream()) {
			json = FileUtils.readFileString(input);
		}
		Map<String,Object> map;
		try {
			map = JsonMapper.parse(json, new TypeReference<>() {});
		} catch (ParseException ex) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT,
					"Invalid JSON content");
		}
		MapReader mapReader = new MapReader(map);
		MfaRecordSmsInput result = new MfaRecordSmsInput();
		try {
			result.phone = mapReader.readStringRegex(
					MfaRecord.Constants.KEY_SMS_PHONE_NUMBER, "\\+[0-9]+");
		} catch (ParseException ex) {
			HttpFieldError error = new HttpFieldError(
					MfaRecord.Constants.KEY_SMS_PHONE_NUMBER, ex.getMessage());
			throw BadRequestException.withInvalidInput(error);
		}
		return result;
	}

	private void checkExistingMfaSms(User user, String phone)
			throws BadRequestException {
		for (PrivateMfaRecord record : user.findVerifiedMfaRecords()) {
			if (!record.getType().equals(MfaRecord.Constants.TYPE_SMS)) {
				continue;
			}
			String recordPhone = (String)record.getPrivateData().get(
					PrivateMfaRecord.Constants.KEY_SMS_PHONE_NUMBER);
			if (recordPhone.equals(phone)) {
				String msg = "MFA records with phone number already exists: " +
						phone;
				HttpError error = new HttpError(
						ErrorCode.AUTH_MFA_SMS_ALREADY_EXISTS, msg);
				error.addFieldError(new HttpFieldError(
						MfaRecord.Constants.KEY_SMS_PHONE_NUMBER, msg));
				throw new BadRequestException(error);
			}
		}
	}

	public Object requestMfaVerification(Database authDb, User user,
			String mfaId) throws HttpException, Exception {
		cleanMfaRecords(authDb, user);
		PrivateMfaRecord record = findMfaRecord(mfaId, user);
		if (record == null || record.getStatus() !=
				PrivateMfaRecord.Status.VERIFY_SUCCESS) {
			throw new NotFoundException("MFA record not found");
		}
		requestMfaVerification(authDb, user, record);
		return null;
	}

	private void requestMfaVerification(Database authDb, User user,
			PrivateMfaRecord record) throws HttpException, Exception {
		checkMaxMfaVerifyCount(record);
		String type = record.getType();
		if (type.equals(MfaRecord.Constants.TYPE_SMS)) {
			requestMfaVerificationSms(authDb, user, record);
		} else if (!type.equals(MfaRecord.Constants.TYPE_TOTP)) {
			Logger logger = AppComponents.getLogger(getClass().getSimpleName());
			logger.error("MFA type not supported: " + type);
			throw new InternalServerErrorException();
		}
	}

	private void requestMfaVerificationSms(Database authDb, User user,
			PrivateMfaRecord record) throws HttpException, Exception {
		UserCache cache = UserCache.getInstance();
		ZonedDateTime time = DateTimeUtils.nowMs();
		record.getVerifyTimes().add(time);
		cache.updateUser(authDb, user);
		String phone = (String)record.getPrivateData().get(
				PrivateMfaRecord.Constants.KEY_SMS_PHONE_NUMBER);
		boolean verifyResult = twilioRequestSmsVerification(phone);
		if (!verifyResult) {
			Logger logger = AppComponents.getLogger(getClass().getSimpleName());
			logger.error("Request SMS verification failed");
			throw new InternalServerErrorException();
		}
	}

	private void cleanMfaRecords(Database authDb, User user)
			throws DatabaseException {
		ZonedDateTime now = DateTimeUtils.nowMs();
		ZonedDateTime minAddTime = now.minusMinutes(MAX_MFA_ADD_MINUTES);
		ZonedDateTime minVerifyTime = now.minusMinutes(MAX_MFA_VERIFY_MINUTES);
		boolean changed = false;
		Iterator<PrivateMfaRecord> it = user.getMfaList().iterator();
		while (it.hasNext()) {
			PrivateMfaRecord record = it.next();
			if (record.getStatus() == PrivateMfaRecord.Status.VERIFY_SUCCESS) {
				if (cleanMfaRecordVerifyTimes(record, minVerifyTime))
					changed = true;
			} else if (record.getCreated().isBefore(minAddTime)) {
				it.remove();
				changed = true;
			}
		}
		if (!changed)
			return;
		UserCache cache = UserCache.getInstance();
		cache.updateUser(authDb, user);
	}

	private boolean cleanMfaRecordVerifyTimes(PrivateMfaRecord record,
			ZonedDateTime minVerifyTime) {
		boolean changed = false;
		Iterator<ZonedDateTime> it = record.getVerifyTimes().iterator();
		while (it.hasNext()) {
			ZonedDateTime time = it.next();
			if (time.isBefore(minVerifyTime)) {
				it.remove();
				changed = true;
			}
		}
		return changed;
	}

	private void checkMaxMfaAddCount(User user) throws BadRequestException {
		int count = 0;
		for (PrivateMfaRecord record : user.getMfaList()) {
			if (record.getStatus() != PrivateMfaRecord.Status.VERIFY_SUCCESS)
				count++;
		}
		if (count >= MAX_MFA_ADD_COUNT) {
			String msg = String.format(
					"Reached maximum number of add MFA attempts (%s in %s minutes)",
					MAX_MFA_ADD_COUNT, MAX_MFA_ADD_MINUTES);
			throw new BadRequestException(ErrorCode.AUTH_MFA_ADD_MAX, msg);
		}
	}

	private void checkMaxMfaVerifyCount(PrivateMfaRecord record)
			throws BadRequestException {
		int count = record.getVerifyTimes().size();
		if (count >= MAX_MFA_VERIFY_COUNT) {
			String msg = String.format(
					"Reached maximum number of MFA verification attempts (%s in %s minutes)",
					MAX_MFA_VERIFY_COUNT, MAX_MFA_VERIFY_MINUTES);
			throw new BadRequestException(ErrorCode.AUTH_MFA_VERIFY_MAX, msg);
		}
	}

	private void checkMaxMfaType(User user, String type, int max)
			throws BadRequestException {
		if (getVerifiedMfaTypeCount(user, type) == max) {
			String msg = String.format(
					"Already reached maximum number (%s) of MFA records with type \"%s\"",
					max, type);
			HttpError error = new HttpError(ErrorCode.AUTH_MFA_TYPE_MAX, msg);
			error.addFieldError(new HttpFieldError("type", msg));
			throw new BadRequestException(error);
		}
	}

	private int getVerifiedMfaTypeCount(User user, String type) {
		int count = 0;
		for (PrivateMfaRecord record : user.findVerifiedMfaRecords()) {
			if (type.equals(record.getType()))
				count++;
		}
		return count;
	}

	public VerifyAddMfaRecordResult verifyAddMfaRecord(ProtocolVersion version,
			HttpServletResponse response, Database authDb, User user,
			AuthDetails authDetails, VerifyMfaParams params)
			throws HttpException, Exception {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		ZonedDateTime now = DateTimeUtils.nowMs();
		cleanMfaRecords(authDb, user);
		validateVerifyMfaParams(params);
		String mfaId = params.getMfaId();
		String code = params.getCode();
		PrivateMfaRecord record = findMfaRecord(mfaId, user);
		if (record == null ||
				record.getStatus() == PrivateMfaRecord.Status.VERIFY_FAIL) {
			throw new NotFoundException("MFA record not found");
		}
		if (record.getStatus() == PrivateMfaRecord.Status.VERIFY_SUCCESS) {
			return createVerifyAddMfaRecordResult(version, response, user,
					authDetails, mfaId, record.toPublicMfaRecord());
		}
		checkMaxMfaVerifyCount(record);
		record.getVerifyTimes().add(now);
		UserCache cache = UserCache.getInstance();
		cache.updateUser(authDb, user);
		String type = record.getType();
		MfaRecord result;
		if (type.equals(MfaRecord.Constants.TYPE_TOTP)) {
			result = verifyAddMfaRecordTotp(record, code, authDb, user);
		} else if (type.equals(MfaRecord.Constants.TYPE_SMS)) {
			result = verifyAddMfaRecordSms(record, code, authDb, user);
		} else {
			logger.error("MFA type not supported: " + type);
			throw new InternalServerErrorException();
		}
		return createVerifyAddMfaRecordResult(version, response, user,
					authDetails, mfaId, result);
	}

	private VerifyAddMfaRecordResult createVerifyAddMfaRecordResult(
			ProtocolVersion version, HttpServletResponse response, User user,
			AuthDetails authDetails, String mfaId, MfaRecord record) {
		ZonedDateTime now = DateTimeUtils.nowMs();
		Integer expireMinutes = LoginParams.DEFAULT_EXPIRATION;
		boolean cookie = false;
		boolean autoExtendCookie = false;
		if (authDetails != null) {
			expireMinutes = authDetails.toExpireMinutes();
			cookie = authDetails.isCookie();
			autoExtendCookie = authDetails.isAutoExtendCookie();
		}
		String token = AuthToken.createToken(version, user, false, mfaId, now,
				expireMinutes, cookie, autoExtendCookie, response);
		return new VerifyAddMfaRecordResult(record, token);
	}

	private MfaRecord verifyAddMfaRecordTotp(PrivateMfaRecord record,
			String code, Database authDb, User user) throws HttpException,
			Exception {
		UserCache cache = UserCache.getInstance();
		try {
			checkMaxMfaType(user, MfaRecord.Constants.TYPE_TOTP,
					MAX_MFA_TOTP_COUNT);
		} catch (BadRequestException ex) {
			record.setStatus(PrivateMfaRecord.Status.VERIFY_FAIL);
			record.setPublicData(new LinkedHashMap<>());
			cache.updateUser(authDb, user);
			throw ex;
		}
		String factorSid = (String)record.getPrivateData().get(
				PrivateMfaRecord.Constants.KEY_TOTP_FACTOR_SID);
		boolean verifyResult = twilioAddTotpVerificationCheck(user, factorSid,
				code);
		if (!verifyResult) {
			if (record.getVerifyTimes().size() > MAX_MFA_ADD_VERIFY)
				record.setStatus(PrivateMfaRecord.Status.VERIFY_FAIL);
			record.setPublicData(new LinkedHashMap<>());
			cache.updateUser(authDb, user);
			HttpFieldError error = new HttpFieldError("code",
					"Invalid verification code");
			throw BadRequestException.withInvalidInput(error);
		}
		record.setStatus(PrivateMfaRecord.Status.VERIFY_SUCCESS);
		record.getPublicData().remove(
				PrivateMfaRecord.Constants.KEY_TOTP_BINDING_URI);
		cache.updateUser(authDb, user);
		return record.toPublicMfaRecord();
	}

	private MfaRecord verifyAddMfaRecordSms(PrivateMfaRecord record,
			String code, Database authDb, User user) throws HttpException,
			Exception {
		UserCache cache = UserCache.getInstance();
		try {
			checkMaxMfaType(user, MfaRecord.Constants.TYPE_SMS,
					MAX_MFA_SMS_COUNT);
		} catch (BadRequestException ex) {
			record.setStatus(PrivateMfaRecord.Status.VERIFY_FAIL);
			record.setPublicData(new LinkedHashMap<>());
			cache.updateUser(authDb, user);
			throw ex;
		}
		String phone = (String)record.getPrivateData().get(
				PrivateMfaRecord.Constants.KEY_SMS_PHONE_NUMBER);
		checkExistingMfaSms(user, phone);
		boolean verifyResult = twilioSmsVerificationCheck(phone, code);
		if (!verifyResult) {
			if (record.getVerifyTimes().size() > MAX_MFA_ADD_VERIFY)
				record.setStatus(PrivateMfaRecord.Status.VERIFY_FAIL);
			record.setPublicData(new LinkedHashMap<>());
			cache.updateUser(authDb, user);
			HttpFieldError error = new HttpFieldError("code",
					"Invalid verification code");
			throw BadRequestException.withInvalidInput(error);
		}
		record.setStatus(PrivateMfaRecord.Status.VERIFY_SUCCESS);
		record.getPublicData().remove(
				PrivateMfaRecord.Constants.KEY_SMS_PHONE_NUMBER);
		record.getPublicData().put(
				PrivateMfaRecord.Constants.KEY_SMS_PARTIAL_PHONE_NUMBER,
				getPartialPhoneNumber(phone));
		cache.updateUser(authDb, user);
		return record.toPublicMfaRecord();
	}

	private String getPartialPhoneNumber(String phone) {
		if (phone.length() < 5)
			return phone;
		return phone.substring(0, 3) +
				"*".repeat(phone.length() - 5) +
				phone.substring(phone.length() - 2);
	}

	private TwilioTotpData twilioCreateTotpFactor(User user) throws HttpClientException,
			ParseException, IOException {
		Configuration config = AppComponents.get(Configuration.class);
		String serviceSid = config.get(Configuration.TWILIO_VERIFY_SERVICE_SID);
		String url = String.format(
				"https://verify.twilio.com/v2/Services/%s/Entities/%s/Factors",
				serviceSid, user.getUserid());
		Map<String,String> params = new LinkedHashMap<>();
		params.put("FriendlyName", user.getEmail());
		params.put("FactorType", "totp");
		Map<String,Object> response;
		try (HttpClient2 httpClient = new HttpClient2(url)) {
			addTwilioAuthHeader(httpClient);
			response = httpClient.setMethod("POST")
					.writePostParams(params)
					.readJson(new TypeReference<>() {});
		}
		TwilioTotpData result = new TwilioTotpData();
		MapReader reader = new MapReader(response);
		result.factorSid = reader.readString("sid");
		Map<String,Object> binding = reader.readJson("binding",
				new TypeReference<>() {});
		MapReader bindingReader = new MapReader(binding);
		result.bindingUri = bindingReader.readString("uri");
		return result;
	}

	private static class TwilioTotpData {
		public String factorSid;
		public String bindingUri;
	}

	private boolean twilioAddTotpVerificationCheck(User user, String factorSid,
			String code) throws HttpClientException, ParseException,
			IOException {
		Configuration config = AppComponents.get(Configuration.class);
		String serviceSid = config.get(Configuration.TWILIO_VERIFY_SERVICE_SID);
		String url = String.format(
				"https://verify.twilio.com/v2/Services/%s/Entities/%s/Factors/%s",
				serviceSid, user.getUserid(), factorSid);
		Map<String,String> params = new LinkedHashMap<>();
		params.put("AuthPayload", code);
		Map<String,Object> response;
		try (HttpClient2 httpClient = new HttpClient2(url)) {
			addTwilioAuthHeader(httpClient);
			response = httpClient.setMethod("POST")
					.writePostParams(params)
					.readJson(new TypeReference<>() {});
		}
		Object status = response.get("status");
		if (!(status instanceof String)) {
			throw new ParseException("Unexpected verify response");
		}
		return "verified".equals(status);
	}

	private boolean twilioTotpChallengeCheck(User user, String factorSid,
			String code) throws HttpClientException, ParseException,
			IOException {
		Configuration config = AppComponents.get(Configuration.class);
		String serviceSid = config.get(Configuration.TWILIO_VERIFY_SERVICE_SID);
		String url = String.format(
				"https://verify.twilio.com/v2/Services/%s/Entities/%s/Challenges",
				serviceSid, user.getUserid());
		Map<String,String> params = new LinkedHashMap<>();
		params.put("AuthPayload", code);
		params.put("FactorSid", factorSid);
		Map<String,Object> response;
		try (HttpClient2 httpClient = new HttpClient2(url)) {
			addTwilioAuthHeader(httpClient);
			response = httpClient.setMethod("POST")
					.writePostParams(params)
					.readJson(new TypeReference<>() {});
		}
		Object status = response.get("status");
		if (!(status instanceof String)) {
			throw new ParseException("Unexpected verify response");
		}
		return "approved".equals(status);
	}

	private boolean twilioRequestSmsVerification(String phone)
			throws HttpClientException, ParseException, IOException {
		Configuration config = AppComponents.get(Configuration.class);
		String serviceSid = config.get(Configuration.TWILIO_VERIFY_SERVICE_SID);
		String url = String.format(
				"https://verify.twilio.com/v2/Services/%s/Verifications",
				serviceSid);
		Map<String,String> params = new LinkedHashMap<>();
		params.put("To", phone);
		params.put("Channel", "sms");
		Map<String,Object> response;
		try (HttpClient2 httpClient = new HttpClient2(url)) {
			addTwilioAuthHeader(httpClient);
			response = httpClient.setMethod("POST")
				.writePostParams(params)
				.readJson(new TypeReference<>() {});
		}
		Object status = response.get("status");
		if (status instanceof Integer statusCode) {
			if (statusCode == 400)
				return false;
		}
		if (!"pending".equals(status))
			throw new ParseException("Unexpected verify response");
		return true;
	}

	private boolean twilioSmsVerificationCheck(String phone, String code)
			throws HttpClientException, ParseException, IOException {
		Configuration config = AppComponents.get(Configuration.class);
		String serviceSid = config.get(Configuration.TWILIO_VERIFY_SERVICE_SID);
		String url = String.format(
				"https://verify.twilio.com/v2/Services/%s/VerificationCheck",
				serviceSid);
		Map<String,String> params = new LinkedHashMap<>();
		params.put("To", phone);
		params.put("Code", code);
		Map<String,Object> response;
		try (HttpClient2 httpClient = new HttpClient2(url)) {
			addTwilioAuthHeader(httpClient);
			response = httpClient.setMethod("POST")
					.writePostParams(params)
					.readJson(new TypeReference<>() {});
		}
		Object status = response.get("status");
		if (!(status instanceof String)) {
			throw new ParseException("Unexpected verify response");
		}
		return "approved".equals(status);
	}

	private void addTwilioAuthHeader(HttpClient2 httpClient) {
		Configuration config = AppComponents.get(Configuration.class);
		String accountSid = config.get(Configuration.TWILIO_ACCOUNT_SID);
		String authToken = config.get(Configuration.TWILIO_AUTH_TOKEN);
		String auth = accountSid + ":" + authToken;
		String base64Auth = Base64.getEncoder().encodeToString(auth.getBytes());
		httpClient.addHeader("Authorization", "Basic " + base64Auth);
	}

	private PrivateMfaRecord findMfaRecord(String id, User user) {
		for (PrivateMfaRecord record : user.getMfaList()) {
			if (id.equals(record.getId())) {
				return record;
			}
		}
		return null;
	}

	public Object deleteMfaRecord(String id, Database authDb, User user)
			throws HttpException, Exception {
		cleanMfaRecords(authDb, user);
		PrivateMfaRecord record = findMfaRecord(id, user);
		if (record == null || record.getStatus() !=
				PrivateMfaRecord.Status.VERIFY_SUCCESS) {
			return null;
		}
		user.getMfaList().remove(record);
		UserCache cache = UserCache.getInstance();
		cache.updateUser(authDb, user);
		return null;
	}

	public List<MfaRecord> getMfaRecords(User user) {
		List<MfaRecord> result = new ArrayList<>();
		for (PrivateMfaRecord record : user.findVerifiedMfaRecords()) {
			result.add(record.toPublicMfaRecord());
		}
		return result;
	}

	public Object setDefaultMfaRecord(Database authDb, User user, String mfaId)
			throws HttpException, Exception {
		cleanMfaRecords(authDb, user);
		PrivateMfaRecord record = findMfaRecord(mfaId, user);
		if (record == null || record.getStatus() !=
				PrivateMfaRecord.Status.VERIFY_SUCCESS) {
			throw new NotFoundException("MFA record not found");
		}
		List<PrivateMfaRecord> list = user.getMfaList();
		list.remove(record);
		list.add(0, record);
		UserCache cache = UserCache.getInstance();
		cache.updateUser(authDb, user);
		return null;
	}

	public Object getMfaAddTotpQRCode(HttpServletResponse response,
			Database authDb, User user, String mfaId) throws HttpException,
			Exception {
		cleanMfaRecords(authDb, user);
		PrivateMfaRecord record = findMfaRecord(mfaId, user);
		if (record == null || record.getStatus() ==
				PrivateMfaRecord.Status.VERIFY_FAIL) {
			throw new NotFoundException("MFA record not found");
		}
		if (!MfaRecord.Constants.TYPE_TOTP.equals(record.getType())) {
			throw new BadRequestException(String.format(
					"MFA record does not have type \"%s\"",
					MfaRecord.Constants.TYPE_TOTP));
		}
		String uri = (String)record.getPrivateData().get(
				PrivateMfaRecord.Constants.KEY_TOTP_BINDING_URI);
		QRCodeWriter qrWriter = new QRCodeWriter();
		BitMatrix matrix = qrWriter.encode(uri, BarcodeFormat.QR_CODE, 500,
				500);
		response.setHeader("Content-Type", "image/png");
		try (OutputStream out = response.getOutputStream()) {
			MatrixToImageWriter.writeToStream(matrix, "png", out);
		}
		return null;
	}

	public LoginResult verifyMfaCode(ProtocolVersion version,
			HttpServletResponse response, Database authDb, User user,
			AuthDetails authDetails, VerifyMfaParams params)
			throws HttpException, Exception {
		ZonedDateTime now = DateTimeUtils.nowMs();
		validateVerifyMfaParams(params);
		String mfaId = params.getMfaId();
		String code = params.getCode();
		cleanMfaRecords(authDb, user);
		if (authDetails == null) {
			throw new BadRequestException("MFA verification not initiated");
		}
		if (authDetails.isPendingMfa() ||
				!mfaId.equals(authDetails.getMfaId())) {
			performVerifyMfaCode(authDb, user, now, mfaId, code);
		}
		Integer expireMinutes = authDetails.toExpireMinutes();
		boolean cookie = authDetails.isCookie();
		boolean autoExtendCookie = authDetails.isAutoExtendCookie();
		String token = AuthToken.createToken(version, user, false, mfaId, now,
				expireMinutes, cookie, autoExtendCookie, response);
		LoginResult result = new LoginResult();
		result.setStatus(LoginResult.Status.COMPLETE);
		result.setUser(user.getUserid());
		result.setEmail(user.getEmail());
		result.setToken(token);
		return result;
	}

	private void validateVerifyMfaParams(VerifyMfaParams params)
			throws HttpException {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		String mfaId = params.getMfaId();
		String code = params.getCode();
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		if (mfaId == null || mfaId.isEmpty()) {
			fieldErrors.add(new HttpFieldError("mfaId",
					"Parameter \"mfaId\" not defined"));
		}
		if (code == null || code.isEmpty()) {
			fieldErrors.add(new HttpFieldError("code",
					"Parameter \"code\" not defined"));
		}
		if (!fieldErrors.isEmpty()) {
			logger.info("Failed verify MFA attempt: " + fieldErrors);
			throw BadRequestException.withInvalidInput(fieldErrors);
		}
	}

	private void performVerifyMfaCode(Database authDb, User user,
			ZonedDateTime now, String mfaId, String code) throws HttpException,
			Exception {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		PrivateMfaRecord record = user.findVerifiedMfaRecord(mfaId);
		if (record == null)
			throw new NotFoundException("MFA record not found");
		checkMaxMfaVerifyCount(record);
		record.getVerifyTimes().add(now);
		UserCache cache = UserCache.getInstance();
		cache.updateUser(authDb, user);
		String type = record.getType();
		boolean verifyResult;
		if (type.equals(MfaRecord.Constants.TYPE_TOTP)) {
			verifyResult = verifyMfaCodeTotp(user, record, code);
		} else if (type.equals(MfaRecord.Constants.TYPE_SMS)) {
			verifyResult = verifyMfaCodeSms(record, code);
		} else {
			logger.error("MFA type not supported: " + type);
			throw new InternalServerErrorException();
		}
		if (!verifyResult) {
			HttpFieldError error = new HttpFieldError("code",
					"Invalid verification code");
			throw BadRequestException.withInvalidInput(error);
		}
		logger.info("User logged in with MFA: userid: {}, email: {}",
				user.getUserid(), user.getEmail());
	}

	private boolean verifyMfaCodeTotp(User user, PrivateMfaRecord record,
			String code) throws HttpException, Exception {
		String factorSid = (String)record.getPrivateData().get(
				PrivateMfaRecord.Constants.KEY_TOTP_FACTOR_SID);
		return twilioTotpChallengeCheck(user, factorSid, code);
	}

	private boolean verifyMfaCodeSms(PrivateMfaRecord record, String code)
			throws HttpException, Exception {
		String phone = (String)record.getPrivateData().get(
				PrivateMfaRecord.Constants.KEY_SMS_PHONE_NUMBER);
		return twilioSmsVerificationCheck(phone, code);
	}

	private static EmailTemplate findEmailTemplate(
			EmailTemplateCollection templates, String template,
			String paramName, List<HttpFieldError> errors) {
		if (template != null && !template.isEmpty()) {
			try {
				return templates.find(template);
			} catch (IllegalArgumentException ex) {
				errors.add(new HttpFieldError(paramName,
						String.format("Invalid value for parameter \"%s\"",
						paramName)));
			}
		}
		return templates.getDefault();
	}

	private void sendResetPasswordMail(HttpServletRequest request, User user,
			EmailTemplate template, Database authDb) throws DatabaseException,
			MessagingException, IOException {
		UserCache userCache = UserCache.getInstance();
		ZonedDateTime now = DateTimeUtils.nowMs();
		String code = UUID.randomUUID().toString().toLowerCase().replace(
				"-", "");
		user.setResetPasswordRequestCode(code);
		user.setResetPasswordRequestTime(now);
		userCache.updateUser(authDb, user);
		Map<String,Object> contentParams = new LinkedHashMap<>();
		contentParams.put("code", code);
		getEmailSender().trySendThread(template, request, user, user.getEmail(),
				contentParams);
	}

	public static void sendNewUserMail(HttpServletRequest request, User user,
			EmailTemplate template, Database authDb) throws DatabaseException,
			MessagingException, IOException {
		ZonedDateTime now = DateTimeUtils.nowMs();
		UserCache userCache = UserCache.getInstance();
		String code = createEmailVerificationCode(user, now);
		user.setVerifyEmailRequestCode(code);
		user.setVerifyEmailRequestTime(now);
		userCache.updateUser(authDb, user);
		String to = user.getEmailPendingVerification() != null ?
				user.getEmailPendingVerification() :
				user.getEmail();
		Map<String,Object> contentParams = new LinkedHashMap<>();
		contentParams.put("code", code);
		getEmailSender().trySendThread(template, request, user, to,
				contentParams);
	}

	private static EmailSender getEmailSender() {
		Configuration config = AppComponents.get(Configuration.class);
		return new EmailSender(config.toEmailConfig());
	}

	public static String createEmailVerificationCode(User user,
			ZonedDateTime now) {
		String currCode = user.getVerifyEmailRequestCode();
		ZonedDateTime currTime = user.getVerifyEmailRequestTime();
		if (currCode != null && currTime != null) {
			ZonedDateTime validUntil = currTime.plusHours(
					EMAIL_CODE_VALID_DURATION);
			if (now.isBefore(validUntil))
				return currCode;
		}
		return UUID.randomUUID().toString().toLowerCase().replace("-", "");
	}

	/**
	 * Sets the password for the specified user. It validates whether the
	 * password has at least 6 characters. Then it generates a salt, creates
	 * a password hash and sets the properties 'password' and 'salt' in the
	 * User object.
	 * 
	 * @param user the user
	 * @param password the password
	 * @param field the name of the input field
	 * @throws HttpException if the password is invalid
	 */
	public static void setPassword(User user, String password, String field,
			boolean isTempPassword) throws HttpException {
		if (password.length() < 6) {
			String msg = "Password must be at least 6 characters";
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
			error.addFieldError(new HttpFieldError(field, msg));
			throw new BadRequestException(error);
		}
		byte[] salt = new byte[32];
		SecureRandom random = new SecureRandom();
		random.nextBytes(salt);
		String hash = hashPassword(password, salt);
		user.setPassword(hash);
		user.setSalt(Base64.getEncoder().encodeToString(salt));
		user.setHasTemporaryPassword(isTempPassword);
	}
	
	/**
	 * Hashes the specified password, secret salt and specified salt and returns
	 * the hash as a Base64 string.
	 * 
	 * @param password the password
	 * @param salt the salt
	 * @return the hash as a Base64 string
	 */
	private static String hashPassword(String password, byte[] salt) {
		Configuration config = AppComponents.get(Configuration.class);
		String secretSaltBase64 = config.get(Configuration.SECRET_SALT);
		byte[] secretSalt = Base64.getDecoder().decode(secretSaltBase64);
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException("Algorithm SHA-256 not found: " +
					ex.getMessage(), ex);
		}
		md.update(secretSalt);
		md.update(salt);
		Charset utf8 = StandardCharsets.UTF_8;
		byte[] bs = md.digest(password.getBytes(utf8));
		return Base64.getEncoder().encodeToString(bs);
	}
	
	/**
	 * Hashes the specified password and salt and returns the hash as a Base64
	 * string. This is the old hash method without secret salt.
	 * 
	 * @param password the password
	 * @param salt the salt
	 * @return the hash as a Base64 string
	 */
	private static String hashPasswordDeprecated(String password, byte[] salt) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException("Algorithm SHA-256 not found: " +
					ex.getMessage(), ex);
		}
		md.update(salt);
		Charset utf8 = StandardCharsets.UTF_8;
		byte[] bs = md.digest(password.getBytes(utf8));
		return Base64.getEncoder().encodeToString(bs);
	}
}
