package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;

/**
 * This validator is used for tokens that grant complete access within a
 * project (the "tokenProject") or any project. It checks the following
 * constraints:
 *
 * <p><ul>
 * <li>A user with the specified user ID or email address exists</li>
 * <li>The user is not an admin.</li>
 * <li>If the endpoint is related to a project and the token project is set,
 * then the requested project must equal the token project.</li>
 * <li>If a token project is set, the user must be a member of the specified
 * project.</li>
 * </ul></p>
 *
 * @author Dennis Hofs (RRD)
 */
public class SSOProjectUserValidator implements SSOUserValidator {
	private String tokenProject;
	private String requestedProject;

	/**
	 * Constructs a new validator.
	 *
	 * @param tokenProject the token project or null (if the token is valid for
	 * any project)
	 * @param requestedProject the requested project or null (if the request is
	 * not related to a project)
	 */
	public SSOProjectUserValidator(String tokenProject,
			String requestedProject) {
		this.tokenProject = tokenProject;
		this.requestedProject = requestedProject;
	}

	@Override
	public User findAuthenticatedUser(ProtocolVersion version,
			HttpServletResponse response, Database authDb, String subject)
			throws HttpException, Exception {
		if (tokenProject != null && requestedProject != null &&
				!requestedProject.equals(tokenProject)) {
			return null;
		}
		UserCache userCache = UserCache.getInstance();
		User user;
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal() &&
				!subject.contains("@")) {
			user = userCache.findByUserid(subject);
		} else {
			user = userCache.findByEmail(subject);
		}
		if (user == null)
			return null;
		if (user.getRole() == Role.ADMIN)
			return null;
		if (user.getRole() == Role.PROFESSIONAL && (tokenProject == null ||
				requestedProject == null)) {
			return null;
		}
		if (tokenProject != null && !user.findProjects(authDb).contains(
				tokenProject)) {
			return null;
		}
		return user;
	}
}
