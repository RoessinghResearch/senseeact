package nl.rrd.senseeact.service;

import nl.rrd.senseeact.service.model.User;

public class ValidateTokenResult {
	private User user;
	private AuthDetails authDetails;

	public ValidateTokenResult(User user, AuthDetails authDetails) {
		this.user = user;
		this.authDetails = authDetails;
	}

	/**
	 * Returns the authenticated user.
	 *
	 * @return the authenticated user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * If the token was a default token, then this method returns additional
	 * details. For other tokens this will be null.
	 *
	 * @return the authentication details or null
	 */
	public AuthDetails getAuthDetails() {
		return authDetails;
	}
}
