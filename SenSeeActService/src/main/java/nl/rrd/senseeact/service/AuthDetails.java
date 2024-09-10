package nl.rrd.senseeact.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * The authentication details that are included in a JWT token. It contains the
 * user ID or email address (for legacy tokens) of the authenticated user.
 * Furthermore it contains the date/time when the JWT token was issued,
 * the date/time when the JWT token expires (if any), and a hash of the salt in
 * the database. The latter is used to invalidate old tokens when users change
 * their password. The hash can be created with {@link #hashSalt(String)
 * hashSalt()}.
 * 
 * @author Dennis Hofs (RRD)
 */
public class AuthDetails {
	private String userid = null;
	private String email = null;
	private Date issuedAt;
	private Date expiration;
	private String hash;
	private boolean pendingMfa = false;
	private String mfaId = null;
	private boolean cookie = false;
	private boolean autoExtendCookie = false;

	private AuthDetails() {
	}

	/**
	 * Constructs a new instance with the user ID of the authenticated user.
	 *
	 * @param userid the user ID of the authenticated user
	 * @param issuedAt the date/time when the JWT token was issued, with
	 * precision of seconds. Any milliseconds are discarded.
	 * @param expiration the date/time when the JWT token expires, with
	 * precision of seconds. Any milliseconds are discarded. This can be null
	 * if the token never expires.
	 * @param hash hash of the salt in the database. This makes old tokens
	 * invalid when users change their password.
	 * @param pendingMfa true if the user enabled multi-factor authentication
	 * and the user only entered the password, but didn't enter the second
	 * factor yet
	 * @param mfaId if the user authenticated with MFA, this should be set to
	 * the used MFA record ID
	 * @param cookie true if the "authToken" cookie should be set
	 * @param autoExtendCookie true if the "authToken" cookie should be
	 * automatically extended at every verification
	 */
	public static AuthDetails forUserid(String userid, Date issuedAt,
			Date expiration, String hash, boolean pendingMfa, String mfaId,
			boolean cookie, boolean autoExtendCookie) {
		AuthDetails details = new AuthDetails();
		details.userid = userid;
		details.issuedAt = issuedAt;
		details.expiration = expiration;
		details.hash = hash;
		details.pendingMfa = pendingMfa;
		details.mfaId = mfaId;
		details.cookie = cookie;
		details.autoExtendCookie = autoExtendCookie;
		return details;
	}

	/**
	 * Constructs a new instance with the email address of the authenticated
	 * user. This is used for legacy tokens.
	 *
	 * @param email the email address of the authenticated user
	 * @param issuedAt the date/time when the JWT token was issued, with
	 * precision of seconds. Any milliseconds are discarded.
	 * @param expiration the date/time when the JWT token expires, with
	 * precision of seconds. Any milliseconds are discarded. This can be null
	 * if the token never expires.
	 * @param hash hash of the salt in the database. This makes old tokens
	 * invalid when users change their password.
	 * @param pendingMfa true if the user enabled multi-factor authentication
	 * and the user only entered the password, but didn't enter the second
	 * factor yet
	 * @param mfaId if the user authenticated with MFA, this should be set to
	 * the used MFA record ID
	 * @param cookie true if the "authToken" cookie should be set
	 * @param autoExtendCookie true if the "authToken" cookie should be
	 * automatically extended at every verification
	 */
	public static AuthDetails forEmail(String email, Date issuedAt,
			Date expiration, String hash, boolean pendingMfa, String mfaId,
			boolean cookie, boolean autoExtendCookie) {
		AuthDetails details = new AuthDetails();
		details.email = email;
		details.issuedAt = issuedAt;
		details.expiration = expiration;
		details.hash = hash;
		details.pendingMfa = pendingMfa;
		details.mfaId = mfaId;
		details.cookie = cookie;
		details.autoExtendCookie = autoExtendCookie;
		return details;
	}

	/**
	 * Returns the user ID of the authenticated user. For a legacy token this
	 * is null and the email address is set.
	 * 
	 * @return the user ID of the authenticated user or null
	 */
	public String getUserid() {
		return userid;
	}

	/**
	 * Returns the email address of the authenticated user. This is set for a
	 * legacy token. Otherwise this is null and the user ID is set.
	 *
	 * @return the email address of the authenticated user or null
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * The date/time when the JWT token was issued, with precision of seconds.
	 * The token becomes invalid when the tokenValidIssueTime property in the
	 * user table is set to a later time, for example when the password is
	 * changed.
	 * 
	 * @return the date/time when the JWT token was issued, with precision of
	 * seconds
	 */
	public Date getIssuedAt() {
		return issuedAt;
	}

	/**
	 * Returns the date/time when the JWT token expires, with precision of
	 * seconds. This can be null if the token never expires.
	 * 
	 * @return the date/time when the JWT token expires, with precision of
	 * seconds, or null
	 */
	public Date getExpiration() {
		return expiration;
	}

	/**
	 * Returns the hash of the salt in the database. This makes old tokens
	 * invalid when users change their password.
	 * 
	 * @return the hash
	 */
	public String getHash() {
		return hash;
	}

	/**
	 * Returns true if the user enabled multi-factor authentication and the user
	 * only entered the password, but didn't enter the second factor yet.
	 *
	 * @return true if this token still needs authentication with a second
	 * factor, false otherwise
	 */
	public boolean isPendingMfa() {
		return pendingMfa;
	}

	/**
	 * If the user authenticated with MFA, then this method returns the used MFA
	 * record ID.
	 *
	 * @return the MFA record ID or null
	 */
	public String getMfaId() {
		return mfaId;
	}

	/**
	 * Returns true if the "authToken" cookie should be set.
	 *
	 * @return true if the "authToken" cookie should be set
	 */
	public boolean isCookie() {
		return cookie;
	}

	/**
	 * Returns true if the "authToken" cookie should be automatically extended
	 * at every verification.
	 *
	 * @return true if the "authToken" cookie should be automatically extended
	 * at every verification, false otherwise
	 */
	public boolean isAutoExtendCookie() {
		return autoExtendCookie;
	}

	/**
	 * Returns the expiration in minutes. This is the difference between
	 * "issuedAt" and "expiration". If "expiration" is null, then this method
	 * also returns null.
	 *
	 * @return the expiration in minutes or null
	 */
	public Integer toExpireMinutes() {
		if (expiration == null)
			return null;
		return (int)ChronoUnit.MINUTES.between(issuedAt.toInstant(),
				expiration.toInstant());
	}

	/**
	 * Creates a hash of the specified salt string. The salt string should be
	 * a Base64 string. The returned hash is also a Base64 string.
	 * 
	 * @param salt the salt Base64 string
	 * @return the hash Base64 string
	 */
	public static String hashSalt(String salt) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException("Algorithm SHA-256 not found: " +
					ex.getMessage(), ex);
		}
		byte[] saltBytes = Base64.getDecoder().decode(salt);
		byte[] hash = md.digest(saltBytes);
		return Base64.getEncoder().encodeToString(hash);
	}
}
