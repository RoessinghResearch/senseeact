package nl.rrd.senseeact.service;

/**
 * This defines the context of a query that is used to check wether an
 * authentication token is valid in that context.
 *
 * @author Dennis Hofs (RRD)
 */
public class QueryContext {
	private String project = null;
	private boolean allowPendingMfa = false;

	/**
	 * If the query accesses project data, this method returns the project code.
	 * For other requests this should be null (default).
	 *
	 * <p>If set, the token validator will check if the user is a member of the
	 * specified project. In case of an SSO token it also checks whether the
	 * project is the same as the SSO project.</p>
	 *
	 * @return a project code or null
	 */
	public String getProject() {
		return project;
	}

	/**
	 * If the request accesses project data, you should set the project code
	 * with this method. For other requests this should be null (default).
	 *
	 * <p>If set, the token validator will check if the user is a member of the
	 * specified project. In case of an SSO token it also checks whether the
	 * project is the same as the SSO project.</p>
	 *
	 * @param project a project code or null
	 * @return this context
	 */
	public QueryContext setProject(String project) {
		this.project = project;
		return this;
	}

	/**
	 * Sets whether the query is allowed if the user enabled multi-factor
	 * authentication and the user only entered the password, but didn't enter
	 * the second factor yet. By default this is not allowed. The only case when
	 * this should be allowed is to verify the second factor.
	 *
	 * @return true if the request is allowed even if a second factor
	 * authentication is still needed
	 */
	public boolean isAllowPendingMfa() {
		return allowPendingMfa;
	}

	/**
	 * Sets whether the query is allowed if the user enabled multi-factor
	 * authentication and the user only entered the password, but didn't enter
	 * the second factor yet. By default this is not allowed. The only case
	 * when this should be allowed is to verify the second factor.
	 *
	 * @param allowPendingMfa true if the request is allowed even if a second
	 * factor authentication is still needed
	 * @return this context
	 */
	public QueryContext setAllowPendingMfa(boolean allowPendingMfa) {
		this.allowPendingMfa = allowPendingMfa;
		return this;
	}
}
