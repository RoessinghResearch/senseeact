package nl.rrd.senseeact.client.model.compat;

import nl.rrd.utils.json.JsonObject;

/**
 * Result of query loginUsername(), which allows users to log in using only the
 * local part of the email address. The result contains the complete email
 * address and the authentication token.
 * 
 * @author Dennis Hofs (RRD)
 */
public class LoginUsernameResultV0 extends JsonObject {
	private String email;
	private String token;

	/**
	 * Default constructor used by the JSON object mapper.
	 */
	public LoginUsernameResultV0() {
	}

	/**
	 * Constructs a new result.
	 *
	 * @param email the complete email address
	 * @param token the authentication token
	 */
	public LoginUsernameResultV0(String email, String token) {
		this.email = email;
		this.token = token;
	}

	/**
	 * Returns the complete email address.
	 * 
	 * @return the complete email address
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the complete email address.
	 * 
	 * @param email the complete email address
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Returns the authentication token.
	 * 
	 * @return the authentication token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Sets the authentication token.
	 * 
	 * @param token the authentication token
	 */
	public void setToken(String token) {
		this.token = token;
	}
}
