package nl.rrd.senseeact.client.model;

import nl.rrd.utils.json.JsonObject;

/**
 * Result of query loginUsername(), which allows users to log in using only the
 * local part of the email address. The result contains the complete email
 * address along with the user ID and the authentication token.
 * 
 * @author Dennis Hofs (RRD)
 */
public class LoginUsernameResult extends JsonObject {
	private String user;
	private String token;
	private String email;

	/**
	 * Default constructor used by the JSON object mapper.
	 */
	public LoginUsernameResult() {
	}
	
	/**
	 * Constructs a new result.
	 *
	 * @param user the user ID
	 * @param token the authentication token
	 * @param email the complete email address
	 */
	public LoginUsernameResult(String user, String token, String email) {
		this.user = user;
		this.token = token;
		this.email = email;
	}

	/**
	 * Returns the user ID.
	 *
	 * @return the user ID
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the user ID.
	 *
	 * @param user the user ID
	 */
	public void setUser(String user) {
		this.user = user;
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
