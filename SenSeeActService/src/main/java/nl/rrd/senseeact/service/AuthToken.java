package nl.rrd.senseeact.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.service.exception.ExpiredAuthTokenException;
import nl.rrd.senseeact.service.exception.InvalidAuthTokenException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;

import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;

/**
 * This class can create or parse a signed Base64 JWT token string. This is
 * a stateless authentication because no tokens or session keys need to be
 * saved in the database. When users log in, they will get a token string.
 * At every request they should authenticate with the token string. The token
 * encodes an instance of {@link AuthDetails AuthDetails}, which defines the
 * user identity and token validity.
 * 
 * @author Dennis Hofs (RRD)
 */
public class AuthToken {
	private static final String VERSION = "version";
	private static final String HASH = "hash";
	private static final String PENDING_MFA = "pendingMfa";
	private static final String MFA_ID = "mfaId";
	private static final String COOKIE = "cookie";
	private static final String AUTO_EXTEND_COOKIE = "autoExtendCookie";

	public static String createToken(ProtocolVersion version, User user,
			boolean pendingMfa, String mfaId, ZonedDateTime now,
			Integer expireMinutes, boolean cookie, boolean autoExtendCookie,
			HttpServletResponse response) {
		ZonedDateTime expiration = createExpiration(now, expireMinutes);
		Date tokenExpire = createTokenExpiration(expiration);
		ZonedDateTime cookieExpire = createCookieExpiration(now, expiration);
		if (!cookie)
			autoExtendCookie = false;
		AuthDetails details;
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal()) {
			details = AuthDetails.forUserid(user.getUserid(),
					Date.from(now.toInstant()), tokenExpire,
					AuthDetails.hashSalt(user.getSalt()), pendingMfa, mfaId,
					cookie, autoExtendCookie);
		} else {
			details = AuthDetails.forEmail(user.getEmail(),
					Date.from(now.toInstant()), tokenExpire,
					AuthDetails.hashSalt(user.getSalt()), pendingMfa, mfaId,
					cookie, autoExtendCookie);
		}
		String token = AuthToken.createToken(details);
		if (cookie)
			setAuthTokenCookie(response, token, cookieExpire);
		return token;
	}

	private static void setAuthTokenCookie(HttpServletResponse response,
			String token, ZonedDateTime expires) {
		Configuration config = AppComponents.get(Configuration.class);
		String baseUrl = config.get(Configuration.BASE_URL);
		SenSeeActCookie.setAuthTokenCookie(baseUrl, response, token, expires);
	}

	/**
	 * Creates the signed Base64 JWT token string for the specified
	 * authentication details.
	 * 
	 * @param details the authentication details
	 * @return the token string
	 */
	private static String createToken(AuthDetails details) {
		byte[] keyBs = getSecretKey();
		JWSSigner signer;
		try {
			signer = new MACSigner(keyBs);
		} catch (JOSEException ex) {
			throw new RuntimeException("Invalid secret key: " + ex.getMessage(),
					ex);
		}
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
		if (details.getUserid() != null)
			claims.subject(details.getUserid());
		else
			claims.subject(details.getEmail());
		claims.issueTime(details.getIssuedAt());
		if (details.getExpiration() != null)
			claims.expirationTime(details.getExpiration());
		claims.claim(HASH, details.getHash());
		if (details.getUserid() != null)
			claims.claim(VERSION, 1);
		claims.claim(PENDING_MFA, details.isPendingMfa());
		if (details.getMfaId() != null)
			claims.claim(MFA_ID, details.getMfaId());
		claims.claim(COOKIE, details.isCookie());
		claims.claim(AUTO_EXTEND_COOKIE, details.isAutoExtendCookie());
		JWSAlgorithm algorithm;
		if (keyBs.length >= 64)
			algorithm = JWSAlgorithm.HS512;
		else
			algorithm = JWSAlgorithm.HS256;
		SignedJWT signedJwt = new SignedJWT(new JWSHeader(algorithm),
				claims.build());
		try {
			signedJwt.sign(signer);
		} catch (JOSEException ex) {
			throw new RuntimeException("Failed to sign token: " +
					ex.getMessage(), ex);
		}
		return signedJwt.serialize();
	}
	
	/**
	 * Parses the specified signed Base64 JWT token string and returns the
	 * authentication details. If the token can't be parsed, this method
	 * throws an exception.
	 * 
	 * @param token the token
	 * @return the authentication details
	 * @throws ExpiredAuthTokenException if the token expired
	 * @throws InvalidAuthTokenException if the token is invalid
	 */
	public static AuthDetails parseToken(String token)
			throws InvalidAuthTokenException, ExpiredAuthTokenException {
		ZonedDateTime now = DateTimeUtils.nowMs();
		ZoneId tz = now.getZone();
		JWSVerifier verifier;
		try {
			verifier = new MACVerifier(getSecretKey());
		} catch (JOSEException ex) {
			throw new RuntimeException("Invalid secret key: " + ex.getMessage(),
					ex);
		}
		boolean verified;
		JWTClaimsSet claims;
		try {
			SignedJWT signedJwt = SignedJWT.parse(token);
			verified = signedJwt.verify(verifier);
			claims = signedJwt.getJWTClaimsSet();
		} catch (ParseException | JOSEException ex) {
			throw new InvalidAuthTokenException("Invalid token: " +
					ex.getMessage(), ex);
		}
		if (!verified)
			throw new InvalidAuthTokenException("Invalid token");
		if (claims.getExpirationTime() != null) {
			ZonedDateTime expires = ZonedDateTime.ofInstant(
					claims.getExpirationTime().toInstant(), tz);
			if (!now.isBefore(expires)) {
				throw new ExpiredAuthTokenException(String.format(
						"Token expired: %s is not before %s",
						now.format(DateTimeUtils.ZONED_FORMAT),
						expires.format(DateTimeUtils.ZONED_FORMAT)));
			}
		}
		Integer version;
		Boolean pendingMfa;
		String mfaId;
		Boolean cookie;
		Boolean autoExtendCookie;
		String hash;
		try {
			version = claims.getIntegerClaim(VERSION);
			if (version == null)
				version = 0;
			pendingMfa = claims.getBooleanClaim(PENDING_MFA);
			if (pendingMfa == null)
				pendingMfa = false;
			mfaId = claims.getStringClaim(MFA_ID);
			cookie = claims.getBooleanClaim(COOKIE);
			if (cookie == null)
				cookie = false;
			autoExtendCookie = claims.getBooleanClaim(AUTO_EXTEND_COOKIE);
			if (autoExtendCookie == null)
				autoExtendCookie = false;
			hash = claims.getStringClaim(HASH);
		} catch (ParseException ex) {
			throw new InvalidAuthTokenException("Invalid claims: " + claims +
					": " + ex.getMessage(), ex);
		}
		if (version == 1) {
			return AuthDetails.forUserid(claims.getSubject(),
					claims.getIssueTime(), claims.getExpirationTime(),
					hash, pendingMfa, mfaId, cookie, autoExtendCookie);
		} else {
			return AuthDetails.forEmail(claims.getSubject(),
					claims.getIssueTime(), claims.getExpirationTime(),
					hash, pendingMfa, mfaId, cookie, autoExtendCookie);
		}
	}

	/**
	 * Gets the secret key by parsing the Base64 string in property
	 * jwtSecretKey in the configuration.
	 * 
	 * @return the secret key
	 */
	private static byte[] getSecretKey() {
		String base64Key = AppComponents.get(Configuration.class).get(
				Configuration.JWT_SECRET_KEY);
		return Base64.getDecoder().decode(base64Key);
	}

	/**
	 * Returns the date/time when a token should expire. This is the current
	 * time plus the expiration time in minutes. If no expiration time is
	 * specified, this method returns null, which means that the token should
	 * never expire.
	 *
	 * @param now the current date/time
	 * @param expiration the expiration time in minutes or null
	 * @return the expiration date/time or null
	 */
	private static ZonedDateTime createExpiration(ZonedDateTime now,
			Integer expiration) {
		if (expiration == null)
			return null;
		else
			return now.plusMinutes(expiration);
	}

	/**
	 * Converts the specified expiration date/time to an expiration date/time
	 * for a JWT token. If the specified time is null, this method returns null,
	 * meaning that the token should never expire.
	 *
	 * @param expiration the expiration date/time, or null if the token should
	 * never expire
	 * @return the JWT token expiration date/time or null
	 */
	private static Date createTokenExpiration(ZonedDateTime expiration) {
		if (expiration == null)
			return null;
		else
			return Date.from(expiration.toInstant());
	}

	/**
	 * Converts the specified expiration date/time to an expiration date/time
	 * for the "authToken" cookie. If the specified time is null, meaning that
	 * it should never expire, this method returns a time one year from now. A
	 * cookie cannot be set to never expire.
	 *
	 * @param expiration the expiration date/time, or null if the token should
	 * never expire
	 * @return the cookie expiration date/time
	 */
	private static ZonedDateTime createCookieExpiration(ZonedDateTime now,
			ZonedDateTime expiration) {
		if (expiration == null)
			return now.plusYears(1);
		else
			return expiration;
	}
}
