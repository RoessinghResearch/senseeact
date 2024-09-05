package nl.rrd.senseeact.client.exception;

/**
 * Possible error codes that may be returned by the SenSeeAct service.
 * 
 * @author Dennis Hofs (RRD)
 */
public class ErrorCode {
	public static final String AUTH_TOKEN_NOT_FOUND = "AUTH_TOKEN_NOT_FOUND";
	public static final String AUTH_TOKEN_INVALID = "AUTH_TOKEN_INVALID";
	public static final String AUTH_TOKEN_EXPIRED = "AUTH_TOKEN_EXPIRED";

	public static final String AUTH_MFA_TYPE_MAX = "AUTH_MFA_TYPE_MAX";
	public static final String AUTH_MFA_ADD_MAX = "AUTH_MFA_ADD_MAX";
	public static final String AUTH_MFA_VERIFY_MAX = "AUTH_MFA_VERIFY_MAX";

	public static final String INVALID_INPUT = "INVALID_INPUT";

	public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
	public static final String ACCOUNT_BLOCKED = "ACCOUNT_BLOCKED";
	public static final String ACCOUNT_INACTIVE = "ACCOUNT_INACTIVE";
	public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
	public static final String GROUP_ALREADY_EXISTS = "GROUP_ALREADY_EXISTS";
	public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
}
