package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import nl.rrd.senseeact.client.SenSeeActClient;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LoginResult {
	public enum Status {
		COMPLETE,
		REQUIRES_MFA
	}

	private Status status;
	@JsonInclude(content=JsonInclude.Include.NON_NULL)
	private String user = null;
	@JsonInclude(content=JsonInclude.Include.NON_NULL)
	private String email = null;
	@JsonInclude(content=JsonInclude.Include.NON_NULL)
	private String token = null;
	@JsonInclude(content=JsonInclude.Include.NON_NULL)
	private MfaRecord mfaRecord = null;

	/**
	 * Returns the status of the login: COMPLETE or REQUIRES_MFA. The latter
	 * is returned if you log in as a user that has a verified multi-factor
	 * authentication record. The login automatically triggered the default
	 * MFA record, which is included with this object. See {@link
	 * SenSeeActClient#requestMfaVerification(String)
	 * SenSeeActClient.requestMfaVerification()} for more information.
	 *
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Sets the status of the login: COMPLETE or REQUIRES_MFA. The latter should
	 * be set if you log in as a user that has a verified multi-factor
	 * authentication record. The login automatically triggered the default MFA
	 * record, which should be included with this object. See {@link
	 * SenSeeActClient#requestMfaVerification(String)
	 * SenSeeActClient.requestMfaVerification()} for more information.
	 *
	 * @param status the status
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * Returns the user ID of the user who logged in.
	 *
	 * @return the user ID of the user who logged in
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the user ID of the user who logged in.
	 *
	 * @param user the user ID of the user who logged in
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Returns the email address of the user who logged in.
	 *
	 * @return the email address of the user who logged in
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the email address of the user who logged in.
	 *
	 * @param email the email address of the user who logged in
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Returns the authentication token. If the status is {@link
	 * Status#REQUIRES_MFA REQUIRES_MFA}, then the token can only be used to
	 * perform an additional authentication against a multi-factor
	 * authentication record.
	 *
	 * @return the authentication token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Sets the authentication token. If the status is {@link
	 * Status#REQUIRES_MFA REQUIRES_MFA}, then the token can only be used to
	 * perform an additional authentication against a multi-factor
	 * authentication record.
	 *
	 * @param token the authentication token
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * If the status is {@link Status#REQUIRES_MFA REQUIRES_MFA}, then the login
	 * triggered the default multi-factor authentication record. This method
	 * returns that record. Otherwise it returns null. See {@link
	 * SenSeeActClient#requestMfaVerification(String)
	 * SenSeeActClient.requestMfaVerification()} for more information.
	 *
	 * @return the default MFA record or null
	 */
	public MfaRecord getMfaRecord() {
		return mfaRecord;
	}

	/**
	 * If the status is {@link Status#REQUIRES_MFA REQUIRES_MFA}, then the login
	 * triggered the default multi-factor authentication record. You should set
	 * that record with this method. Otherwise it should be null (default). See
	 * {@link SenSeeActClient#requestMfaVerification(String)
	 * SenSeeActClient.requestMfaVerification()} for more information.
	 *
	 * @param mfaRecord the default MFA record or null
	 */
	public void setMfaRecord(MfaRecord mfaRecord) {
		this.mfaRecord = mfaRecord;
	}
}
