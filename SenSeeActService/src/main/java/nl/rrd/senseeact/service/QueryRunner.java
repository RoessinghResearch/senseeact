package nl.rrd.senseeact.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.AuthHeader;
import nl.rrd.senseeact.client.SenSeeActClient;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.exception.InternalServerErrorException;
import nl.rrd.senseeact.service.exception.UnauthorizedException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.utils.AppComponents;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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
	 * <p>This method uses the default {@link QueryContext QueryContext}.</p>
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
	public static <T> T runAuthQuery(AuthQuery<T> query, String versionName,
			HttpServletRequest request, HttpServletResponse response)
			throws HttpException, Exception {
		return runAuthQuery(query, versionName, request, response,
				new QueryContext());
	}

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
	 * @param context the query context
	 * @return the query result
	 * @throws HttpException if the query should return an HTTP error status
	 * @throws Exception if an unexpected error occurs. This results in HTTP
	 * error status 500 Internal Server Error.
	 */
	public static <T> T runAuthQuery(AuthQuery<T> query, String versionName,
			HttpServletRequest request, HttpServletResponse response,
			QueryContext context) throws HttpException, Exception {
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
			if (request != null) {
				user = AuthTokenValidator.validate(version, request, response,
						authDb, context);
			}
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
			QueryContext context = new QueryContext().setProject(project);
			User user = AuthTokenValidator.validate(version, request, response,
					authDb, context);
			if (logId != null) {
				logger.info("Run project query {} validated token, project {}, user {}",
						logId, project, user.getUserid());
			}
			ProjectRepository projectRepo = AppComponents.get(
					ProjectRepository.class);
			BaseProject baseProject = projectRepo.findProjectByCode(project);
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

	public static SenSeeActClient getSelfClient(HttpServletRequest request) {
		Configuration config = AppComponents.get(Configuration.class);
		String baseUrl = config.getBaseUrl();
		String path = baseUrl.replaceAll("^https?://[^/]+", "");
		baseUrl = "http://localhost:8080" + path;
		SenSeeActClient client = new SenSeeActClient(baseUrl);
		List<AuthHeader> authHeaders = new ArrayList<>();
		request.getHeaderNames().asIterator().forEachRemaining(
				name -> {
					name = name.toLowerCase();
					if (!name.equals("authorization") && !name.startsWith("x-"))
						return;
					String value = request.getHeader(name);
					authHeaders.add(new AuthHeader(name, value));
				}
		);
		StringBuilder cookieStr = new StringBuilder();
		for (Cookie cookie : request.getCookies()) {
			if (!cookieStr.isEmpty())
				cookieStr.append("; ");
			cookieStr.append(cookie.getName());
			cookieStr.append("=");
			cookieStr.append(cookie.getValue());
		}
		if (!cookieStr.isEmpty())
			authHeaders.add(new AuthHeader("Cookie", cookieStr.toString()));
		client.setAuthHeaders(authHeaders);
		return client;
	}
}
