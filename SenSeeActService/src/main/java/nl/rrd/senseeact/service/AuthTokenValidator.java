package nl.rrd.senseeact.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.controller.AuthControllerExecution;
import nl.rrd.senseeact.service.controller.ProjectControllerExecution;
import nl.rrd.senseeact.service.exception.ExpiredAuthTokenException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.exception.InvalidAuthTokenException;
import nl.rrd.senseeact.service.exception.UnauthorizedException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;
import nl.rrd.senseeact.service.sso.SSOToken;
import nl.rrd.senseeact.service.sso.SSOTokenRepository;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import org.slf4j.Logger;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * This class can validate whether an authentication token is valid in a certain
 * context. The token can be the general token or an SSO token for a specific
 * project.
 *
 * @author Dennis Hofs (RRD)
 */
public class AuthTokenValidator {
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
	 * tokens for specific projects. For SSO tokens, in addition to project
	 * restrictions, it checks the user role. You cannot authenticate as an
	 * admin using an SSO token. And if you authenticate as a professional, you
	 * can only access project data, and not for example the user profile or
	 * user access functions.</p>
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
	 * @param context the query context
	 * @return the authenticated user
	 * @throws UnauthorizedException if no token is specified, or the token is
	 * empty or invalid
	 * @throws DatabaseException if a database error occurs
	 */
	public static ValidateTokenResult validate(ProtocolVersion version,
			HttpServletRequest request, HttpServletResponse response,
			Database authDb, QueryContext context) throws HttpException,
			Exception {
		ValidateTokenResult result = getAuthenticatedUser(version, request,
				response, authDb, context);
		if (!result.getUser().isActive()) {
			throw new UnauthorizedException(ErrorCode.ACCOUNT_INACTIVE,
					"Account has been deactivated");
		}
		AuthDetails authDetails = result.getAuthDetails();
		if (!context.isAllowPendingMfa() && authDetails != null &&
				authDetails.isPendingMfa()) {
			throw new UnauthorizedException(ErrorCode.PENDING_MFA,
					"Multi-factor authentication not completed");
		}
		if (context.getProject() != null) {
			ProjectControllerExecution.findUserProject(context.getProject(),
					authDb, result.getUser());
		}
		if (authDetails != null && authDetails.isAutoExtendCookie()) {
			ZonedDateTime now = DateTimeUtils.nowMs();
			AuthToken.createToken(version, result.getUser(),
					authDetails.isPendingMfa(), authDetails.getMfaId(), now,
					authDetails.toExpireMinutes(), true, true, response);
		}
		User user = result.getUser();
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			UserCache userCache = UserCache.getInstance();
			ZonedDateTime now = DateTimeUtils.nowMs(user.toTimeZone());
			user.setLastActive(now);
			userCache.updateUser(authDb, user);
		}
		return result;
	}

	private static ValidateTokenResult getAuthenticatedUser(
			ProtocolVersion version, HttpServletRequest request,
			HttpServletResponse response, Database authDb, QueryContext context)
			throws HttpException, Exception {
		String token = request.getHeader("X-Auth-Token");
		if (token != null)
			return validateDefaultToken(token);
		SSOTokenRepository ssoRepo = AppComponents.get(
				SSOTokenRepository.class);
		for (SSOToken ssoToken : ssoRepo.getTokens()) {
			if (ssoToken.requestHasToken(request)) {
				return ssoToken.validateToken(version, request, response,
						authDb, context.getProject());
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
	private static ValidateTokenResult validateDefaultToken(String token)
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
		if (!details.isPendingMfa() && user.hasVerifiedMfaRecord()) {
			String mfaId = details.getMfaId();
			if (mfaId == null) {
				logger.info("Invalid auth token: Not authenticated with MFA");
				throw new UnauthorizedException(
						ErrorCode.AUTH_TOKEN_INVALID_MFA,
						"Authentication token invalid");
			}
			if (user.findVerifiedMfaRecord(mfaId) == null) {
				logger.info("Invalid auth token: MFA record ID not found");
				throw new UnauthorizedException(
						ErrorCode.AUTH_TOKEN_INVALID_MFA,
						"Authentication token invalid");
			}
		}
		return new ValidateTokenResult(user, details);
	}
}
