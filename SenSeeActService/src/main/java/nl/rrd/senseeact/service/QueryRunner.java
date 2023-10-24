package nl.rrd.senseeact.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.service.controller.AuthControllerExecution;
import nl.rrd.senseeact.service.controller.ProjectControllerExecution;
import nl.rrd.senseeact.service.exception.*;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;
import nl.rrd.senseeact.service.sso.SSOToken;
import nl.rrd.senseeact.service.sso.SSOTokenRepository;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * This class can run queries on the authentication database or a project
 * database. It can validate an authentication token and it can validate
 * whether the authenticated user has access to a project. Then it connects
 * to the database server and loads the needed databases. It will create,
 * initialise or upgrade the databases if needed. See {@link DatabaseLoader
 * DatabaseLoader}. When the query is completed, it will close the connection.
 * 
 * @author Dennis Hofs (RRD)
 */
public class QueryRunner {

	/**
	 * Runs a query on the authentication database. If the HTTP request is
	 * specified, it will validate the authentication token. If there is no
	 * token in the request, or the token is empty or invalid, it throws an
	 * HttpException with 401 Unauthorized. If the request is null, it will not
	 * validate anything. This can be used for a login or signup.
	 * 
	 * @param query the query
	 * @param versionName the protocol version name (see {@link ProtocolVersion
	 * ProtocolVersion})
	 * @param request the HTTP request or null
	 * @param response the HTTP response (to add header WWW-Authenticate in
	 * case of 401 Unauthorized)
	 * @return the query result
	 * @throws HttpException if the query should return an HTTP error status
	 * @throws Exception if an unexpected error occurs. This results in HTTP
	 * error status 500 Internal Server Error.
	 */
	public static <T> T runAuthQuery(AuthQuery<T> query,
			String versionName, HttpServletRequest request,
			HttpServletResponse response) throws HttpException, Exception {
		ProtocolVersion version;
		try {
			version = ProtocolVersion.forVersionName(versionName);
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException("Unknown protocol version: " +
					versionName);
		}
		DatabaseConnection conn = null;
		try {
			DatabaseLoader dbLoader = DatabaseLoader.getInstance();
			conn = dbLoader.openConnection();
			Database authDb = dbLoader.initAuthDatabase(conn);
			User user = null;
			if (request != null)
				user = validateToken(version, request, response, authDb, null);
			return query.runQuery(version, authDb, user);
		} catch (UnauthorizedException ex) {
			response.addHeader("WWW-Authenticate", "None");
			throw ex;
		} catch (HttpException ex) {
			throw ex;
		} catch (Exception ex) {
			Logger logger = AppComponents.getLogger(
					QueryRunner.class.getSimpleName());
			String stackTrace;
			StringWriter stringWriter = new StringWriter();
			try (PrintWriter writer = new PrintWriter(stringWriter)) {
				ex.printStackTrace(writer);
				stackTrace = stringWriter.getBuffer().toString();
			}
			logger.error("Internal Server Error in auth query: " +
					ex.getMessage() + ": " + stackTrace, ex);
			throw new InternalServerErrorException();
		} finally {
			if (conn != null)
				conn.close();
		}
	}
	
	/**
	 * Runs a query on a project database. It will validate the authentication
	 * token in the specified HTTP request. If no token is specified, or the
	 * token is empty or invalid, it throws an HttpException with
	 * 401 Unauthorized.
	 * 
	 * @param query the query
	 * @param versionName the protocol version name (see {@link ProtocolVersion
	 * ProtocolVersion})
	 * @param project the project code
	 * @param request the HTTP request
	 * @param response the HTTP response (to add header WWW-Authenticate in
	 * case of 401 Unauthorized)
	 * @return the query result
	 * @throws HttpException if the query should return an HTTP error status
	 * @throws Exception if an unexpected error occurs. This results in HTTP
	 * error status 500 Internal Server Error.
	 */
	public static <T> T runProjectQuery(ProjectQuery<T> query,
			String versionName, String project, HttpServletRequest request,
			HttpServletResponse response) throws HttpException, Exception {
		return runProjectQuery(query, versionName, project, request, response,
				null);
	}

	public static <T> T runProjectQuery(ProjectQuery<T> query,
			String versionName, String project, HttpServletRequest request,
			HttpServletResponse response, String logId) throws HttpException,
			Exception {
		Logger logger = AppComponents.getLogger(
				QueryRunner.class.getSimpleName());
		if (logId != null) {
			logger.info("Run project query {} start, project {}", logId, project);
		}
		ProtocolVersion version;
		try {
			version = ProtocolVersion.forVersionName(versionName);
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException("Unknown protocol version: " +
					versionName);
		}
		DatabaseConnection conn = null;
		try {
			DatabaseLoader dbLoader = DatabaseLoader.getInstance();
			if (logId != null) {
				logger.info("Run project query {} before open database connection, project {}",
						logId, project);
			}
			conn = dbLoader.openConnection();
			if (logId != null) {
				logger.info("Run project query {} after open database connection, project {}",
						logId, project);
			}
			Database authDb = dbLoader.initAuthDatabase(conn);
			if (logId != null) {
				logger.info("Run project query {} after init auth database, project {}",
						logId, project);
			}
			User user = validateToken(version, request, response, authDb,
					project);
			if (logId != null) {
				logger.info("Run project query {} validated token, project {}, user {}",
						logId, project, user.getUserid());
			}
			BaseProject baseProject =
					ProjectControllerExecution.findUserProject(project, authDb,
					user);
			Database projectDb = dbLoader.initProjectDatabase(conn, project);
			if (logId != null) {
				logger.info("Run project query {} after init sample database, project {}, user {}",
						logId, project, user.getUserid());
			}
			T result = query.runQuery(version, authDb, projectDb, user,
					baseProject);
			if (logId != null) {
				logger.info("Run project query {} after run query, project {}, user {}",
						logId, project, user.getUserid());
			}
			return result;
		} catch (UnauthorizedException ex) {
			response.addHeader("WWW-Authenticate", "None");
			throw ex;
		} catch (HttpException ex) {
			throw ex;
		} catch (Exception ex) {
			String stackTrace;
			StringWriter stringWriter = new StringWriter();
			try (PrintWriter writer = new PrintWriter(stringWriter)) {
				ex.printStackTrace(writer);
				stackTrace = stringWriter.getBuffer().toString();
			}
			logger.error("Internal Server Error in project query: " +
					ex.getMessage() + ": " + stackTrace, ex);
			throw new InternalServerErrorException();
		} finally {
			if (conn != null)
				conn.close();
		}
	}

	/**
	 * Validates the authentication token in the specified HTTP request. If the
	 * validation fails, this method throws an HttpException with
	 * 401 Unauthorized. This happens in the following cases:
	 *
	 * <p><ul>
	 * <li>No token is specified</li>
	 * <li>The token is empty or invalid</li>
	 * <li>The authenticated user is inactive</li>
	 * </ul></p>
	 *
	 * <p>An error code is included with the exception.</p>
	 *
	 * <p>If the validation succeeds, this method will return the user object
	 * for the authenticated user.</p>
	 *
	 * <p>The general token can be specified in header X-Auth-Token or in cookie
	 * authToken. In addition to the general token, this method can validate SSO
	 * tokens for specific projects. In that case this method also checks if the
	 * user is a member of that project. If the request accesses a specific
	 * project, it also checks whether that is the same as the SSO project.
	 * Furthermore it checks the user role. You cannot authenticate as an admin
	 * using an SSO token. And if you authenticate as a professional, you can
	 * only access project data, and not for example the user profile or user
	 * access functions.</p>
	 *
	 * <p>In protocol versions since 6.0.0, the SSO token should contain the
	 * user ID. In older versions it should contain the email address of the
	 * user.</p>
	 *
	 * <p>The general token may be configured so that the token and authToken
	 * cookie should be extended at every successful validation. In that case
	 * this method will set the extended cookie in the specified HTTP
	 * response.</p>
	 * 
	 * @param authDb the authentication database
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param project the code of the project that the user wants to access,
	 * or null
	 * @return the authenticated user
	 * @throws UnauthorizedException if no token is specified, or the token is
	 * empty or invalid
	 * @throws DatabaseException if a database error occurs
	 */
	private static User validateToken(ProtocolVersion version,
			HttpServletRequest request, HttpServletResponse response,
			Database authDb, String project) throws HttpException, Exception {
		ValidateTokenResult result = getAuthenticatedUser(version, request,
				response, authDb, project);
		if (!result.getUser().isActive()) {
			throw new UnauthorizedException(ErrorCode.ACCOUNT_INACTIVE,
					"Account has been deactivated");
		}
		if (result.getAuthDetails() != null && result.getAuthDetails()
				.isAutoExtendCookie()) {
			ZonedDateTime now = DateTimeUtils.nowMs();
			AuthToken.createToken(version, result.getUser(), now,
					result.getAuthDetails().getAutoExtendCookieMinutes(), true,
					true, response);
		}
		User user = result.getUser();
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			UserCache userCache = UserCache.getInstance();
			ZonedDateTime now = DateTimeUtils.nowMs(user.toTimeZone());
			user.setLastActive(now);
			userCache.updateUser(authDb, user);
		}
		return user;
	}

	private static ValidateTokenResult getAuthenticatedUser(
			ProtocolVersion version, HttpServletRequest request,
			HttpServletResponse response,  Database authDb, String project)
			throws HttpException, Exception {
		String token = request.getHeader("X-Auth-Token");
		if (token != null)
			return validateDefaultToken(token);
		SSOTokenRepository ssoRepo = AppComponents.get(
				SSOTokenRepository.class);
		for (SSOToken ssoToken : ssoRepo.getTokens()) {
			if (ssoToken.requestHasToken(request)) {
				return ssoToken.validateToken(version, request, response,
						authDb, project);
			}
		}
		token = findAuthTokenCookie(request);
		if (token != null && !token.isEmpty())
			return validateDefaultToken(token);
		throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_NOT_FOUND,
				"Authentication token not found");
	}

	private static String findAuthTokenCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null)
			return null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals("authToken"))
				return cookie.getValue();
		}
		return null;
	}
	
	/**
	 * Validates a token from request header X-Auth-Token. If it's empty or
	 * invalid, it will throw an HttpException with 401 Unauthorized. Otherwise
	 * it will return the user object for the authenticated user.
	 * 
	 * @param token the authentication token (not null)
	 * @return the authenticated user
	 * @throws UnauthorizedException if the token is empty or invalid
	 */
	public static ValidateTokenResult validateDefaultToken(String token)
			throws UnauthorizedException {
		Logger logger = AppComponents.getLogger(
				QueryRunner.class.getSimpleName());
		if (token.trim().isEmpty()) {
			logger.info("Invalid auth token: Token empty");
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		AuthDetails details;
		try {
			details = AuthToken.parseToken(token);
		} catch (ExpiredAuthTokenException ex) {
			logger.info("Expired auth token: " + ex.getMessage());
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_EXPIRED,
					"Authentication token expired");
		} catch (InvalidAuthTokenException ex) {
			logger.info("Invalid auth token: " + ex.getMessage());
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		UserCache userCache = UserCache.getInstance();
		String userid;
		User user;
		if (details.getUserid() != null) {
			userid = details.getUserid();
			user = userCache.findByUserid(details.getUserid());
		} else {
			userid = details.getEmail();
			user = userCache.findByEmail(details.getEmail());
		}
		if (user == null) {
			logger.info("Invalid auth token: User not found: " + userid);
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		long now = System.currentTimeMillis();
		if (details.getExpiration() != null &&
				details.getExpiration().getTime() < now) {
			ZonedDateTime time = ZonedDateTime.ofInstant(
					details.getExpiration().toInstant(),
					ZoneId.systemDefault());
			logger.info("Expired auth token: " + time.format(
					DateTimeUtils.ZONED_FORMAT));
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_EXPIRED,
					"Authentication token expired");
		}
		if (!AuthDetails.hashSalt(user.getSalt()).equals(details.getHash())) {
			logger.info("Invalid auth token: Invalid salt hash");
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		return new ValidateTokenResult(user, details);
	}
}
