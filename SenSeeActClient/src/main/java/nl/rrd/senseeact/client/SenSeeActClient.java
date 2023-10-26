package nl.rrd.senseeact.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.SenSeeActClientException;
import nl.rrd.senseeact.client.model.*;
import nl.rrd.senseeact.client.model.SyncWatchResult.ResultCode;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.*;
import nl.rrd.senseeact.dao.sync.*;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.beans.PropertyReader;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpClient;
import nl.rrd.utils.http.HttpClientException;
import org.slf4j.Logger;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The SenSeeAct client can communicate with the SenSeeAct web service. After
 * construction you should call {@link
 * #login(String, String, Integer, boolean, boolean) login()}, {@link
 * #loginUsername(String, String, Integer, boolean, boolean) loginUsername()},
 * {@link #signup(String, String, Integer, boolean, boolean, String) signup()}
 * or {@link #setAuthHeaders(List) setAuthHeaders()}. After that you can run
 * other queries.
 * 
 * <p>When the service returns an error, this class will throw a {@link
 * SenSeeActClientException SenSeeActClientException}. General error codes that
 * may occur:</p>
 * 
 * <p><ul>
 * <li>{@link ErrorCode#AUTH_TOKEN_NOT_FOUND AUTH_TOKEN_NOT_FOUND}: If you try
 * to run a query that requires authentication and you didn't log in first.</li>
 * <li>{@link ErrorCode#ACCOUNT_INACTIVE ACCOUNT_INACTIVE}: If the user account
 * has been deactivated.</li>
 * <li>{@link ErrorCode#INVALID_INPUT INVALID_INPUT}: If some of the user input
 * is invalid.</li>
 * </ul></p>
 * 
 * <p>All methods except {@link #close() close()} must be called on the same
 * thread.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class SenSeeActClient {
	public static final String DEFAULT_AUTH_HEADER = "X-Auth-Token";

	public static final String PROTOCOL_VERSION = "6.0.7";
	public static final String SYNC_REMOTE_ID = "remote";
	private static final int MAX_ACTION_LOG = 10;
	
	private Logger logger;
	
	private final Object lock = new Object();
	private boolean closed = false;
	private List<HttpClient> activeClients = new ArrayList<>();
	private String baseUrl;
	
	// the following variable is set after login(), loginUsername(), signup()
	// and setAuthHeaders()
	private List<AuthHeader> authHeaders = null;

	private Map<String,String> responseHeaders = new LinkedHashMap<>();
	
	/**
	 * Constructs a new SenSeeAct client.
	 * 
	 * @param baseUrl the base URL of the SenSeeAct service, e.g.
	 * https://www.example.com/servlets/senseeact. Any trailing slashes are
	 * discarded.
	 */
	public SenSeeActClient(String baseUrl) {
		logger = AppComponents.getLogger(getClass().getSimpleName());
		this.baseUrl = baseUrl.replaceAll("/+$", "");
	}

	/**
	 * Returns the response headers that were returned at the latest query.
	 *
	 * @return the response headers
	 */
	public Map<String, String> getResponseHeaders() {
		return responseHeaders;
	}

	/**
	 * Closes this client. Any running queries will throw an IOException.
	 */
	public void close() {
		synchronized (lock) {
			if (closed)
				return;
			closed = true;
			for (HttpClient client : activeClients) {
				client.close();
			}
		}
	}

	/**
	 * Logs in to SenSeeAct. The returned token will expire after {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION} minutes. The service
	 * may return the following error codes:
	 *
	 * <p><ul>
	 * <li>{@link ErrorCode#INVALID_CREDENTIALS INVALID_CREDENTIALS}:
	 * If the email or password is invalid.</li>
	 * <li>{@link ErrorCode#ACCOUNT_INACTIVE ACCOUNT_INACTIVE}: If the account
	 * has been deactivated.</li>
	 * <li>{@link ErrorCode#ACCOUNT_BLOCKED ACCOUNT_BLOCKED}:
	 * If at least 10 subsequent logins failed for this user. The account will
	 * be blocked for 60 seconds.</li>
	 * </ul></p>
	 *
	 * @param email the email address
	 * @param password the password
	 * @return the authentication token
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TokenResult login(String email, String password)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return doLogin(email, password, LoginParams.DEFAULT_EXPIRATION, false,
				false);
	}

	/**
	 * Logs in to SenSeeAct. The service may return the following error codes:
	 * 
	 * <p><ul>
	 * <li>{@link ErrorCode#INVALID_CREDENTIALS INVALID_CREDENTIALS}:
	 * If the email or password is invalid.</li>
	 * <li>{@link ErrorCode#ACCOUNT_INACTIVE ACCOUNT_INACTIVE}: If the account
	 * has been deactivated.</li>
	 * <li>{@link ErrorCode#ACCOUNT_BLOCKED ACCOUNT_BLOCKED}:
	 * If at least 10 subsequent logins failed for this user. The account will
	 * be blocked for 60 seconds.</li>
	 * </ul></p>
	 * 
	 * @param email the email address
	 * @param password the password
	 * @param tokenExpiration the token expiration in minutes, or null if the
	 * token should never expire. You may use {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION}.
	 * @param cookie if the "authToken" cookie should be set
	 * @param autoExtendCookie true if the "authToken" cookie should be
	 * automatically extended at every verification
	 * @return the authentication token
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TokenResult login(String email, String password,
			Integer tokenExpiration, boolean cookie, boolean autoExtendCookie)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return doLogin(email, password, tokenExpiration, cookie,
				autoExtendCookie);
	}

	/**
	 * Runs the login query and returns the authentication token.
	 * 
	 * @param email the email address
	 * @param password the password
	 * @param tokenExpiration the token expiration in minutes, or null if the
	 * token should never expire. You may use {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION}.
	 * @param cookie if the "authToken" cookie should be set
	 * @param autoExtendCookie true if the "authToken" cookie should be
	 * automatically extended at every verification
	 * @return the authentication token
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	private TokenResult doLogin(String email, String password,
			Integer tokenExpiration, boolean cookie, boolean autoExtendCookie)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		LoginParams params = new LoginParams();
		params.setEmail(email);
		params.setPassword(password);
		params.setTokenExpiration(tokenExpiration);
		params.setCookie(cookie);
		params.setAutoExtendCookie(autoExtendCookie);
		TokenResult result = runQuery("/auth/login", "POST", false,
				client -> client.writeJson(params)
						.readJson(TokenResult.class));
		this.authHeaders = List.of(new AuthHeader(DEFAULT_AUTH_HEADER,
				result.getToken()));
		return result;
	}

	/**
	 * Logs in to SenSeeAct with a username. This can be the email address or
	 * just the local part of the email address. Logging in with the local part
	 * is only possible for certain domains and the local part must be
	 * unambiguous across the accepted domains.
	 *
	 * <p>The returned token will expire after {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION} minutes.</p>
	 *
	 * <p>The service may return the following error codes:</p>
	 *
	 * <p><ul>
	 * <li>{@link ErrorCode#INVALID_CREDENTIALS INVALID_CREDENTIALS}:
	 * If the email/username or password is invalid, or if there are more users
	 * with the same username in different domains.</li>
	 * <li>{@link ErrorCode#ACCOUNT_INACTIVE ACCOUNT_INACTIVE}: If the account
	 * has been deactivated.</li>
	 * <li>{@link ErrorCode#ACCOUNT_BLOCKED ACCOUNT_BLOCKED}:
	 * If at least 10 subsequent logins failed for this user. The account will
	 * be blocked for 60 seconds.</li>
	 * </ul></p>
	 *
	 * @param username the username
	 * @param password the password
	 * @return the email address and authentication token
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public LoginUsernameResult loginUsername(String username, String password)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return loginUsername(username, password,
				LoginParams.DEFAULT_EXPIRATION, false, false);
	}

	/**
	 * Logs in to SenSeeAct with a username. This can be the email address or
	 * just the local part of the email address. Logging in with the local part
	 * is only possible for certain domains and the local part must be
	 * unambiguous across the accepted domains.
	 * 
	 * <p>The service may return the following error codes:</p>
	 * 
	 * <p><ul>
	 * <li>{@link ErrorCode#INVALID_CREDENTIALS INVALID_CREDENTIALS}:
	 * If the email/username or password is invalid, or if there are more users
	 * with the same username in different domains.</li>
	 * <li>{@link ErrorCode#ACCOUNT_INACTIVE ACCOUNT_INACTIVE}: If the account
	 * has been deactivated.</li>
	 * <li>{@link ErrorCode#ACCOUNT_BLOCKED ACCOUNT_BLOCKED}:
	 * If at least 10 subsequent logins failed for this user. The account will
	 * be blocked for 60 seconds.</li>
	 * </ul></p>
	 * 
	 * @param username the username
	 * @param password the password
	 * @param tokenExpiration the token expiration in minutes, or null if the
	 * token should never expire. You may use {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION}.
	 * @param cookie if the "authToken" cookie should be set
	 * @param autoExtendCookie true if the "authToken" cookie should be
	 * automatically extended at every verification
	 * @return the email address and authentication token
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public LoginUsernameResult loginUsername(String username, String password,
			Integer tokenExpiration, boolean cookie, boolean autoExtendCookie)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		LoginUsernameParams params = new LoginUsernameParams();
		params.setUsername(username);
		params.setPassword(password);
		params.setTokenExpiration(tokenExpiration);
		params.setCookie(cookie);
		params.setAutoExtendCookie(autoExtendCookie);
		LoginUsernameResult loginResult = runQuery("/auth/login-username",
				"POST", false,
				client -> client.writeJson(params)
						.readJson(LoginUsernameResult.class));
		this.authHeaders = List.of(new AuthHeader(DEFAULT_AUTH_HEADER,
				loginResult.getToken()));
		return loginResult;
	}
	
	/**
	 * Sets authentication headers directly instead of running a login or
	 * signup. A header can be X-Auth-Token or a single sign-on header for a
	 * project like X-Project-Auth-Token.
	 * 
	 * @param authHeaders the authentication headers
	 */
	public void setAuthHeaders(List<AuthHeader> authHeaders) {
		this.authHeaders = authHeaders;
	}

	public String getDefaultAuthHeader() {
		return getAuthHeader(DEFAULT_AUTH_HEADER);
	}

	public String getAuthHeader(String name) {
		if (authHeaders == null)
			return null;
		for (AuthHeader header : authHeaders) {
			if (header.getName().equalsIgnoreCase(name))
				return header.getValue();
		}
		return null;
	}

	/**
	 * Logs in as another user. This can only be called by admins. You can't
	 * use this method to log in as yourself.
	 * 
	 * <p>The service may return the following error code:</p>
	 * 
	 * <p><ul>
	 * <li>{@link ErrorCode#ACCOUNT_INACTIVE ACCOUNT_INACTIVE}: If the account
	 * of the specified user has been deactivated.</li>
	 * </ul></p>
	 * 
	 * @param user user ID of the user as whom you want to log in
	 * @return the authentication token
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TokenResult loginAs(final String user) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		TokenResult result = runQuery("/auth/login-as", "GET", true,
			client -> client.addQueryParam("user", user)
					.readJson(TokenResult.class));
		this.authHeaders = List.of(new AuthHeader(DEFAULT_AUTH_HEADER,
				result.getToken()));
		return result;
	}

	/**
	 * Signs up a new user. It will try to create a user with role "patient".
	 * The user will not be added to a project.
	 *
	 * <p>The returned token will expire after {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION} minutes.</p>
	 *
	 * <p>The service may return the following error code:</p>
	 *
	 * <p><ul>
	 * <li>{@link ErrorCode#USER_ALREADY_EXISTS USER_ALREADY_EXISTS}:
	 * If a user with the same email already exists.</li>
	 * </ul></p>
	 *
	 * <p>If the signup succeeds, then the new user will be logged in at this
	 * client. Thus it overrides any previous login.</p>
	 *
	 * @param email the email address
	 * @param password the password (at least 6 characters)
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TokenResult signup(String email, String password)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return signup(email, password, LoginParams.DEFAULT_EXPIRATION, false,
				false, null);
	}

	/**
	 * Signs up a new user. It will try to create a user with role "patient"
	 * and add the user as a patient to the specified project. If you set the
	 * project to null, the user will not be added to a project.
	 *
	 * <p>The returned token will expire after {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION} minutes.</p>
	 *
	 * <p>The service may return the following error code:</p>
	 *
	 * <p><ul>
	 * <li>{@link ErrorCode#USER_ALREADY_EXISTS USER_ALREADY_EXISTS}:
	 * If a user with the same email already exists.</li>
	 * </ul></p>
	 *
	 * <p>If the signup succeeds, then the new user will be logged in at this
	 * client. Thus it overrides any previous login.</p>
	 *
	 * @param email the email address
	 * @param password the password (at least 6 characters)
	 * @param project the project code or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TokenResult signup(String email, String password, String project)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return signup(email, password, LoginParams.DEFAULT_EXPIRATION, false,
				false, project);
	}

	/**
	 * Signs up a new user. It will try to create a user with role "patient".
	 * The user will not be added to a project.
	 *
	 * <p>The service may return the following error code:</p>
	 *
	 * <p><ul>
	 * <li>{@link ErrorCode#USER_ALREADY_EXISTS USER_ALREADY_EXISTS}:
	 * If a user with the same email already exists.</li>
	 * </ul></p>
	 *
	 * <p>If the signup succeeds, then the new user will be logged in at this
	 * client. Thus it overrides any previous login.</p>
	 *
	 * @param email the email address
	 * @param password the password (at least 6 characters)
	 * @param tokenExpiration the token expiration in minutes, or null if the
	 * token should never expire. You may use {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION}.
	 * @param cookie if the "authToken" cookie should be set
	 * @param autoExtendCookie true if the "authToken" cookie should be
	 * automatically extended at every verification
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TokenResult signup(String email, String password,
			Integer tokenExpiration, boolean cookie, boolean autoExtendCookie)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return signup(email, password, tokenExpiration, cookie,
				autoExtendCookie, null);
	}

	/**
	 * Signs up a new user. It will try to create a user with role "patient"
	 * and add the user as a patient to the specified project. If you set the
	 * project to null, the user will not be added to a project.
	 * 
	 * <p>The service may return the following error code:</p>
	 * 
	 * <p><ul>
	 * <li>{@link ErrorCode#USER_ALREADY_EXISTS USER_ALREADY_EXISTS}:
	 * If a user with the same email already exists.</li>
	 * </ul></p>
	 * 
	 * <p>If the signup succeeds, then the new user will be logged in at this
	 * client. Thus it overrides any previous login.</p>
	 * 
	 * @param email the email address
	 * @param password the password (at least 6 characters)
	 * @param tokenExpiration the token expiration in minutes, or null if the
	 * token should never expire. You may use {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION}.
	 * @param cookie if the "authToken" cookie should be set
	 * @param autoExtendCookie true if the "authToken" cookie should be
	 * automatically extended at every verification
	 * @param project the project code or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TokenResult signup(String email, String password,
			Integer tokenExpiration, boolean cookie, boolean autoExtendCookie,
			String project) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		SignupParams params = new SignupParams();
		params.setEmail(email);
		params.setPassword(password);
		params.setTokenExpiration(tokenExpiration);
		params.setCookie(cookie);
		params.setAutoExtendCookie(autoExtendCookie);
		params.setProject(project);
		TokenResult result = runQuery("/auth/signup", "POST", false,
				client -> client.writeJson(params)
							.readJson(TokenResult.class));
		this.authHeaders = List.of(new AuthHeader(DEFAULT_AUTH_HEADER,
				result.getToken()));
		return result;
	}


	/**
	 * Signs up a new user. It will try to create a user with role "patient"
	 * and add the user as a patient to the specified project. If you set the
	 * project to null, the user will not be added to a project.
	 *
	 * <p>The service may return the following error code:</p>
	 *
	 * <p><ul>
	 * <li>{@link ErrorCode#USER_ALREADY_EXISTS USER_ALREADY_EXISTS}:
	 * If a user with the same email already exists.</li>
	 * </ul></p>
	 *
	 * <p>If the signup succeeds, then the new user will be logged in at this
	 * client. Thus it overrides any previous login.</p>
	 *
	 * @param email the email address
	 * @param password the password (at least 6 characters)
	 * @param tokenExpiration the token expiration in minutes, or null if the
	 * token should never expire. You may use {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION}.
	 * @param cookie if the "authToken" cookie should be set
	 * @param autoExtendCookie true if the "authToken" cookie should be
	 * automatically extended at every verification
	 * @param project the project code or null
	 * @param emailTemplate the name of a custom email template that should be
	 * used to send a new user mail. Set to null for the default email template.
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TokenResult signup(String email, String password,
			Integer tokenExpiration, boolean cookie, boolean autoExtendCookie,
			String project, String emailTemplate)
			throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		SignupParams params = new SignupParams();
		params.setEmail(email);
		params.setPassword(password);
		params.setTokenExpiration(tokenExpiration);
		params.setCookie(cookie);
		params.setAutoExtendCookie(autoExtendCookie);
		params.setProject(project);
		params.setEmailTemplate(emailTemplate);
		TokenResult result = runQuery("/auth/signup", "POST", false,
				client -> client.writeJson(params)
							.readJson(TokenResult.class));
		this.authHeaders = List.of(new AuthHeader(DEFAULT_AUTH_HEADER,
				result.getToken()));
		return result;
	}

	/**
	 * Creates a new temporary SenSeeAct user. The user will have role "patient"
	 * and will be added to the specified project. This endpoint creates a user
	 * with a random email address in domain "temp.senseeact.com" and a random
	 * password. It will be automatically deleted after 24 hours.
	 *
	 * <p>The user can be made permanent by changing the email address {@link
	 * #updateUser(String, User) updateUser()}. In that case you probably want
	 * to change the password as well ({@link
	 * #changePassword(String, String, String) changePassword()}.</p>
	 *
	 * <p>The returned token will expire after {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION} minutes.</p>
	 *
	 * <p>If the signup succeeds, then the new user will be logged in at this
	 * client. Thus it overrides any previous login.</p>
	 *
	 * @param project the project code or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TokenResult signupTemporaryUser(String project)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return signupTemporaryUser(LoginParams.DEFAULT_EXPIRATION, false, false,
				project);
	}

	/**
	 * Creates a new temporary SenSeeAct user. The user will have role "patient"
	 * and will be added to the specified project. This endpoint creates a user
	 * with a random email address in domain "temp.senseeact.com" and a random
	 * password. It will be automatically deleted after 24 hours.
	 *
	 * <p>The user can be made permanent by changing the email address {@link
	 * #updateUser(String, User) updateUser()}. In that case you probably want
	 * to change the password as well ({@link
	 * #changePassword(String, String, String) changePassword()}.</p>
	 *
	 * <p>If the signup succeeds, then the new user will be logged in at this
	 * client. Thus it overrides any previous login.</p>
	 *
	 * @param tokenExpiration the token expiration in minutes, or null if the
	 * token should never expire. You may use {@link
	 * LoginParams#DEFAULT_EXPIRATION DEFAULT_EXPIRATION}.
	 * @param cookie if the "authToken" cookie should be set
	 * @param autoExtendCookie true if the "authToken" cookie should be
	 * automatically extended at every verification
	 * @param project the project code or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TokenResult signupTemporaryUser(Integer tokenExpiration,
			boolean cookie, boolean autoExtendCookie, String project)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		SignupTemporaryUserParams params = new SignupTemporaryUserParams();
		params.setTokenExpiration(tokenExpiration);
		params.setCookie(cookie);
		params.setAutoExtendCookie(autoExtendCookie);
		params.setProject(project);
		TokenResult result = runQuery("/auth/signup-temporary-user", "POST",
				false,
				client -> client.writeJson(params)
						.readJson(TokenResult.class));
		this.authHeaders = List.of(new AuthHeader(DEFAULT_AUTH_HEADER,
				result.getToken()));
		return result;
	}

	/**
	 * Requests to send an email to verify the user's email address. This will
	 * be the same email that is sent when a user signs up. It includes a link
	 * to a webpage that calls {@link #verifyEmail(String, String)
	 * verifyEmail()}.
	 *
	 * @param template the name of a custom email template that should be used.
	 * Set to null for the default email template.
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void requestVerifyEmail(String template)
			throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		runQuery("/auth/request-verify-email", "GET", true,
				client -> {
					if (template != null && !template.isEmpty())
						client.addQueryParam("template", template);
					return client.readString();
				}
		);
	}

	/**
	 * Tries to verify an email. This should be called from a link that was
	 * sent to the user's email address.
	 *
	 * @param userId the user ID
	 * @param code the verification code
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void verifyEmail(String userId, String code)
			throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		runQuery("/auth/verify-email", "POST", false,
				client -> client.writePostParam("user", userId)
						.writePostParam("code", code)
						.readString()
		);
	}

	/**
	 * Changes the password of the specified user. If you are not an admin, you
	 * can only change your own password. If you are not an admin or a temporary
	 * user, you must specify your old password.
	 *
	 * <p>After changing your password, you should call {@link
	 * #login(String, String, Integer, boolean, boolean) login()} again to
	 * update the token.</p>
	 *
	 * @param email the email address, or null to change your own password
	 * @param oldPassword the old password or null (only if you are an admin or
	 * a temporary user)
	 * @param newPassword the new password
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void changePassword(String email, String oldPassword,
			String newPassword) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		runQuery("/auth/change-password", "POST", true,
			client -> {
				if (email != null)
					client.writePostParam("email", email);
				if (oldPassword != null)
					client.writePostParam("oldPassword", oldPassword);
				return client.writePostParam("newPassword", newPassword)
						.readString();
			}
		);
	}

	/**
	 * Sends a request to reset the password for a user. An email with a reset
	 * link will be sent to the email address of the user.
	 * 
	 * <p>You may specify a locale so SenSeeAct tries to send the email in the
	 * preferred language of the user. If you set it to null, it will use the
	 * default locale of the system.</p>
	 * 
	 * <p>The email will contain a reset link with a code. This code can be used
	 * to reset the password with {@link #resetPassword(String, String, String)
	 * resetPassword()}. The code will be valid for 24 hours.</p>
	 * 
	 * @param email the email address of the user
	 * @param locale the preferred locale or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void requestResetPassword(final String email, Locale locale)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		final Locale emailLocale = locale != null ?
				locale : Locale.getDefault();
		runQuery("/auth/request-reset-password", "GET", false,
				client -> client.addQueryParam("email", email)
						.addHeader("Accept-Language",
								emailLocale.getLanguage() + "-" +
								emailLocale.getCountry() + ", *;q=0.1")
						.readString());
	}
	
	/**
	 * Resets the password of a user. You should specify the code that was
	 * received in the email from {@link #requestResetPassword(String, Locale)
	 * requestResetPassword()}.
	 * 
	 * <p>If the specified reset code is incorrect or expired, you have to call
	 * {@link #requestResetPassword(String, Locale) requestResetPassword()}
	 * again. Any correct code is invalidated. Once the password has been reset,
	 * you cannot use the code again.</p>
	 * 
	 * @param email the email address of the user
	 * @param code the reset password code
	 * @param password the new password
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void resetPassword(final String email, final String code,
			final String password) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		runQuery("/auth/reset-password", "POST", false,
				client -> client.writePostParam("email", email)
						.writePostParam("code", code)
						.writePostParam("password", password)
						.readString());
	}

	/**
	 * Returns a list of all users with short info about each user. The users
	 * are sorted by email address. This can only be called by admins.
	 *
	 * @return the user list
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<ListUser> getUserList() throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return runQuery("/user/list", "GET", true,
				client -> client.readJson(new TypeReference<>() {}));
	}

	/**
	 * Returns the profile for the specified user.
	 * 
	 * @param user the user ID of the user, or null to get your own
	 * profile
	 * @return the user profile
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public User getUser(final String user) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return runQuery("/user/", "GET", true,
				client -> {
					if (user != null)
						client.addQueryParam("user", user);
					return client.readJson(User.class);
				});
	}
	
	/**
	 * Returns the profile for the user with the specified email address.
	 *
	 * @param email the email address of the user, or null to get your own
	 * profile
	 * @return the user profile
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public User getUserByEmail(final String email) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return runQuery("/user/", "GET", true,
				client -> {
					if (email != null)
						client.addQueryParam("email", email);
					return client.readJson(User.class);
				});
	}

	/**
	 * Updates the profile for the specified user. You can't change the
	 * following fields:
	 *
	 * <p><ul>
	 * <li>{@link User#getUserid() userid}</li>
	 * <li>{@link User#isEmailVerified() emailVerified}</li>
	 * <li>{@link User#getEmailPendingVerification() emailPendingVerification}</li>
	 * <li>{@link User#isHasTemporaryEmail() hasTemporaryEmail}</li>
	 * <li>{@link User#isHasTemporaryPassword() hasTemporaryPassword}</li>
	 * <li>{@link User#getRole() role}</li>
	 * <li>{@link User#isActive() active}</li>
	 * <li>{@link User#getCreated() created}</li>
	 * <li>{@link User#getLastActive() lastActive}</li>
	 * </ul></p>
	 *
	 * <p>If you want to change only some fields and leave other fields
	 * unaffected, you can call {@link #updateUser(String, Map)
	 * updateUser(String email, Map&lt;String,?&gt; userData)}.</p>
	 * 
	 * @param user the user ID of the user whose profile you want to
	 * change, or null to change your own profile
	 * @param profile the new user profile
	 * @return the new user object
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public User updateUser(final String user, final User profile)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery("/user/", "PUT", true,
				client -> {
					if (user != null)
						client.addQueryParam("user", user);
					return client.writeJson(profile)
						.readJson(User.class);
				});
	}

	/**
	 * Updates the profile for the specified user. You can't change the
	 * following fields:
	 *
	 * <p><ul>
	 * <li>{@link User#getUserid() userid}</li>
	 * <li>{@link User#isEmailVerified() emailVerified}</li>
	 * <li>{@link User#getEmailPendingVerification() emailPendingVerification}</li>
	 * <li>{@link User#isHasTemporaryEmail() hasTemporaryEmail}</li>
	 * <li>{@link User#isHasTemporaryPassword() hasTemporaryPassword}</li>
	 * <li>{@link User#getRole() role}</li>
	 * <li>{@link User#isActive() active}</li>
	 * <li>{@link User#getCreated() created}</li>
	 * <li>{@link User#getLastActive() lastActive}</li>
	 * </ul></p>
	 *
	 * <p>If you want to change only some fields and leave other fields
	 * unaffected, you can call {@link #updateUser(String, Map)
	 * updateUser(String email, Map&lt;String,?&gt; userData)}.</p>
	 *
	 * @param user the user ID of the user whose profile you want to
	 * change, or null to change your own profile
	 * @param profile the new user profile
	 * @param emailTemplate the name of a custom email template that should be
	 * used to send a mail if the user changes the email address. Set to null
	 * for the default email template.
	 * @return the new user object
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public User updateUser(String user, User profile, String emailTemplate)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery("/user/", "PUT", true,
				client -> {
					if (user != null)
						client.addQueryParam("user", user);
					if (emailTemplate != null)
						client.addQueryParam("emailTemplate", emailTemplate);
					return client.writeJson(profile)
						.readJson(User.class);
				});
	}

	/**
	 * Updates the profile for the specified user. You can't change the
	 * following fields:
	 *
	 * <p><ul>
	 * <li>{@link User#getUserid() userid}</li>
	 * <li>{@link User#isEmailVerified() emailVerified}</li>
	 * <li>{@link User#getEmailPendingVerification() emailPendingVerification}</li>
	 * <li>{@link User#isHasTemporaryEmail() hasTemporaryEmail}</li>
	 * <li>{@link User#isHasTemporaryPassword() hasTemporaryPassword}</li>
	 * <li>{@link User#getRole() role}</li>
	 * <li>{@link User#isActive() active}</li>
	 * <li>{@link User#getCreated() created}</li>
	 * <li>{@link User#getLastActive() lastActive}</li>
	 * </ul></p>
	 *
	 * <p>The map should contain the fields that you want to change. The values
	 * should be serializable to JSON. Any fields that you don't specify, will
	 * remain unchanged.</p>
	 * 
	 * @param user the user ID of the user whose profile you want to
	 * change, or null to change your own profile
	 * @param userData the new user data
	 * @return the new user object
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public User updateUser(final String user, final Map<String,?> userData)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery("/user/", "PUT", true,
				client -> {
					if (user != null)
						client.addQueryParam("user", user);
					return client.writeJson(userData)
						.readJson(User.class);
				});
	}

	/**
	 * Updates the profile for the specified user. You can't change the
	 * following fields:
	 *
	 * <p><ul>
	 * <li>{@link User#getUserid() userid}</li>
	 * <li>{@link User#isEmailVerified() emailVerified}</li>
	 * <li>{@link User#getEmailPendingVerification() emailPendingVerification}</li>
	 * <li>{@link User#isHasTemporaryEmail() hasTemporaryEmail}</li>
	 * <li>{@link User#isHasTemporaryPassword() hasTemporaryPassword}</li>
	 * <li>{@link User#getRole() role}</li>
	 * <li>{@link User#isActive() active}</li>
	 * <li>{@link User#getCreated() created}</li>
	 * <li>{@link User#getLastActive() lastActive}</li>
	 * </ul></p>
	 *
	 * <p>The map should contain the fields that you want to change. The values
	 * should be serializable to JSON. Any fields that you don't specify, will
	 * remain unchanged.</p>
	 *
	 * @param user the user ID of the user whose profile you want to
	 * change, or null to change your own profile
	 * @param userData the new user data
	 * @param emailTemplate the name of a custom email template that should be
	 * used to send a mail if the user changes the email address. Set to null
	 * for the default email template.
	 * @return the new user object
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public User updateUser(final String user, final Map<String,?> userData,
			String emailTemplate) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return runQuery("/user/", "PUT", true,
				client -> {
					if (user != null)
						client.addQueryParam("user", user);
					if (emailTemplate != null)
						client.addQueryParam("emailTemplate", emailTemplate);
					return client.writeJson(userData)
						.readJson(User.class);
				});
	}

	/**
	 * Completely deletes a user and all related data from all projects. All
	 * users can delete themselves. Patients and professionals can only delete
	 * themselves. Admins can delete any user. If you run this query as an admin
	 * and the specified user doesn't exist, this method has no effect.
	 * 
	 * @param user the user ID of the user to delete
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteUser(final String user) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		runQuery("/user/", "DELETE", true,
				client -> client.addQueryParam("user", user).readString());
	}

	/**
	 * Returns the names of all groups where the specified user is a member.
	 * Only admins can get the groups of other users.
	 * 
	 * @param user the user ID of the user, or null to get your own groups
	 * @return the names of the groups
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<String> getUserGroups(final String user)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery("/user/groups", "GET", true,
				client -> {
					if (user != null)
						client.addQueryParam("user", user);
					return client.readJson(new TypeReference<>() {});
				});
	}
	
	/**
	 * Creates the group with the specified name and members. The group name
	 * must be formatted as an email address. Only admins can run this query.
	 * 
	 * @param name the group name (formatted as email address)
	 * @param members user IDs of users to add to the group
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void createGroup(String name, String... members)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		createGroup(name, Arrays.asList(members));
	}
	
	/**
	 * Creates the group with the specified name and members. The group name
	 * must be formatted as an email address. Only admins can run this query.
	 * 
	 * <p>If a group with the specified name already exists, this method
	 * returns error code {@link ErrorCode#GROUP_ALREADY_EXISTS
	 * GROUP_ALREADY_EXISTS}.</p>
	 * 
	 * @param name the group name (formatted as email address)
	 * @param members user IDs of users to add to the group (can be null or
	 * empty)
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void createGroup(final String name, final List<String> members)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery("/group/", "POST", true,
				client -> {
					client.addQueryParam("name", name);
					if (members != null && !members.isEmpty()) {
						Map<String,Object> params = new LinkedHashMap<>();
						params.put("members", members);
						client.writeJson(params);
					}
					client.readString();
					return null;
				});
	}
	
	/**
	 * Returns the group and its members for the specified group name. Admins
	 * can call this method for any group. Other users must be a member of the
	 * group. The returned members are only users that you can access. That is
	 * all users in the group with a role equal or lower than your own role.
	 * So admins get all members; professionals only get other professionals
	 * and patients; patients only get patients.
	 * 
	 * @param name the group name (formatted as email address)
	 * @param includeInactiveMembers true if you want to include inactive
	 * members, false if you only want to get active members
	 * @return the group and its members
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public Group getGroup(final String name,
			final boolean includeInactiveMembers) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return runQuery("/group/", "GET", true,
				client -> client.addQueryParam("name", name)
						.addQueryParam("includeInactiveMembers",
								Boolean.toString(includeInactiveMembers))
						.readJson(Group.class));
	}
	
	/**
	 * Deletes the group with the specified name. Only admins can run this
	 * query.
	 * 
	 * @param name the group name (formatted as email address)
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteGroup(final String name) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		runQuery("/group/", "DELETE", true,
				client -> client.addQueryParam("name", name).readString());
	}
	
	/**
	 * Adds a member to the specified group. If the user is already a member,
	 * then this query has no effect. Only admins can run this query.
	 * 
	 * @param group the group name (formatted as email address)
	 * @param member the user ID of the member
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void addGroupMember(final String group, final String member)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery("/group/member", "POST", true,
				client -> client.addQueryParam("group", group)
						.addQueryParam("member", member)
						.readString());
	}
	
	/**
	 * Deletes a member from the specified group. If the user is not a member,
	 * then this query has no effect. Only admins can run this query.
	 * 
	 * @param group the group name (formatted as email address)
	 * @param member the user ID of the member
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteGroupMember(final String group, final String member)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery("/group/member", "DELETE", true,
				client -> client.addQueryParam("group", group)
					.addQueryParam("member", member)
					.readString());
	}

	/**
	 * Sets the role of another user. Only admins can run this query. You can
	 * only set the role of another user.
	 * 
	 * @param user user ID of the user whose role should be set
	 * @param role the role
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void setRole(final String user, final Role role)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery("/user/role", "PUT", true,
				client -> client.addQueryParam("user", user)
						.addQueryParam("role", role.toString())
						.readString());
	}
	
	/**
	 * Activates or deactivates a user. An inactive user cannot log in or call
	 * any queries that require authentication. Only admins can run this query.
	 * You can only activate or deactivate another user.
	 * 
	 * @param user user ID of the user that should be activated or deactivated
	 * @param active true if the user should be active, false if the user should
	 * be inactive
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void setUserActive(final String user, final boolean active)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery("/user/active", "PUT", true,
				client -> client.addQueryParam("user", user)
						.addQueryParam("active", Boolean.toString(active))
						.readString());
	}

	/**
	 * Returns the projects that the current user can access.
	 * 
	 * @return the projects
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<Project> getProjectList() throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return runQuery("/project/list", "GET", true,
				client -> client.readJson(new TypeReference<>() {}));
	}
	
	/**
	 * Returns the project codes of all projects, including those that the
	 * current user is not a member of. Users can add themselves to any project.
	 * 
	 * @return the project codes
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<String> getAllProjectList() throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return runQuery("/project/list/all", "GET", true,
				client -> client.readJson(new TypeReference<>() {}));
	}
	
	/**
	 * Checks whether the current user can access the specified project and the
	 * system is working correctly. If you specify a subject, this method will
	 * also check whether the current user can access the subject.
	 * 
	 * @param project the project code
	 * @param subject user ID of the subject user or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void checkProject(String project, final String subject)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery(String.format("/project/%s/check", project), "GET", true,
				client -> {
					if (subject != null)
						client.addQueryParam("user", subject);
					client.readString();
					return null;
				});
	}

	/**
	 * Returns all users that you can access within a project. If you specify a
	 * role, this method returns users that have been added to the project with
	 * that role. If you are an admin, you will get all users. If you are a
	 * professional, you will only get yourself and subjects to which you were
	 * granted access with {@link #addSubject(String, String) addSubject()}. If
	 * you are a patient, you will only get yourself.
	 * 
	 * <p>If you are an admin, you can also get the accessible users for another
	 * user with {@link #getProjectUsers(String, String, Role, boolean)
	 * getProjectUsers(project, user, role, includeInactive)}.</p>
	 * 
	 * @param project the project code
	 * @param role the role or null
	 * @param includeInactive true if you want to include inactive users, false
	 * if you only want to get active users
	 * @return the users
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<User> getProjectUsers(String project, Role role,
			boolean includeInactive) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return getProjectUsers(project, null, role, includeInactive);
	}

	/**
	 * Returns all users that you or the specified user can access within a
	 * project. If you are not an admin, you can only get the accessible users
	 * for yourself (set "user" to null or yourself). If you specify a role,
	 * this method returns users that have been added to the project with that
	 * role.
	 * 
	 * <p>If you get the users for an admin, you will get all users. For a
	 * professional you will only get the professional and subjects to which the
	 * professional was granted access with {@link #addSubject(String, String)
	 * addSubject()}. For a patient you will only get the patient itself.</p>
	 * 
	 * @param project the project code
	 * @param user the user ID of the user whose accessible users you want to
	 * get. Pass null to get your own accessible users.
	 * @param role the role or null
	 * @param includeInactive true if you want to include inactive users, false
	 * if you only want to get active users
	 * @return the users
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<User> getProjectUsers(String project, final String user,
			final Role role, final boolean includeInactive)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery(String.format("/project/%s/users", project), "GET",
				true,
				client -> {
					if (user != null)
						client.addQueryParam("user", user);
					if (role != null)
						client.addQueryParam("role", role.toString());
					client.addQueryParam("includeInactive", Boolean.toString(
							includeInactive));
					return client.readJson(new TypeReference<>() {});
				});
	}
	
	/**
	 * Adds a user to a project. All users can add themselves to any project.
	 * Admins can add any user. Professionals can add any user that they can
	 * access. Patients can only add themselves.
	 * 
	 * @param user the user ID of the user to add or null to add yourself
	 * @param project the project code
	 * @param asRole the role as which the user should be added to the project
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void addUserToProject(final String user, String project,
			final Role asRole) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		runQuery(String.format("/project/%s/user", project), "POST",
				true,
				client -> {
					if (user != null)
						client.writePostParam("user", user);
					client.writePostParam("asRole", asRole.toString())
							.readString();
					return null;
				});
	}
	
	/**
	 * Removes a user from a project. If you specify a role, the user will only
	 * be removed for that role and it may remain in the project with other
	 * roles. If you don't specify a role, the user will be removed completely,
	 * so the user can no longer access the project.
	 * 
	 * <p>All users can remove themselves from any project. Admins can remove
	 * any user. Professionals can remove any user that they can access.
	 * Patients can only remove themselves.</p>
	 * 
	 * @param user the user ID of the user to remove or null to remove yourself
	 * @param project the project code
	 * @param asRole the role or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void removeUserFromProject(final String user, String project,
			final Role asRole) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		runQuery(String.format("/project/%s/user", project), "DELETE",
				true,
				client -> {
					if (user != null)
						client.addQueryParam("user", user);
					if (asRole != null)
						client.addQueryParam("asRole", asRole.toString());
					client.readString();
					return null;
				});
	}

	/**
	 * Returns the access rules for all grantees for the specified subject
	 * within a project.
	 *
	 * <p>All users can get the grantees for themselves. Admins can get the
	 * grantees of any user. Professionals can get the grantees of any user that
	 * they can access. Patients can only get the grantees of themselves.</p>
	 *
	 * @param project the project code
	 * @param subject the user ID of the subject or null to get the grantees for
	 * yourself
	 * @return the access rules for the grantees
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<ProjectUserAccessRule> getProjectUserAccessGrantees(
			String project, String subject) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return runQuery(String.format("/access/project/%s/grantee/list",
				project), "GET", true,
				client -> {
					if (subject != null)
						client.addQueryParam("subject", subject);
					return client.readJson(new TypeReference<>() {});
				});
	}

	/**
	 * Returns the access rules for all subjects for the specified grantee
	 * within a project.
	 *
	 * <p>All users can get the subjects for themselves. Admins can get the
	 * subjects of any user. Professionals can get the subjects of any user that
	 * they can access. Patients can only get the subjects of themselves.</p>
	 *
	 * @param project the project code
	 * @param grantee the user ID of the grantee or null to get the subjects for
	 * yourself
	 * @return the access rules for the subjects
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<ProjectUserAccessRule> getProjectUserAccessSubjects(
			String project, String grantee) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return runQuery(String.format("/access/project/%s/subject/list",
				project), "GET", true,
				client -> {
					if (grantee != null)
						client.addQueryParam("grantee", grantee);
					return client.readJson(new TypeReference<>() {});
				});
	}

	/**
	 * Grants access to grantee to data of the specified subject within a
	 * project.
	 *
	 * <p>All users can grant access to data of themselves. Admins can grant
	 * access to data of any user. Professionals can grant access to data of
	 * any user that they can access. Patients can only grant access to their
	 * own data.</p>
	 *
	 * @param project the project code
	 * @param granteeEmail the email address of the grantee
	 * @param subject the user ID of the subject or null for yourself
	 * @param accessRestriction the access restriction. This can be null to
	 * grant full access. Otherwise it should be a list with at least one
	 * restriction.
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void setProjectUserAccess(String project, String granteeEmail,
			String subject,
			List<ProjectUserAccessRestriction> accessRestriction)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery(String.format("/access/project/%s", project), "POST", true,
				client -> {
					client.addQueryParam("granteeEmail", granteeEmail);
					if (subject != null)
						client.addQueryParam("subject", subject);
					ProjectUserAccessRule rule = new ProjectUserAccessRule();
					rule.setAccessRestriction(accessRestriction);
					client.writeJson(rule).readString();
					return null;
				});
	}

	/**
	 * Revokes access to grantee to data of the specified subject within a
	 * project.
	 *
	 * <p>All users can revoke access to data of themselves. Admins can revoke
	 * access to data of any user. Professionals can revoke access to data of
	 * any user that they can access. Patients can only revoke access to their
	 * own data.</p>
	 *
	 * @param project the project code
	 * @param grantee the user ID of the grantee
	 * @param subject the user ID of the subject or null for yourself
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteProjectUserAccess(String project, String grantee,
			String subject) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		runQuery(String.format("/access/project/%s", project), "DELETE", true,
				client -> {
					client.addQueryParam("grantee", grantee);
					if (subject != null)
						client.addQueryParam("subject", subject);
					client.readString();
					return null;
				});
	}

	/**
	 * Adds a patient subject to a professional user. This means that the
	 * professional is granted access to the patient. Only admins can run this
	 * query.
	 *
	 * @param user user ID of the professional. This must be a user with role
	 * {@link Role#PROFESSIONAL PROFESSIONAL}.
	 * @param subject user ID of the patient. This must be a user with role
	 * {@link Role#PATIENT PATIENT}.
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void addSubject(final String user, final String subject)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery("/access/subject", "POST", true,
				client -> client.addQueryParam("user", user)
						.addQueryParam("subject", subject)
						.readString());
	}

	/**
	 * Removes a patient subject from a professional user. This revokes access
	 * for the professional to the patient. Only admins can run this query.
	 *
	 * @param user user ID of the professional. This must be a user with role
	 * {@link Role#PROFESSIONAL PROFESSIONAL}.
	 * @param subject user ID of the patient. This must be a user with role
	 * {@link Role#PATIENT PATIENT}.
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void removeSubject(final String user, final String subject)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery("/access/subject", "DELETE", true,
				client -> client.addQueryParam("user", user)
						.addQueryParam("subject", subject)
						.readString());
	}

	/**
	 * Adds a registration to watch subjects that you can access within the
	 * specified project.
	 * 
	 * <p>This method returns a registration ID that you can pass to {@link
	 * #watchSubjects(String, String) watchSubjects()}. If a registration with
	 * the same parameters already exists, then this method returns the ID of
	 * that registration. Any client that was watching that registration
	 * will be cancelled.</p>
	 * 
	 * <p>If you no longer want to watch the table, you should call {@link
	 * #unregisterWatchSubjects(String, String) unregisterWatchSubjects()}.
	 * Note that the server may automatically clean up registrations that
	 * haven't been watched for an hour.</p>
	 * 
	 * @param project the project code
	 * @param reset true if any pending events in an existing registration
	 * should be cleared, false otherwise
	 * @return the registration ID
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public String registerWatchSubjects(String project, final boolean reset)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery(String.format("/project/%s/subjects/watch/register",
				project), "POST", true,
				client -> client.addQueryParam("reset", Boolean.toString(reset))
						.readString());
	}
	
	/**
	 * Waits up to one minute until a subject event occurs for the specified
	 * watch registration or until the watch is unregistered. You receive events
	 * when a subject is added or removed, or when the user profile of a subject
	 * is changed.
	 * 
	 * <p>As soon as there are new events, this method returns those events.
	 * This may include events that occurred since the registration or last
	 * watch and that you haven't received yet. If no events occur before the
	 * time-out, this method returns an empty list.</p>
	 * 
	 * <p>Only one client can watch a specific registration. If another client
	 * registers or starts to watch the same registration, then the current
	 * client will be cancelled. This means that this method will return
	 * immediately with an empty list result. Likewise, when you call this
	 * method, any other clients that were watching the same registration, will
	 * be cancelled.</p>
	 * 
	 * <p>If you disconnect the client during a watch, the server may still send
	 * an event and think that you received it. If you start a new session and
	 * don't want to miss any events, it's best to run a read query after the
	 * watch registration.</p>
	 * 
	 * <p>You should specify a registration ID that you got from {@link
	 * #registerWatchSubjects(String, boolean) registerWatchSubjects()}. If you
	 * no longer want to watch the subjects, you should call {@link
	 * #unregisterWatchSubjects(String, String) unregisterWatchSubjects()}. Note
	 * that the server may automatically clean up registrations that haven't
	 * been watched for an hour.</p>
	 * 
	 * @param project the project code
	 * @param regId the registration ID
	 * @return new subject events, or an empty list if no events occurred before
	 * the time-out
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<SubjectEvent> watchSubjects(String project, String regId)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery(String.format("/project/%s/subjects/watch/%s",
				project, regId), "GET", true,
				client -> {
					List<?> list = client.readJson(List.class);
					List<SubjectEvent> result = new ArrayList<>();
					for (Object obj : list) {
						result.add(SubjectEvent.parse(obj));
					}
					return result;
				});
	}
	
	/**
	 * Removes a registration that was added with {@link
	 * #registerWatchSubjects(String, boolean) registerWatchSubjects()}. Any
	 * client that was watching this registration, will be cancelled. If the
	 * registration doesn't exist, this method has no effect.
	 * 
	 * @param project the project code
	 * @param regId the registration ID
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void unregisterWatchSubjects(String project, String regId)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery(String.format("/project/%s/subjects/watch/unregister/%s",
				project, regId), "POST", true,
				HttpClient::readString);
	}
	
	/**
	 * Returns the names of all tables in the specified project database.
	 * 
	 * @param project the project code
	 * @return the names of the tables
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<String> getTables(String project)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery(String.format("/project/%s/tables", project),
				"GET", true,
				client -> client.readJson(new TypeReference<>() {}));
	}

	/**
	 * Returns the specification of the specified table.
	 * 
	 * @param project the project code
	 * @param table the name of the table
	 * @return the table specification
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public TableSpec getTableSpec(String project, String table)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery(String.format("/project/%s/table/%s/spec",
				project, table), "GET", true,
				client -> client.readJson(TableSpec.class));
	}

	/**
	 * Reads records from a table within a project. You can filter by user.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>The returned records are sorted by utcTime, localTime or id, depending
	 * on what fields are available.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getRecords(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getRecords(String, String, String, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and UTC time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and local time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and date, apply custom filter, sort and limit</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the records
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> List<T> getRecords(String project,
			String table, String subject, Class<T> dataClass)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return doGetRecords(project, table, subject, null, null, null, null, 0,
				dataClass);
	}

	/**
	 * Reads records from a table within a project. You can filter by user and
	 * UTC time.
	 * 
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 * 
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 * 
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 * 
	 * <p>The returned records are sorted by utcTime, localTime or id, depending
	 * on what fields are available.</p>
	 * 
	 * <p>Related methods:</p>
	 * 
	 * <p>{@link #getRecords(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getRecords(String, String, String, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and UTC time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and local time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and date, apply custom filter, sort and limit</p>
	 * 
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the records
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> List<T> getRecords(String project,
			String table, String subject, ZonedDateTime start,
			ZonedDateTime end, Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetRecords(project, table, subject, start, end, null, null, 0,
				dataClass);
	}

	/**
	 * Reads records from a table within a project. You can filter by user and
	 * local time.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>The returned records are sorted by utcTime, localTime or id, depending
	 * on what fields are available.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getRecords(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getRecords(String, String, String, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and UTC time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and local time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and date, apply custom filter, sort and limit</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the records
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> List<T> getRecords(String project,
			String table, String subject, LocalDateTime start,
			LocalDateTime end, Class<T> dataClass)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return doGetRecords(project, table, subject, start, end, null, null, 0,
				dataClass);
	}

	/**
	 * Reads records from a table within a project. You can filter by user and
	 * date.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end date. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>The returned records are sorted by utcTime, localTime or id, depending
	 * on what fields are available.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getRecords(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getRecords(String, String, String, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and UTC time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and local time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and date, apply custom filter, sort and limit</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start date or null
	 * @param end the end date or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the records
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> List<T> getRecords(String project,
			String table, String subject, LocalDate start, LocalDate end,
			Class<T> dataClass) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		return doGetRecords(project, table, subject, start, end, null, null, 0,
				dataClass);
	}

	/**
	 * Reads records from a table within a project. You can filter by user and
	 * apply a custom filter, sort and limit.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>If you don't specify a custom sort, then the returned records are
	 * sorted by utcTime, localTime or id, depending on what fields are
	 * available.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getRecords(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getRecords(String, String, String, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and UTC time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and local time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and date, apply custom filter, sort and limit</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param limit the maximum number of records to return. If you set this to
	 * 0, there is no limit.
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the records
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> List<T> getRecords(String project,
			String table, String subject, DatabaseCriteria criteria,
			DatabaseSort[] sort, int limit, Class<T> dataClass)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return doGetRecords(project, table, subject, null, null, criteria, sort,
				limit, dataClass);
	}

	/**
	 * Reads records from a table within a project. You can filter by user and
	 * UTC time, and apply a custom filter, sort and limit.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>If you don't specify a custom sort, then the returned records are
	 * sorted by utcTime, localTime or id, depending on what fields are
	 * available.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getRecords(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getRecords(String, String, String, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and UTC time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and local time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and date, apply custom filter, sort and limit</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param limit the maximum number of records to return. If you set this to
	 * 0, there is no limit.
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the records
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> List<T> getRecords(String project,
			String table, String subject, ZonedDateTime start,
			ZonedDateTime end, DatabaseCriteria criteria, DatabaseSort[] sort,
			int limit, Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetRecords(project, table, subject, start, end, criteria, sort,
				limit, dataClass);
	}

	/**
	 * Reads records from a table within a project. You can filter by user and
	 * local time, and apply a custom filter, sort and limit.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>If you don't specify a custom sort, then the returned records are
	 * sorted by utcTime, localTime or id, depending on what fields are
	 * available.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getRecords(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getRecords(String, String, String, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and UTC time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and local time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and date, apply custom filter, sort and limit</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param limit the maximum number of records to return. If you set this to
	 * 0, there is no limit.
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the records
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> List<T> getRecords(String project,
			String table, String subject, LocalDateTime start,
			LocalDateTime end, DatabaseCriteria criteria, DatabaseSort[] sort,
			int limit, Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetRecords(project, table, subject, start, end, criteria, sort,
				limit, dataClass);
	}

	/**
	 * Reads records from a table within a project. You can filter by user and
	 * date, and apply a custom filter, sort and limit.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end date. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>If you don't specify a custom sort, then the returned records are
	 * sorted by utcTime, localTime or id, depending on what fields are
	 * available.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getRecords(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getRecords(String, String, String, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and UTC time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and local time, apply custom filter, sort and limit</p>
	 *
	 * <p>{@link #getRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], int, Class)}<br />
	 * Filter by user and date, apply custom filter, sort and limit</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start date or null
	 * @param end the end date or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param limit the maximum number of records to return. If you set this to
	 * 0, there is no limit.
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the records
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> List<T> getRecords(String project,
			String table, String subject, LocalDate start, LocalDate end,
			DatabaseCriteria criteria, DatabaseSort[] sort, int limit,
			Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetRecords(project, table, subject, start, end, criteria, sort,
				limit, dataClass);
	}

	/**
	 * Common implementation for the different getRecords() methods.
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or date or null. If not null, it should be
	 * a {@link ZonedDateTime ZonedDateTime}, {@link LocalDateTime
	 * LocalDateTime} or {@link LocalDate LocalDate}.
	 * @param end the end time or date or null. If not null, it should be a
	 * {@link ZonedDateTime ZonedDateTime}, {@link LocalDateTime LocalDateTime}
	 * or {@link LocalDate LocalDate}.
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param limit the maximum number of records to return. If you set this to
	 * 0, there is no limit.
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the records
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	private <T extends DatabaseObject> List<T> doGetRecords(String project,
			String table, final String subject, final Object start,
			final Object end, final DatabaseCriteria criteria,
			final DatabaseSort[] sort, final int limit,
			final Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		String path, method;
		if (criteria == null && sort == null && limit != 0) {
			path = String.format("/project/%s/table/%s", project, table);
			method = "GET";
		} else {
			path = String.format("/project/%s/table/%s/filter/get",
					project, table);
			method = "POST";
		}
		return runQuery(path, method, true,
			client -> {
				DateTimeFormatter dateFormat = DateTimeUtils.DATE_FORMAT;
				DateTimeFormatter localFormat = DateTimeUtils.LOCAL_FORMAT;
				DateTimeFormatter zonedFormat = DateTimeUtils.ZONED_FORMAT;
				if (subject != null)
					client.addQueryParam("user", subject);
				if (start instanceof ZonedDateTime) {
					client.addQueryParam("start", ((ZonedDateTime)start).format(
							zonedFormat));
				} else if (start instanceof LocalDateTime) {
					client.addQueryParam("start", ((LocalDateTime)start).format(
							localFormat));
				} else if (start instanceof LocalDate) {
					client.addQueryParam("start", ((LocalDate)start).format(
							dateFormat));
				}
				if (end instanceof ZonedDateTime) {
					client.addQueryParam("end", ((ZonedDateTime)end).format(
							zonedFormat));
				} else if (end instanceof LocalDateTime) {
					client.addQueryParam("end", ((LocalDateTime)end).format(
							localFormat));
				} else if (end instanceof LocalDate) {
					client.addQueryParam("end", ((LocalDate)end).format(
							dateFormat));
				}
				Map<String,Object> content = new LinkedHashMap<>();
				if (criteria != null) {
					Map<String,Object> jsonCriteria =
							SelectFilterGenerator.toJsonObject(criteria);
					content.put("filter", jsonCriteria);
				}
				if (sort != null)
					content.put("sort", sort);
				if (limit > 0)
					content.put("limit", limit);
				if (!content.isEmpty())
					client.writeJson(content);
				List<Map<?,?>> mapList = client.readJson(
						new TypeReference<>() {});
				List<T> result = new ArrayList<>();
				DatabaseObjectMapper mapper = new DatabaseObjectMapper();
				for (Map<?,?> map : mapList) {
					result.add(mapper.mapToObject(map, dataClass, true));
				}
				return result;
			});
	}

	/**
	 * Returns the first record from a table. The table is sorted by utcTime,
	 * localTime or id, depending on what fields are available. If there are no
	 * records, this method returns null. You can filter by user.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 * 
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 * 
	 * <p>Related methods:</p>
	 * 
	 * <p>{@link #getFirstRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getFirstRecord(String project,
			String table, String subject, Class<T> dataClass)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return doGetFirstLastRecord(project, table, subject, null, null, null,
				null, dataClass, true);
	}

	/**
	 * Returns the first record from a table. The table is sorted by utcTime,
	 * localTime or id, depending on what fields are available. If there are no
	 * records, this method returns null. You can filter by user and UTC time.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getFirstRecord(String project,
			String table, String subject, ZonedDateTime start,
			ZonedDateTime end, Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, start, end, null,
				null, dataClass, true);
	}

	/**
	 * Returns the first record from a table. The table is sorted by utcTime,
	 * localTime or id, depending on what fields are available. If there are no
	 * records, this method returns null. You can filter by user and local time.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getFirstRecord(String project,
			String table, String subject, LocalDateTime start,
			LocalDateTime end, Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, start, end, null,
				null, dataClass, true);
	}

	/**
	 * Returns the first record from a table. The table is sorted by utcTime,
	 * localTime or id, depending on what fields are available. If there are no
	 * records, this method returns null. You can filter by user and date.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end date. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start date or null
	 * @param end the end date or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getFirstRecord(String project,
			String table, String subject, LocalDate start, LocalDate end,
			Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, start, end, null,
				null, dataClass, true);
	}


	/**
	 * Returns the first record from a table. If there are no records, this
	 * method returns null. You can filter by user and apply a custom filter
	 * and sort. If you don't specify a custom sort, then the table is sorted by
	 * utcTime, localTime or id, depending on what fields are available.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getFirstRecord(String project,
			String table, String subject, DatabaseCriteria criteria,
			DatabaseSort[] sort, Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, null, null,
				criteria, sort, dataClass, true);
	}

	/**
	 * Returns the first record from a table. If there are no records, this
	 * method returns null. You can filter by user and UTC time, and apply a
	 * custom filter and sort. If you don't specify a custom sort, then the
	 * table is sorted by utcTime, localTime or id, depending on what fields
	 * are available.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getFirstRecord(String project,
			String table, String subject, ZonedDateTime start,
			ZonedDateTime end, DatabaseCriteria criteria, DatabaseSort[] sort,
			Class<T> dataClass) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, start, end,
				criteria, sort, dataClass, true);
	}

	/**
	 * Returns the first record from a table. If there are no records, this
	 * method returns null. You can filter by user and local time, and apply a
	 * custom filter and sort. If you don't specify a custom sort, then the
	 * table is sorted by utcTime, localTime or id, depending on what fields
	 * are available.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getFirstRecord(String project,
			String table, String subject, LocalDateTime start,
			LocalDateTime end, DatabaseCriteria criteria, DatabaseSort[] sort,
			Class<T> dataClass) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, start, end,
				criteria, sort, dataClass, true);
	}

	/**
	 * Returns the first record from a table. If there are no records, this
	 * method returns null. You can filter by user and date, and apply a custom
	 * filter and sort. If you don't specify a custom sort, then the table is
	 * sorted by utcTime, localTime or id, depending on what fields are
	 * available.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end date. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getFirstRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start date or null
	 * @param end the end date or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getFirstRecord(String project,
			String table, String subject, LocalDate start, LocalDate end,
			DatabaseCriteria criteria, DatabaseSort[] sort, Class<T> dataClass)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return doGetFirstLastRecord(project, table, subject, start, end,
				criteria, sort, dataClass, true);
	}

	/**
	 * Returns the last record from a table. The table is sorted by utcTime,
	 * localTime or id, depending on what fields are available. If there are no
	 * records, this method returns null. You can filter by user.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getLastRecord(String project,
			String table, String subject, Class<T> dataClass)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return doGetFirstLastRecord(project, table, subject, null, null, null,
				null, dataClass, false);
	}

	/**
	 * Returns the last record from a table. The table is sorted by utcTime,
	 * localTime or id, depending on what fields are available. If there are no
	 * records, this method returns null. You can filter by user and UTC time.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getLastRecord(String project,
			String table, String subject, ZonedDateTime start,
			ZonedDateTime end, Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, start, end, null,
				null, dataClass, false);
	}

	/**
	 * Returns the last record from a table. The table is sorted by utcTime,
	 * localTime or id, depending on what fields are available. If there are no
	 * records, this method returns null. You can filter by user and local time.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getLastRecord(String project,
			String table, String subject, LocalDateTime start,
			LocalDateTime end, Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, start, end, null,
				null, dataClass, false);
	}

	/**
	 * Returns the last record from a table. The table is sorted by utcTime,
	 * localTime or id, depending on what fields are available. If there are no
	 * records, this method returns null. You can filter by user and date.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end date. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start date or null
	 * @param end the end date or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getLastRecord(String project,
			String table, String subject, LocalDate start, LocalDate end,
			Class<T> dataClass) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, start, end, null,
				null, dataClass, false);
	}


	/**
	 * Returns the last record from a table. If there are no records, this
	 * method returns null. You can filter by user and apply a custom filter
	 * and sort. If you don't specify a custom sort, then the table is sorted by
	 * utcTime, localTime or id, depending on what fields are available.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getLastRecord(String project,
			String table, String subject, DatabaseCriteria criteria,
			DatabaseSort[] sort, Class<T> dataClass) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, null, null,
				criteria, sort, dataClass, false);
	}

	/**
	 * Returns the last record from a table. If there are no records, this
	 * method returns null. You can filter by user and UTC time, and apply a
	 * custom filter and sort. If you don't specify a custom sort, then the
	 * table is sorted by utcTime, localTime or id, depending on what fields are
	 * available.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getLastRecord(String project,
			String table, String subject, ZonedDateTime start,
			ZonedDateTime end, DatabaseCriteria criteria, DatabaseSort[] sort,
			Class<T> dataClass) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, start, end,
				criteria, sort, dataClass, false);
	}

	/**
	 * Returns the last record from a table. If there are no records, this
	 * method returns null. You can filter by user and local time, and apply a
	 * custom filter and sort. If you don't specify a custom sort, then the
	 * table is sorted by utcTime, localTime or id, depending on what fields are
	 * available.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getLastRecord(String project,
			String table, String subject, LocalDateTime start,
			LocalDateTime end, DatabaseCriteria criteria, DatabaseSort[] sort,
			Class<T> dataClass) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		return doGetFirstLastRecord(project, table, subject, start, end,
				criteria, sort, dataClass, false);
	}

	/**
	 * Returns the last record from a table. If there are no records, this
	 * method returns null. You can filter by user and date, and apply a custom
	 * filter and sort. If you don't specify a custom sort, then the table is
	 * sorted by utcTime, localTime or id, depending on what fields are
	 * available.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>This method returns the data for the specified subject. If you set
	 * it to null, you will get your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end date. This method returns records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, Class)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, Class)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, Class)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, Class)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and UTC time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and local time, apply custom filter and sort</p>
	 *
	 * <p>{@link #getLastRecord(String, String, String, LocalDate, LocalDate, DatabaseCriteria, DatabaseSort[], Class)}<br />
	 * Filter by user and date, apply custom filter and sort</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start date or null
	 * @param end the end date or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the first record or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getLastRecord(String project,
			String table, String subject, LocalDate start, LocalDate end,
			DatabaseCriteria criteria, DatabaseSort[] sort, Class<T> dataClass)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return doGetFirstLastRecord(project, table, subject, start, end,
				criteria, sort, dataClass, false);
	}

	/**
	 * Common implementation for the different getFirstRecord() and
	 * getLastRecord() methods.
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or date or null. If not null, it should be
	 * a {@link ZonedDateTime ZonedDateTime}, {@link LocalDateTime
	 * LocalDateTime} or {@link LocalDate LocalDate}.
	 * @param end the end time or date or null. If not null, it should be a
	 * {@link ZonedDateTime ZonedDateTime}, {@link LocalDateTime LocalDateTime}
	 * or {@link LocalDate LocalDate}.
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @param sort custom database sort or null
	 * @param dataClass the data class to return
	 * @param isFirst true to get the first record, false to get the last record
	 * @param <T> the data class
	 * @return the records
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	private <T extends DatabaseObject> T doGetFirstLastRecord(String project,
			String table, final String subject, final Object start,
			final Object end, final DatabaseCriteria criteria,
			final DatabaseSort[] sort, final Class<T> dataClass,
			boolean isFirst) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		String path, method;
		if (criteria == null && sort == null) {
			path = String.format("/project/%s/table/%s/%s", project, table,
					isFirst ? "first" : "last");
			method = "GET";
		} else {
			path = String.format("/project/%s/table/%s/filter/get/%s",
					project, table, isFirst ? "first" : "last");
			method = "POST";
		}
		return runQuery(path, method, true,
			client -> {
				DateTimeFormatter dateFormat = DateTimeUtils.DATE_FORMAT;
				DateTimeFormatter localFormat = DateTimeUtils.LOCAL_FORMAT;
				DateTimeFormatter zonedFormat = DateTimeUtils.ZONED_FORMAT;
				if (subject != null)
					client.addQueryParam("user", subject);
				if (start instanceof ZonedDateTime) {
					client.addQueryParam("start", ((ZonedDateTime)start).format(
							zonedFormat));
				} else if (start instanceof LocalDateTime) {
					client.addQueryParam("start", ((LocalDateTime)start).format(
							localFormat));
				} else if (start instanceof LocalDate) {
					client.addQueryParam("start", ((LocalDate)start).format(
							dateFormat));
				}
				if (end instanceof ZonedDateTime) {
					client.addQueryParam("end", ((ZonedDateTime)end).format(
							zonedFormat));
				} else if (end instanceof LocalDateTime) {
					client.addQueryParam("end", ((LocalDateTime)end).format(
							localFormat));
				} else if (end instanceof LocalDate) {
					client.addQueryParam("end", ((LocalDate)end).format(
							dateFormat));
				}
				Map<String,Object> content = new LinkedHashMap<>();
				if (criteria != null) {
					Map<String,Object> jsonCriteria =
							SelectFilterGenerator.toJsonObject(criteria);
					content.put("filter", jsonCriteria);
				}
				if (sort != null)
					content.put("sort", sort);
				if (!content.isEmpty())
					client.writeJson(content);
				Map<?,?> response = client.readJson(Map.class);
				DatabaseObjectMapper mapper = new DatabaseObjectMapper();
				Map<?,?> value = (Map<?,?>)response.get("value");
				if (value == null)
					return null;
				else
					return mapper.mapToObject(value, dataClass, true);
			});
	}

	/**
	 * Reads the record with the specified ID from a table within a project.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (for parameter "dataClass").</p>
	 *
	 * <p>The specified record must belong to the specified subject. If you set
	 * the subject to null, the record should belong to yourself. If the
	 * specified subject doesn't exist or you're not allowed to access the
	 * subject, this method will throw an {@link SenSeeActClientException
	 * SenSeeActClientException} with 403 Forbidden. If the record ID doesn't exist,
	 * this method will throw an {@link SenSeeActClientException SenSeeActClientException}
	 * with 404 Not Found.</p>
	 * 
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param id the record ID
	 * @param dataClass the data class to return
	 * @param <T> the data class
	 * @return the record
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T extends DatabaseObject> T getRecord(String project, String table,
			final String subject, String id, final Class<T> dataClass)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery(String.format("/project/%s/table/%s/%s",
				project, table, id), "GET", true,
				client -> {
					if (subject != null)
						client.addQueryParam("user", subject);
					Map<?,?> map = client.readJson(Map.class);
					DatabaseObjectMapper mapper = new DatabaseObjectMapper();
					return mapper.mapToObject(map, dataClass, true);
				});
	}
	
	/**
	 * Adds a registration to watch a table for new database actions for the
	 * specified subject. If you set the subject to null, it will wait for a
	 * database action for yourself.
	 *
	 * <p>There are two ways to watch for new database actions. The preferred
	 * way is with a callback URL. The other way is with a hanging GET using
	 * {@link #watchTable(String, String, String) watchTable()}. If you want to
	 * receive a callback, you should specify the callback URL.</p>
	 *
	 * <p>If a registration with the same parameters already exists, it does
	 * not create a new registration, but re-uses the current registration. Any
	 * client that was watching that registration will be cancelled. This method
	 * returns a registration ID that you can pass to {@link
	 * #watchTable(String, String, String) watchTable()}.</p>
	 *
	 * <p>If you no longer want to watch the table, you can call {@link
	 * #unregisterWatchTable(String, String, String) unregisterWatchTable()}.
	 * The server also cleans up registrations automatically. Registrations with
	 * a callback URL are removed if the callback failed at least 5 times for at
	 * least 24 hours in a row. Registrations without a callback are removed if
	 * they haven't been watched for an hour.</p>
	 *
	 * <p>Finally a callback can indicate deletion through its response. It
	 * should return 404 Not Found with this JSON payload:<br />
	 * { "error": "callback_expired" }</p>
	 * 
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param callbackUrl a callback URL or null
	 * @param reset true if any pending database actions in an existing
	 * registration should be cleared, false otherwise
	 * @return the registration ID
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public String registerWatchTable(String project, String table,
			final String subject, final String callbackUrl, final boolean reset)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery(String.format("/project/%s/table/%s/watch/register",
				project, table), "POST", true,
				client -> {
					if (subject != null)
						client.addQueryParam("user", subject);
					if (callbackUrl != null)
						client.addQueryParam("callbackUrl", callbackUrl);
					return client.addQueryParam("reset", Boolean.toString(reset))
							.readString();
				});
	}
	
	/**
	 * Adds a registration to watch a table for new database actions for any
	 * subject. Only admins can call this method.
	 *
	 * <p>There are two ways to watch for new database actions. The preferred
	 * way is with a callback URL. The other way is with a hanging GET using
	 * {@link #watchTable(String, String, String) watchTable()}. If you want to
	 * receive a callback, you should specify the callback URL.</p>
	 *
	 * <p>If a registration with the same parameters already exists, it does
	 * not create a new registration, but re-uses the current registration. Any
	 * client that was watching that registration will be cancelled. This method
	 * returns a registration ID that you can pass to {@link
	 * #watchTable(String, String, String) watchTable()}.</p>
	 *
	 * <p>If you no longer want to watch the table, you can call {@link
	 * #unregisterWatchTable(String, String, String) unregisterWatchTable()}.
	 * The server also cleans up registrations automatically. Registrations with
	 * a callback URL are removed if the callback failed at least 5 times for at
	 * least 24 hours in a row. Registrations without a callback are removed if
	 * they haven't been watched for an hour.</p>
	 *
	 * <p>Finally a callback can indicate deletion through its response. It
	 * should return 404 Not Found with this JSON payload:<br />
	 * { "error": "callback_expired" }</p>
	 * 
	 * @param project the project code
	 * @param table the table name
	 * @param callbackUrl a callback URL or null
	 * @param reset true if any pending database actions in an existing
	 * registration should be cleared, false otherwise
	 * @return the registration ID
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public String registerWatchTableAnySubject(String project, String table,
			final String callbackUrl, final boolean reset)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery(String.format("/project/%s/table/%s/watch/register",
				project, table), "POST", true,
				client -> {
					if (callbackUrl != null)
						client.addQueryParam("callbackUrl", callbackUrl);
					return client.addQueryParam("anyUser", "true")
							.addQueryParam("reset", Boolean.toString(reset))
							.readString();
				});
	}
	
	/**
	 * Waits up to one minute until a database action occurs for the specified
	 * watch registration or until the watch is unregistered.
	 * 
	 * <p>As soon as there are new database actions, this method returns them.
	 * This may include actions that occurred since the registration or last
	 * watch and that you haven't received yet. If no actions occur before the
	 * time-out, this method returns an empty list.</p>
	 * 
	 * <p>Only one client can watch a specific registration. If another client
	 * registers or starts to watch the same registration, then the current
	 * client will be cancelled. This means that this method will return
	 * immediately with an empty list result. Likewise, when you call this
	 * method, any other clients that were watching the same registration, will
	 * be cancelled.</p>
	 * 
	 * <p>If you disconnect the client during a watch, the server may still send
	 * an action and think that you received it. If you start a new session and
	 * don't want to miss any actions, it's best to run a read query after the
	 * watch registration.</p>
	 * 
	 * <p>You should specify a registration ID that you got from {@link
	 * #registerWatchTable(String, String, String, String, boolean)
	 * registerWatchTable()} or {@link
	 * #registerWatchTableAnySubject(String, String, String, boolean)
	 * registerWatchTableAnySubject()}. If you no longer want to watch the
	 * table, you should call {@link
	 * #unregisterWatchTable(String, String, String) unregisterWatchTable()}.
	 * Note that the server may automatically clean up registrations that
	 * haven't been watched for an hour.</p>
	 * 
	 * @param project the project code
	 * @param table the table name
	 * @param regId the registration ID
	 * @return user IDs of the users for which database actions occurred
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<String> watchTable(String project, String table, String regId)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery(String.format("/project/%s/table/%s/watch/%s",
				project, table, regId), "GET", true,
				client -> client.readJson(new TypeReference<>() {}));
	}
	
	/**
	 * Removes a registration that was added with {@link
	 * #registerWatchTable(String, String, String, String, boolean)
	 * registerWatchTable()} or {@link
	 * #registerWatchTableAnySubject(String, String, String, boolean)
	 * registerWatchTableAnySubject()}. Any client that was watching this
	 * registration, will be cancelled. If the registration doesn't exist, this
	 * method has no effect.
	 * 
	 * @param project the project code
	 * @param table the table name
	 * @param regId the registration ID
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void unregisterWatchTable(String project, String table, String regId)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery(String.format("/project/%s/table/%s/watch/unregister/%s",
				project, table, regId), "POST", true,
				client -> {
					client.readString();
					return null;
				});
	}
	
	/**
	 * Inserts a record into a table within a project. The new ID will be set
	 * into the specified record.
	 * 
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (to use in parameter "record").</p>
	 *
	 * <p>This method writes data for the specified subject. If you set it to
	 * null, that will be yourself. The specified record must belong to the
	 * subject. If the subject doesn't exist or you're not allowed to access
	 * the subject, this method will throw an {@link SenSeeActClientException
	 * SenSeeActClientException} with 403 Forbidden.</p>
	 * 
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param record the record (must belong to the specified subject)
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void writeRecord(String project, String table, String subject,
			DatabaseObject record) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		writeRecords(project, table, subject, Collections.singletonList(
				record));
	}

	/**
	 * Inserts records into a table within a project. The new IDs will be set
	 * into the specified records.
	 * 
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}. The
	 * returned tables contain the name (for parameter "table") and the data
	 * class (to use in parameter "records").</p>
	 * 
	 * <p>This method writes data for the specified subject. If you set it to
	 * null, that will be yourself. The specified records must belong to the
	 * subject. If the subject doesn't exist or you're not allowed to access
	 * the subject, this method will throw an {@link SenSeeActClientException
	 * SenSeeActClientException} with 403 Forbidden.</p>
	 * 
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param records the records (must belong to the specified subject)
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void writeRecords(String project, String table, final String subject,
			final List<? extends DatabaseObject> records)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery(String.format("/project/%s/table/%s", project, table),
				"POST", true,
				client -> {
					if (subject != null)
						client.addQueryParam("user", subject);
					DatabaseObjectMapper mapper = new DatabaseObjectMapper();
					List<Map<?,?>> maps = new ArrayList<>();
					for (DatabaseObject record : records) {
						maps.add(mapper.objectToMap(record, true));
					}
					List<String> ids = client.writeJson(maps)
							.readJson(new TypeReference<>() {});
					Iterator<String> idIt = ids.iterator();
					for (DatabaseObject record : records) {
						record.setId(idIt.next());
					}
					return null;
				});
	}
	
	/**
	 * Updates a record in a table within a project. You cannot change the ID or
	 * user. If the record user doesn't exist or you're not allowed to access
	 * the user, this method will throw an {@link SenSeeActClientException
	 * SenSeeActClientException} with 403 Forbidden. If the record ID doesn't exist,
	 * this method will throw an {@link SenSeeActClientException SenSeeActClientException}
	 * with 404 Not Found.
	 * 
	 * @param project the project code
	 * @param table the table name
	 * @param record the updated record
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void updateRecord(String project, String table,
			final DatabaseObject record) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		runQuery(String.format("/project/%s/table/%s/%s",
				project, table, record.getId()),
				"PUT", true,
				client -> {
					String user = (String)PropertyReader.readProperty(record,
							"user");
					client.addQueryParam("user", user);
					DatabaseObjectMapper mapper = new DatabaseObjectMapper();
					Map<?,?> map = mapper.objectToMap(record, true);
					client.writeJson(map).readString();
					return null;
				});
	}

	/**
	 * Deletes records from a table within a project. You can filter by user.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}.</p>
	 *
	 * <p>This method deletes data for the specified subject. If you set it to
	 * null, you will delete your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, DatabaseCriteria)}<br />
	 * Filter by user, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria)}<br />
	 * Filter by user and UTC time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria)}<br />
	 * Filter by user and local time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria)}<br />
	 * Filter by user and date, apply custom filter</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteRecords(String project, String table, String subject)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		doDeleteRecords(project, table, subject, null, null, null);
	}

	/**
	 * Deletes records from a table within a project. You can filter by user and
	 * UTC time.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}.</p>
	 *
	 * <p>This method deletes data for the specified subject. If you set it to
	 * null, you will delete your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method deletes records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, DatabaseCriteria)}<br />
	 * Filter by user, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria)}<br />
	 * Filter by user and UTC time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria)}<br />
	 * Filter by user and local time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria)}<br />
	 * Filter by user and date, apply custom filter</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteRecords(String project, String table, String subject,
			ZonedDateTime start, ZonedDateTime end) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		doDeleteRecords(project, table, subject, start, end, null);
	}

	/**
	 * Deletes records from a table within a project. You can filter by user and
	 * local time.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}.</p>
	 *
	 * <p>This method deletes data for the specified subject. If you set it to
	 * null, you will delete your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method deletes records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, DatabaseCriteria)}<br />
	 * Filter by user, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria)}<br />
	 * Filter by user and UTC time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria)}<br />
	 * Filter by user and local time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria)}<br />
	 * Filter by user and date, apply custom filter</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteRecords(String project, String table, String subject,
			LocalDateTime start, LocalDateTime end)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		doDeleteRecords(project, table, subject, start, end, null);
	}

	/**
	 * Deletes records from a table within a project. You can filter by user and
	 * date.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}.</p>
	 *
	 * <p>This method deletes data for the specified subject. If you set it to
	 * null, you will delete your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end date. This method deletes records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, DatabaseCriteria)}<br />
	 * Filter by user, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria)}<br />
	 * Filter by user and UTC time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria)}<br />
	 * Filter by user and local time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria)}<br />
	 * Filter by user and date, apply custom filter</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start date or null
	 * @param end the end date or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteRecords(String project, String table, String subject,
			LocalDate start, LocalDate end) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		doDeleteRecords(project, table, subject, start, end, null);
	}

	/**
	 * Deletes records from a table within a project. You can filter by user and
	 * apply a custom filter.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}.</p>
	 *
	 * <p>This method deletes data for the specified subject. If you set it to
	 * null, you will delete your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, DatabaseCriteria)}<br />
	 * Filter by user, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria)}<br />
	 * Filter by user and UTC time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria)}<br />
	 * Filter by user and local time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria)}<br />
	 * Filter by user and date, apply custom filter</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteRecords(String project, String table, String subject,
			DatabaseCriteria criteria) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		doDeleteRecords(project, table, subject, null, null, criteria);
	}

	/**
	 * Deletes records from a table within a project. You can filter by user and
	 * UTC time, and apply a custom filter.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}.</p>
	 *
	 * <p>This method deletes data for the specified subject. If you set it to
	 * null, you will delete your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method deletes records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, DatabaseCriteria)}<br />
	 * Filter by user, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria)}<br />
	 * Filter by user and UTC time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria)}<br />
	 * Filter by user and local time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria)}<br />
	 * Filter by user and date, apply custom filter</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteRecords(String project, String table, String subject,
			ZonedDateTime start, ZonedDateTime end, DatabaseCriteria criteria)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		doDeleteRecords(project, table, subject, start, end, criteria);
	}

	/**
	 * Deletes records from a table within a project. You can filter by user and
	 * local time, and apply a custom filter.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}.</p>
	 *
	 * <p>This method deletes data for the specified subject. If you set it to
	 * null, you will delete your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end time. This method deletes records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, DatabaseCriteria)}<br />
	 * Filter by user, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria)}<br />
	 * Filter by user and UTC time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria)}<br />
	 * Filter by user and local time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria)}<br />
	 * Filter by user and date, apply custom filter</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or null
	 * @param end the end time or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteRecords(String project, String table, String subject,
			LocalDateTime start, LocalDateTime end, DatabaseCriteria criteria)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		doDeleteRecords(project, table, subject, start, end, criteria);
	}

	/**
	 * Deletes records from a table within a project. You can filter by user and
	 * date, and apply a custom filter.
	 *
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}.</p>
	 *
	 * <p>This method deletes data for the specified subject. If you set it to
	 * null, you will delete your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 *
	 * <p>You may specify a start and end date. This method deletes records so
	 * that start &lt;= record &lt; end.</p>
	 *
	 * <p>Related methods:</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String)}<br />
	 * Filter by user</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime)}<br />
	 * Filter by user and UTC time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime)}<br />
	 * Filter by user and local time</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate)}<br />
	 * Filter by user and date</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, DatabaseCriteria)}<br />
	 * Filter by user, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, ZonedDateTime, ZonedDateTime, DatabaseCriteria)}<br />
	 * Filter by user and UTC time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDateTime, LocalDateTime, DatabaseCriteria)}<br />
	 * Filter by user and local time, apply custom filter</p>
	 *
	 * <p>{@link #deleteRecords(String, String, String, LocalDate, LocalDate, DatabaseCriteria)}<br />
	 * Filter by user and date, apply custom filter</p>
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start date or null
	 * @param end the end date or null
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteRecords(String project, String table, String subject,
			LocalDate start, LocalDate end, DatabaseCriteria criteria)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		doDeleteRecords(project, table, subject, start, end, criteria);
	}

	/**
	 * Common implementation for the different deleteRecords() methods.
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param start the start time or date or null. If not null, it should be
	 * a {@link ZonedDateTime ZonedDateTime}, {@link LocalDateTime
	 * LocalDateTime} or {@link LocalDate LocalDate}.
	 * @param end the end time or date or null. If not null, it should be a
	 * {@link ZonedDateTime ZonedDateTime}, {@link LocalDateTime LocalDateTime}
	 * or {@link LocalDate LocalDate}.
	 * @param criteria database criteria on other fields than user or time.
	 * This can be null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	private void doDeleteRecords(String project, String table,
			final String subject, final Object start, final Object end,
			final DatabaseCriteria criteria) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		String path, method;
		if (criteria == null) {
			path = String.format("/project/%s/table/%s", project, table);
			method = "DELETE";
		} else {
			path = String.format("/project/%s/table/%s/filter/delete",
					project, table);
			method = "POST";
		}
		runQuery(path, method, true,
			client -> {
				DateTimeFormatter dateFormat = DateTimeUtils.DATE_FORMAT;
				DateTimeFormatter localFormat = DateTimeUtils.LOCAL_FORMAT;
				DateTimeFormatter zonedFormat = DateTimeUtils.ZONED_FORMAT;
				if (subject != null)
					client.addQueryParam("user", subject);
				if (start instanceof ZonedDateTime) {
					client.addQueryParam("start", ((ZonedDateTime)start).format(
							zonedFormat));
				} else if (start instanceof LocalDateTime) {
					client.addQueryParam("start", ((LocalDateTime)start).format(
							localFormat));
				} else if (start instanceof LocalDate) {
					client.addQueryParam("start", ((LocalDate)start).format(
							dateFormat));
				}
				if (end instanceof ZonedDateTime) {
					client.addQueryParam("end", ((ZonedDateTime)end).format(
							zonedFormat));
				} else if (end instanceof LocalDateTime) {
					client.addQueryParam("end", ((LocalDateTime)end).format(
							localFormat));
				} else if (end instanceof LocalDate) {
					client.addQueryParam("end", ((LocalDate)end).format(
							dateFormat));
				}
				if (criteria != null) {
					Map<String,Object> jsonCriteria =
							SelectFilterGenerator.toJsonObject(criteria);
					Map<String,Object> content = new LinkedHashMap<>();
					content.put("filter", jsonCriteria);
					client.writeJson(content);
				}
				client.readString();
				return null;
			});
	}

	/**
	 * Deletes a record from a table within a project. The record should belong
	 * to the specified subject. If the subject doesn't exist or you're not
	 * allowed to access the subject, this method will throw an {@link
	 * SenSeeActClientException SenSeeActClientException} with 403 Forbidden. You may set
	 * the subject to null if you want to delete a record of your own. If the
	 * record ID doesn't exist, this method has no effect.
	 *
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @param id the ID of the record to delete
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void deleteRecord(String project, String table, final String subject,
			String id) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		runQuery(String.format("/project/%s/table/%s/%s", project, table, id),
				"DELETE", true,
				client -> {
					if (subject != null)
						client.addQueryParam("user", subject);
					client.readString();
					return null;
				});
	}

	/**
	 * Purges all records for a user from a table within a project. This method
	 * also deletes database actions and synchronization progress. It should
	 * only be used for maintenance. Normally you can delete records with one
	 * of the deleteRecords() methods, which creates delete actions in the
	 * action table.
	 * 
	 * <p>To get the tables in a project, get the {@link BaseProject
	 * BaseProject} from the {@link ProjectRepository ProjectRepository} and
	 * call {@link BaseProject#getDatabaseTables() getDatabaseTables()}.</p>
	 * 
	 * <p>This method purges data for the specified subject. If you set it to
	 * null, you will purge your own data. If the subject doesn't exist or
	 * you're not allowed to access the subject, this method will throw an
	 * {@link SenSeeActClientException SenSeeActClientException} with 403 Forbidden.</p>
	 * 
	 * @param project the project code
	 * @param table the table name
	 * @param subject the user ID of the subject user or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void purgeRecords(String project, String table, final String subject)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery(String.format("/project/%s/table/%s/purge",
				project, table), "DELETE", true,
				client -> {
					if (subject != null)
						client.addQueryParam("user", subject);
					client.readString();
					return null;
				});
	}

	/**
	 * Synchronizes new database actions from the remote server database to the
	 * local client database. It gets all data for the specified user.
	 * 
	 * <p>The database actions are retrieved in batches of a specified size and
	 * a listener can be notified at every batch and at the completion of the
	 * entire process.</p>
	 * 
	 * <p>You need to specify the object database and sample database for the
	 * specified project. You can get them from {@link BaseProject
	 * BaseProject}. Either of them may be null.</p>
	 * 
	 * @param project the project code
	 * @param batchSize the maximum number of database actions to read in one
	 * batch
	 * @param db the project database (can be null)
	 * @param subject the user ID of the subject user
	 * @param includeOwn true if actions that were earlier synchronized from
	 * the local database to the server, should be included
	 * @param syncRestriction synchronization restrictions. This can be null.
	 * Any time range restrictions are only used to limit database actions to
	 * receive, not to purge old data.
	 * @param listener a progress listener or null
	 * @return the synchronization result
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 * @throws DatabaseException if a database error occurs or received
	 * database actions are illegal
	 */
	public SyncResult syncRead(String project, int batchSize, Database db,
			String subject, boolean includeOwn, SyncRestriction syncRestriction,
			SyncProgressListener listener) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException,
			DatabaseException {
		if (subject == null)
			throw new NullPointerException("Parameter \"subject\" is null");
		if (syncRestriction == null)
			syncRestriction = new SyncRestriction();
		if (db == null || !syncRestriction.isSyncEnabled())
			return new SyncResult();
		logger.info("Start synchronization from server");
		SyncActionStats stats = getSyncReadStats(project, db, subject,
				syncRestriction.getTableRestriction(),
				syncRestriction.getTimeRangeRestrictions(), includeOwn);
		logger.info(
				"Database actions to read from sample database at server: " +
				stats);
		int expectedTotal = stats.getCount();
		logger.info("Start sync sample database from server");
		int total = syncRead(project, batchSize, stats, db, subject,
				syncRestriction.getTableRestriction(),
				syncRestriction.getTimeRangeRestrictions(), includeOwn,
				listener);
		logger.info("Completed synchronization from server: {} of estimated {} unmerged database actions",
				total, expectedTotal);
		SyncResult result = new SyncResult();
		result.setCount(total);
		result.setProgress(stats.getProgress());
		return result;
	}
	
	/**
	 * Returns statistics about the new database actions that should be
	 * synchronized from the remote server database to the local client
	 * database. It gets all data for the specified user.
	 * 
	 * <p>You need to specify the object database or the sample database for
	 * the specified project. You can get it from {@link BaseProject
	 * BaseProject} and it can be null.</p>
	 * 
	 * @param project the project code
	 * @param db the object database or sample database of the project (can be
	 * null)
	 * @param subject the user ID of the subject user
	 * @param tableRestriction tables to include or exclude
	 * @param timeRangeRestrictions time range restrictions or null or an empty
	 * list. Restrictions are only used to limit database actions to receive,
	 * not to purge old data.
	 * @param includeOwn true if actions that were earlier synchronized from
	 * the local database to the server, should be included
	 * @return the statistics about the new database actions
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 * @throws DatabaseException if a database error occurs
	 */
	private SyncActionStats getSyncReadStats(String project, Database db,
			final String subject, SyncTableRestriction tableRestriction,
			List<SyncTimeRangeRestriction> timeRangeRestrictions,
			boolean includeOwn) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException, DatabaseException {
		if (subject == null)
			throw new NullPointerException("Parameter \"subject\" is null");
		if (db == null)
			return new SyncActionStats(null, 0, null);
		getAuthHeaders();
		logger.debug(String.format(
				"Get progress of actions from server written to database (user: %s)",
				subject));
		DatabaseSynchronizer sync = new DatabaseSynchronizer(subject);
		sync.setIncludeTables(tableRestriction.getIncludeTables());
		sync.setExcludeTables(tableRestriction.getExcludeTables());
		List<SyncProgress> progress = sync.getSyncProgress(db);
		logger.debug("Progress of actions from server written to database: " +
				progress);
		final Map<String,Object> params = new LinkedHashMap<>();
		params.put("includeOwn", includeOwn);
		params.put("progress", progress);
		params.put("includeTables", tableRestriction.getIncludeTables());
		params.put("excludeTables", tableRestriction.getExcludeTables());
		if (timeRangeRestrictions != null && !timeRangeRestrictions.isEmpty()) {
			params.put("timeRangeRestrictions", timeRangeRestrictions);
		}
		return runQuery(
				String.format("/sync/project/%s/get-read-stats", project),
				"POST", true,
				client -> client.addQueryParam("user", subject)
						.writeJson(params)
						.readJson(SyncActionStats.class));
	}

	/**
	 * Synchronizes new database actions from the remote server database to the
	 * local client database. It gets all data for the specified user.
	 * 
	 * <p>The database actions are retrieved in batches of a specified size and
	 * a listener can be notified at every batch and at the completion of the
	 * entire process.</p>
	 * 
	 * <p>You need to specify the object database or the sample database for
	 * the specified project. You can get it from {@link BaseProject
	 * BaseProject} and it can be null.</p>
	 * 
	 * <p>The progress in syncStats is updated after the synchronization.</p>
	 * 
	 * @param project the project code
	 * @param batchSize the maximum number of database actions to read in one
	 * batch
	 * @param syncStats statistics about the new database actions
	 * @param db the object database or sample database of the project (can be
	 * null)
	 * @param subject the user ID of the subject user
	 * @param tableRestriction tables to include or exclude
	 * @param timeRangeRestrictions time range restrictions or null or an empty
	 * list. Restrictions are only used to limit database actions to receive,
	 * not to purge old data.
	 * @param includeOwn true if actions that were earlier synchronized from
	 * the local database to the server, should be included
	 * @param listener a progress listener or null
	 * @return the total number of synchronized database actions
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 * @throws DatabaseException if a database error occurs or received
	 * database actions are illegal
	 */
	private int syncRead(String project, int batchSize,
			SyncActionStats syncStats, Database db, String subject,
			SyncTableRestriction tableRestriction,
			List<SyncTimeRangeRestriction> timeRangeRestrictions,
			boolean includeOwn, SyncProgressListener listener)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException, DatabaseException {
		if (subject == null)
			throw new NullPointerException("Parameter \"subject\" is null");
		if (db == null)
			return 0;
		int totalCount = 0;
		boolean hasMore = true;
		while (hasMore) {
			int count = syncReadBatch(project, batchSize, syncStats, db,
					subject, tableRestriction, timeRangeRestrictions,
					includeOwn);
			if (count > 0) {
				totalCount += count;
				if (listener != null)
					listener.syncUpdate(totalCount, syncStats.getCount());
			} else {
				hasMore = false;
			}
		}
		return totalCount;
	}
	
	/**
	 * Synchronizes a batch of new database actions from the remote server
	 * database to the local client database. It gets all data for the specified
	 * user.
	 * 
	 * <p>You need to specify the object database or the sample database for
	 * the specified project. You can get it from {@link BaseProject
	 * BaseProject} and it can be null.</p>
	 * 
	 * <p>The progress in syncStats is updated after the synchronization.</p>
	 * 
	 * @param project the project code
	 * @param maxCount the maximum number of database actions to read
	 * @param syncStats statistics about the new database actions
	 * @param db the object database or sample database of the project (can be
	 * null)
	 * @param subject the user ID of the subject user
	 * @param tableRestriction tables to include or exclude
	 * @param timeRangeRestrictions time range restrictions or null or an empty
	 * list. Restrictions are only used to limit database actions to receive,
	 * not to purge old data.
	 * @param includeOwn true if actions that were earlier synchronized from
	 * the local database to the server, should be included
	 * @return the number of synchronized database actions
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 * @throws DatabaseException if a database error occurs or received
	 * database actions are illegal
	 */
	private int syncReadBatch(String project, int maxCount,
			SyncActionStats syncStats, Database db, final String subject,
			SyncTableRestriction tableRestriction,
			List<SyncTimeRangeRestriction> timeRangeRestrictions,
			boolean includeOwn) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException, DatabaseException {
		if (subject == null)
			throw new NullPointerException("Parameter \"subject\" is null");
		if (db == null)
			return 0;
		logger.debug("Read batch of database actions from server");
		final Map<String,Object> params = new LinkedHashMap<>();
		params.put("maxCount", maxCount);
		params.put("maxTime", syncStats.getLatestTime());
		params.put("includeOwn", includeOwn);
		params.put("progress", syncStats.getProgress());
		params.put("includeTables", tableRestriction.getIncludeTables());
		params.put("excludeTables", tableRestriction.getExcludeTables());
		if (timeRangeRestrictions != null && !timeRangeRestrictions.isEmpty()) {
			params.put("timeRangeRestrictions", timeRangeRestrictions);
		}
		List<DatabaseAction> actions = runQuery(
				String.format("/sync/project/%s/read", project), "POST", true,
				client -> client.addQueryParam("user", subject)
						.writeJson(params)
						.readJson(new TypeReference<>() {})
		);
		if (actions.size() == 0)
			return 0;
		getAuthHeaders();
		logger.debug(String.format(
			"Write batch of database actions to local database (user: %s)",
			subject));
		DatabaseSynchronizer sync = new DatabaseSynchronizer(subject);
		sync.setIncludeTables(tableRestriction.getIncludeTables());
		sync.setExcludeTables(tableRestriction.getExcludeTables());
		try {
			sync.writeSyncActions(db, actions, SYNC_REMOTE_ID);
		} catch (IllegalInputException | SyncForbiddenException ex) {
			throw new DatabaseException(ex.getMessage(), ex);
		}
		syncStats.setProgress(sync.getSyncProgress(db));
		logger.debug("Synchronized batch from server: " + actions.size() +
				" items");
		for (int i = 0; i < actions.size() && i < MAX_ACTION_LOG; i++) {
			logger.debug("    " + actions.get(i));
		}
		return actions.size();
	}

	public void syncRegisterPush(String project, final String subject,
			final SyncTableRestriction tableRestriction, final String deviceId,
			final String fcmToken) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		runQuery(String.format("/sync/project/%s/register-push", project), "POST",
				true,
				client -> {
					if (subject != null)
						client.addQueryParam("user", subject);
					Map<String,Object> data = new LinkedHashMap<>();
					data.put("fcmToken", fcmToken);
					data.put("deviceId", deviceId);
					data.put("includeTables", tableRestriction.getIncludeTables());
					data.put("excludeTables", tableRestriction.getExcludeTables());
					client.writeJson(data).readString();
					return null;
				});
	}

	public void syncUnregisterPush(String project, final String subject,
			final String deviceId) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		runQuery(String.format("/sync/project/%s/unregister-push", project),
				"POST", true,
				client -> {
					if (subject != null)
						client.addQueryParam("user", subject);
					client.addQueryParam("deviceId", deviceId)
						.readString();
					return null;
				});
	}

	/**
	 * Synchronizes a batch of new database actions from the remote server
	 * database to the local client database. If no new actions are available,
	 * this method blocks until a new action arrives or a timeout occurs. It
	 * gets all data for the specified user.
	 * 
	 * <p>You need to specify the object database or the sample database for
	 * the specified project. You can get it from {@link BaseProject
	 * BaseProject} and it can be null.</p>
	 * 
	 * @param project the project code
	 * @param maxCount the maximum number of database actions to read
	 * @param db the object database or sample database of the project (can be
	 * null)
	 * @param subject the user ID of the subject user
	 * @param tableRestriction tables to include or exclude
	 * @param timeRangeRestrictions time range restrictions or null or an empty
	 * list. Restrictions are only used to limit database actions to receive,
	 * not to purge old data.
	 * @return true if the method can be called again to receive or wait for
	 * new actions, false if there will never be database actions (because the
	 * database is not defined)
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 * @throws DatabaseException if a database error occurs or received
	 * database actions are illegal
	 */
	public boolean syncWatch(String project, int maxCount, Database db,
			final String subject, SyncTableRestriction tableRestriction,
			List<SyncTimeRangeRestriction> timeRangeRestrictions)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException, DatabaseException {
		if (subject == null)
			throw new NullPointerException("Parameter \"subject\" is null");
		if (db == null)
			return false;
		logger.info("Start synchronization watch from database at server");
		SyncActionStats syncStats = getSyncReadStats(project, db, subject,
				tableRestriction, timeRangeRestrictions, false);
		logger.info("Database actions to read from database at server: " +
				syncStats);
		final Map<String,Object> params = new LinkedHashMap<>();
		params.put("maxCount", maxCount);
		params.put("progress", syncStats.getProgress());
		params.put("includeTables", tableRestriction.getIncludeTables());
		params.put("excludeTables", tableRestriction.getExcludeTables());
		if (timeRangeRestrictions != null && !timeRangeRestrictions.isEmpty()) {
			params.put("timeRangeRestrictions", timeRangeRestrictions);
		}
		SyncWatchResult syncRes = runQuery(String.format(
				"/sync/project/%s/watch", project), "POST", true,
				client -> {
					client.addQueryParam("user", subject);
					return client.writeJson(params)
							.readJson(SyncWatchResult.class);
				});
		if (syncRes.getResultCode() == ResultCode.NO_DATA)
			return false;
		else if (syncRes.getResultCode() == ResultCode.TIMEOUT)
			return true;
		getAuthHeaders();
		logger.debug(String.format(
			"Write batch of database actions to local database (user: %s)",
			subject));
		DatabaseSynchronizer sync = new DatabaseSynchronizer(subject);
		sync.setIncludeTables(tableRestriction.getIncludeTables());
		sync.setExcludeTables(tableRestriction.getExcludeTables());
		try {
			sync.writeSyncActions(db, syncRes.getActions(), SYNC_REMOTE_ID);
		} catch (IllegalInputException | SyncForbiddenException ex) {
			throw new DatabaseException(ex.getMessage(), ex);
		}
		syncStats.setProgress(sync.getSyncProgress(db));
		logger.debug("Synchronized batch from server: " +
				syncRes.getActions().size() + " items");
		for (int i = 0; i < syncRes.getActions().size() && i < MAX_ACTION_LOG;
				i++) {
			logger.debug("    " + syncRes.getActions().get(i));
		}
		return true;
	}
	
	/**
	 * Synchronizes new database actions from the local client database to the
	 * remote server database. It assumes that the local database only contains
	 * user data for the specified user.
	 * 
	 * <p>The database actions are written in batches of a specified size and
	 * a listener can be notified at every batch and at the completion of the
	 * entire process.</p>
	 * 
	 * <p>You need to specify the object database and sample database for the
	 * specified project. You can get them from {@link BaseProject
	 * BaseProject}. Either of them may be null.</p>
	 * 
	 * @param project the project code
	 * @param batchSize the maximum number of database actions to write in one
	 * batch
	 * @param db the project database (can be null)
	 * @param subject the user ID of the subject user
	 * @param syncRestriction synchronization restrictions. This can be null.
	 * Any sample time range restrictions are ignored.
	 * @param listener a progress listener or null
	 * @return the synchronization result
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 * @throws DatabaseException if a database error occurs
	 */
	public SyncResult syncWrite(String project, int batchSize, Database db,
			String subject, SyncRestriction syncRestriction,
			final SyncProgressListener listener) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException,
			DatabaseException {
		if (subject == null)
			throw new NullPointerException("Parameter \"subject\" is null");
		if (syncRestriction == null)
			syncRestriction = new SyncRestriction();
		if (db == null || !syncRestriction.isSyncEnabled())
			return new SyncResult();
		logger.info("Start synchronization to server");
		SyncTableRestriction restrict = syncRestriction.getTableRestriction();
		SyncActionStats stats = getSyncWriteStats(project, db, subject,
				restrict);
		logger.info(
				"Database actions to sync from sample database to server: " +
				stats);
		int expectedTotal = stats.getCount();
		logger.info("Start sync sample database to server");
		int total = syncWrite(project, batchSize, stats, db, subject, restrict,
				listener);
		logger.info("Completed synchronization to server: {} of estimated {} unmerged database actions",
				total, expectedTotal);
		SyncResult result = new SyncResult();
		result.setCount(total);
		result.setProgress(stats.getProgress());
		return result;
	}

	/**
	 * Returns statistics about the new database actions that should be
	 * synchronized from the local client database to the remote server
	 * database. It assumes that the local database only contains user data for
	 * the specified user.
	 * 
	 * <p>You need to specify the object database or the sample database for
	 * the specified project. You can get it from {@link BaseProject
	 * BaseProject} and it can be null.</p>
	 * 
	 * @param project the project code
	 * @param db the object database or sample database of the project (can be
	 * null)
	 * @param subject the user ID of the subject user
	 * @param tableRestriction tables to include or exclude
	 * @return the statistics about the new database actions
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 * @throws DatabaseException if a database error occurs
	 */
	private SyncActionStats getSyncWriteStats(String project, Database db,
			final String subject, SyncTableRestriction tableRestriction)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException, DatabaseException {
		if (subject == null)
			throw new NullPointerException("Parameter \"subject\" is null");
		if (db == null)
			return new SyncActionStats(null, 0, null);
		final Map<String,Object> params = new LinkedHashMap<>();
		params.put("includeTables", tableRestriction.getIncludeTables());
		params.put("excludeTables", tableRestriction.getExcludeTables());
		String action = String.format("/sync/project/%s/get-progress", project);
		List<SyncProgress> progress = runQuery(action, "POST", true,
			client -> {
				client.addQueryParam("user", subject);
				return client.writeJson(params)
						.readJson(new TypeReference<>() {});
			});
		logger.debug("Progress of actions from database written to server: " +
				progress);
		getAuthHeaders();
		logger.debug(String.format(
				"Get sync stats for actions from database to write to server (user: %s)",
				subject));
		DatabaseSynchronizer sync = new DatabaseSynchronizer(subject);
		sync.setIncludeTables(tableRestriction.getIncludeTables());
		sync.setExcludeTables(tableRestriction.getExcludeTables());
		return sync.getSyncActionStats(db, progress,
				Collections.singletonList(SYNC_REMOTE_ID));
	}
	
	/**
	 * Synchronizes new database actions from the local client database to the
	 * remote server database. It assumes that the local database only contains
	 * user data for the specified user.
	 * 
	 * <p>The database actions are retrieved in batches of a specified size and
	 * a listener can be notified at every batch and at the completion of the
	 * entire process.</p>
	 * 
	 * <p>You need to specify the object database or the sample database for
	 * the specified project. You can get it from {@link BaseProject
	 * BaseProject} and it can be null.</p>
	 * 
	 * <p>The progress in syncStats is updated after the synchronization.</p>
	 * 
	 * @param project the project code
	 * @param batchSize the maximum number of database actions to write in one
	 * batch
	 * @param syncStats statistics about the new database actions
	 * @param db the object database or sample database of the project (can be
	 * null)
	 * @param subject the user ID of the subject user
	 * @param tableRestriction tables to include or exclude
	 * @param listener a progress listener or null
	 * @return the total number of synchronized database actions
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 * @throws DatabaseException if a database error occurs
	 */
	private int syncWrite(String project, int batchSize,
			SyncActionStats syncStats, Database db, String subject,
			SyncTableRestriction tableRestriction,
			SyncProgressListener listener) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException,
			DatabaseException {
		if (subject == null)
			throw new NullPointerException("Parameter \"subject\" is null");
		if (db == null)
			return 0;
		int totalCount = 0;
		boolean hasMore = true;
		while (hasMore) {
			int count = syncWriteBatch(project, batchSize, syncStats, db,
					subject, tableRestriction);
			if (count > 0) {
				totalCount += count;
				if (listener != null)
					listener.syncUpdate(totalCount, syncStats.getCount());
			} else {
				hasMore = false;
			}
		}
		return totalCount;
	}
	
	/**
	 * Synchronizes a batch of new database actions from the local client
	 * database to the remote server database. It assumes that the local
	 * database only contains user data for the specified user.
	 * 
	 * <p>You need to specify the object database or the sample database for
	 * the specified project. You can get it from {@link BaseProject
	 * BaseProject} and it can be null.</p>
	 * 
	 * <p>The progress in syncStats is updated after the synchronization.</p>
	 * 
	 * @param project the project code
	 * @param maxCount the maximum number of database actions to write
	 * @param syncStats statistics about the new database actions
	 * @param db the object database or sample database of the project (can be
	 * null)
	 * @param subject the user ID of the subject user
	 * @param tableRestriction tables to include or exclude
	 * @return the number of synchronized database actions
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 * @throws DatabaseException if a database error occurs
	 */
	private int syncWriteBatch(String project, int maxCount,
			SyncActionStats syncStats, Database db, final String subject,
			SyncTableRestriction tableRestriction) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException, DatabaseException {
		if (subject == null)
			throw new NullPointerException("Parameter \"subject\" is null");
		if (db == null)
			return 0;
		getAuthHeaders();
		logger.debug(String.format(
				"Read batch of database actions from local database (user: %s)",
				subject));
		DatabaseSynchronizer sync = new DatabaseSynchronizer(subject);
		sync.setIncludeTables(tableRestriction.getIncludeTables());
		sync.setExcludeTables(tableRestriction.getExcludeTables());
		List<DatabaseAction> actions = sync.readSyncActions(db,
				syncStats.getProgress(), maxCount, syncStats.getLatestTime(),
				Collections.singletonList(SYNC_REMOTE_ID));
		if (actions.size() == 0)
			return 0;
		logger.debug("Write batch of database actions to server");
		final Map<String,Object> params = new LinkedHashMap<>();
		params.put("actions", actions);
		params.put("includeTables", tableRestriction.getIncludeTables());
		params.put("excludeTables", tableRestriction.getExcludeTables());
		runQuery(String.format("/sync/project/%s/write", project),
				"POST", true,
				client -> {
					client.addQueryParam("user", subject);
					return client.writeJson(params).readString();
				});
		params.clear();
		params.put("includeTables", tableRestriction.getIncludeTables());
		params.put("excludeTables", tableRestriction.getExcludeTables());
		List<SyncProgress> progress = runQuery(String.format(
				"/sync/project/%s/get-progress", project), "POST", true,
				client -> client.addQueryParam("user", subject)
						.writeJson(params)
						.readJson(new TypeReference<>() {}));
		syncStats.setProgress(progress);
		logger.debug("Synchronized batch to server: " + actions.size() +
				" items");
		return actions.size();
	}

	/**
	 * Writes (part of) a log file from a mobile app to the server. On the
	 * server it will store the log file in a path user/app/device or
	 * user/app (if you don't set a device). Log files are separated by date.
	 * You should specify at what position the data should be written in the
	 * log file for the specified date.
	 * 
	 * <p>If you set a device ID, it must be a string of at most 64 characters
	 * using only alphanumeric characters and dashes (-). For example you can
	 * pass a UUID or an Android device ID.</p>
	 * 
	 * <p>Log files are written on the server in a file yyyyMMdd.log or
	 * yyyyMMdd.zip. The extension depends on the parameter "zip".</p>
	 * 
	 * @param app the mobile app
	 * @param device the device ID or null
	 * @param date the date
	 * @param position the position in the log file where the data should be
	 * written
	 * @param zip true if the log file should be written on the server with
	 * extension .zip, false if it should be written with extension .log.
	 * @param data the data
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void writeMobileLog(String app, String device, LocalDate date,
			long position, final boolean zip, final byte[] data)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery("/mobilelog/" + app, "POST", true,
				client -> {
					if (device != null && device.length() > 0)
						client.addQueryParam("device", device);
					client.addQueryParam("date", date.format(
							DateTimeFormatter.ofPattern("yyyy-MM-dd")))
						.addQueryParam("position", Long.toString(position))
						.addQueryParam("zip", Boolean.toString(zip))
						.addHeader("Content-Type", "application/octet-stream")
						.writeBytes(data)
						.readBytes();
					return null;
				});
	}
	
	/**
	 * Returns the history of a system statistic. The statistics will be sorted
	 * by time. Each statistic has a numerical value. Some statistics have an
	 * extra string value that is described at {@link
	 * nl.rrd.senseeact.client.model.SystemStat.Name SystemStat.Name}.
	 * 
	 * <p>Note that a statistic may have multiple instances at one time. For
	 * example the file store space statistics have one value for each file
	 * store.</p>
	 * 
	 * @param name the statistic name
	 * @param start start time of the statistics to return (inclusive)
	 * @param end end time of the statistics to return (exclusive)
	 * @return the statistics
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<SystemStat> getSystemStats(SystemStat.Name name,
			final ZonedDateTime start, final ZonedDateTime end)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery("/stats/" + name, "GET", true,
			client -> {
				DateTimeFormatter zonedFormat = DateTimeUtils.ZONED_FORMAT;
				if (start != null) {
					client.addQueryParam("start", start.format(zonedFormat));
				}
				if (end != null) {
					client.addQueryParam("end", end.format(zonedFormat));
				}
				List<Map<?,?>> maps = client.readJson(new TypeReference<>() {});
				List<SystemStat> result = new ArrayList<>();
				DatabaseObjectMapper mapper = new DatabaseObjectMapper();
				for (Map<?,?> map : maps) {
					result.add(mapper.mapToObject(map, SystemStat.class, true));
				}
				return result;
			});
	}
	
	/**
	 * Returns the latest value of a system statistic. Each statistic has a
	 * numerical value. Some statistics have an extra string value that is
	 * described at {@link nl.rrd.senseeact.client.model.SystemStat.Name
	 * SystemStat.Name}.
	 * 
	 * <p>Note that a statistic may have multiple instances at one time. For
	 * example the file store space statistics have one value for each file
	 * store. This method still only returns one of them.</p>
	 * 
	 * @param name the statistic name
	 * @return the latest statistic or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public SystemStat getLatestSystemStat(SystemStat.Name name)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		return runQuery("/stats/" + name + "/latest", "GET", true,
				client -> {
					Map<?,?> nullable = client.readJson(Map.class);
					Map<?,?> map = (Map<?,?>)nullable.get("value");
					if (map == null)
						return null;
					DatabaseObjectMapper mapper = new DatabaseObjectMapper();
					return mapper.mapToObject(map, SystemStat.class, true);
				});
	}

	/**
	 * Downloads a file from a configured HTTP client to the specified
	 * directory. This method gets the reponse from the HTTP client. It
	 * requires that the response contains header Content-Disposition
	 * with a filename.
	 * 
	 * @param client the HTTP client
	 * @param fileSize the file size
	 * @param dir the download directory
	 * @throws HttpClientException if the client receives a HTTP error 
	 * @throws IOException if an error occurs while communicating with the
	 * service
	 */
	public File downloadFile(HttpClient client, Long fileSize, File dir,
			DownloadProgressListener listener) throws HttpClientException,
			IOException {
		String disposition = client.getResponse().getHeaderField(
				"Content-Disposition");
		if (disposition == null)
			throw new IOException("Header Content-Disposition not found");
		Pattern regex = Pattern.compile("filename=\"(.+)\"");
		Matcher m = regex.matcher(disposition);
		if (!m.find()) {
			throw new IOException(
					"Can't find filename in Content-Disposition: " +
					disposition);
		}
		String filename = m.group(1);
		long current = 0;
		File file = new File(dir, filename);
		InputStream in = client.getInputStream();
		try (OutputStream out = new FileOutputStream(file)) {
			byte[] bs = new byte[4096];
			int len;
			while ((len = in.read(bs)) > 0) {
				out.write(bs, 0, len);
				current += len;
				if (listener != null)
					listener.onDownloadProgress(current, fileSize);
			}
		}
		return file;
	}

	/**
	 * This listener is notified about the progress of a download.
	 */
	public interface DownloadProgressListener {

		/**
		 * Called when a new data block has been downloaded. It specifies the
		 * total number of bytes downloaded so far. If the total number of bytes
		 * to download is known, it will also be specified.
		 *
		 * @param current the total number of bytes downloaded so far
		 * @param total the total number of bytes to download, or null
		 */
		void onDownloadProgress(long current, Long total);
	}

	/**
	 * Returns the regular expressions for email addresses that the specified
	 * user can access within the specified project. Not every project supports
	 * this type of access control. This method can only be called by admins.
	 *
	 * @param project the project code
	 * @param subject the user ID of the subject user
	 * @return the regular expressions
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public List<String> getUserEmailRegexAccess(String project, String subject)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		String url = String.format("/project/%s/user_access", project);
		return runQuery(url, "GET", true,
				client -> client.addQueryParam("user", subject)
						.readJson(new TypeReference<>() {}));
	}

	/**
	 * Adds a regular expression for email addresses that the specified user can
	 * access within the specified project. Not every project supports this type
	 * of access control. This method can only be called by admins.
	 *
	 * @param project the project code
	 * @param subject the user ID of the subject user
	 * @param emailRegex the regular expression
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void addUserEmailRegexAccess(String project, String subject,
			String emailRegex) throws SenSeeActClientException, HttpClientException,
			ParseException, IOException {
		String url = String.format("/project/%s/user_access", project);
		runQuery(url, "POST", true,
				client -> {
					client.addQueryParam("user", subject)
							.addQueryParam("emailRegex", emailRegex)
							.readString();
					return null;
				});
	}

	/**
	 * Removes one or all regular expressions for email addresses that the
	 * specified user can access within the specified project. Not every project
	 * supports this type of access control. This method can only be called by
	 * admins.
	 *
	 * @param project the project code
	 * @param subject the user ID of the subject user or null
	 * @param emailRegex the regular expression, or null if all regular
	 * expressions should be removed
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void removeUserEmailRegexAccess(String project,
			String subject, String emailRegex)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery(String.format("/project/%s/site_access", project), "DELETE",
				true,
				client -> {
					client.addQueryParam("user", subject);
					if (emailRegex != null)
						client.addQueryParam("emailRegex", emailRegex);
					client.readString();
					return null;
				});
	}

	/**
	 * Registers a request for the server to send push messages at a regular
	 * interval in order to wake up a mobile app.
	 *
	 * @param subject the user ID of the subject user or null
	 * @param deviceId the device ID
	 * @param fcmToken the client token of Firebase Cloud Messaging
	 * @param interval the interval in seconds (at least 60)
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void registerMobileWake(String subject, String deviceId,
			String fcmToken, int interval) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		runQuery("/mobile/wake/register", "POST", true,
				client -> {
					if (subject != null)
						client.writePostParam("user", subject);
					client.writePostParam("deviceId", deviceId)
						.writePostParam("fcmToken", fcmToken)
						.writePostParam("interval", Integer.toString(interval))
						.readString();
					return null;
				});
	}

	/**
	 * Unregisters a request for the server to send push messages to wake up a
	 * mobile app.
	 *
	 * @param subject the user ID of the subject user or null
	 * @param deviceId the device ID
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public void unregisterMobileWake(String subject, String deviceId)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		runQuery("/mobile/wake/unregister", "POST", true,
				client -> {
					if (subject != null)
						client.writePostParam("user", subject);
					client.writePostParam("deviceId", deviceId)
						.readString();
					return null;
				});
	}

	/**
	 * Gets the authentication headers for the current user. If no user logged
	 * in, this method throws an exception.
	 * 
	 * @return the authentication token
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	private List<AuthHeader> getAuthHeaders() throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		if (authHeaders != null) {
			return authHeaders;
		} else {
			throw new SenSeeActClientException(401, "Unauthorized",
					new HttpError(ErrorCode.AUTH_TOKEN_NOT_FOUND,
					"Authentication token not found"));
		}
	}

	/**
	 * Runs a SenSeeAct query. If the query requires authentication, you should
	 * set "authenticate" to true. Then this method will add the authentication
	 * token. For other queries you should set "authenticate" to false, because
	 * it may not be possible to add an authentication token (for example for
	 * a login or signup).
	 * 
	 * @param action the action. This is appended to the base URL and should
	 * start with a slash.
	 * @param method the HTTP method (e.g. GET or POST)
	 * @param authenticate true if the query requires authentication, false
	 * otherwise
	 * @param runner the query runner
	 * @return the query result
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	protected <T> T runQuery(String action, String method, boolean authenticate,
			SenSeeActQueryRunner<T> runner) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		HttpClient client = getHttpClient(action, method, authenticate);
		try {
			T result = runner.runQuery(client);
			responseHeaders = client.getResponseHeaders();
			return result;
		} catch (HttpClientException httpEx) {
			ObjectMapper mapper = new ObjectMapper();
			HttpError error;
			try {
				error = mapper.readValue(httpEx.getErrorContent(),
						HttpError.class);
			} catch (JsonProcessingException parseEx) {
				error = null;
			}
			if (error == null)
				throw httpEx;
			throw new SenSeeActClientException(httpEx.getStatusCode(),
					httpEx.getStatusMessage(), error);
		} finally {
			closeHttpClient(client);
		}
	}
	
	/**
	 * Creates an HTTP client for a new SenSeeAct query. This method is used in
	 * {@link #runQuery(String, String, boolean, SenSeeActQueryRunner)
	 * runQuery()}. When the client is no longer needed, it should be closed
	 * with {@link #closeHttpClient(HttpClient) closeHttpClient()}.
	 * 
	 * @param action the action. This is appended to the base URL and should
	 * start with a slash.
	 * @param method the HTTP method (e.g. GET or POST)
	 * @param authenticate true if the query requires authentication, false
	 * otherwise
	 * @return the HTTP client
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	private HttpClient getHttpClient(String action, String method,
			boolean authenticate) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		return getHttpClientForUrl(baseUrl + "/v" + PROTOCOL_VERSION + action,
				method, authenticate);
	}

	/**
	 * Creates an HTTP client for a new SenSeeAct query. In contrast to {@link
	 * #getHttpClient(String, String, boolean) getHttpClient()}, this method
	 * takes a complete URL, so it can also be used for SenSeeAct project
	 * services (see {@link SenSeeActProjectClient SenSeeActProjectClient}).
	 * When the client is no longer needed, it should be closed with {@link
	 * #closeHttpClient(HttpClient) closeHttpClient()}.
	 *
	 * @param url the URL
	 * @param method the HTTP method (e.g. GET or POST)
	 * @param authenticate true if the query requires authentication, false
	 * otherwise
	 * @return the HTTP client
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	protected HttpClient getHttpClientForUrl(String url, String method,
			boolean authenticate) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		HttpClient client = new HttpClient(url);
		client.setMethod(method);
		if (authenticate) {
			List<AuthHeader> headers = getAuthHeaders();
			for (AuthHeader header : headers) {
				client.addHeader(header.getName(), header.getValue());
			}
		}
		synchronized (lock) {
			if (closed)
				throw new IOException("RRDSenSeeActClient closed");
			activeClients.add(client);
		}
		return client;
	}
	
	/**
	 * Closes the specified HTTP client. It's also removed from the list of
	 * active clients. This method should be called after {@link
	 * #getHttpClient(String, String, boolean) getHttpClient()} when the client
	 * is no longer needed.
	 * 
	 * @param client the HTTP client
	 */
	protected void closeHttpClient(HttpClient client) {
		synchronized (lock) {
			if (closed)
				return;
			activeClients.remove(client);
			client.close();
		}
	}
}
