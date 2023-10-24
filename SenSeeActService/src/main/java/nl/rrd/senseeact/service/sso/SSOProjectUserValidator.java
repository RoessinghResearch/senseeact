package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;

/**
 * This validator is used for tokens that grant complete access to data
 * within a project (the "tokenProject"). It checks the following
 * constraints:
 *
 * <p><ul>
 * <li>A user with the specified user ID or email address exists</li>
 * <li>The user is not an admin.</li>
 * <li>If the endpoint is related to a project, then the requested project
 * must equal the token project.</li>
 * <li>The user must be a member of the specified project.</li>
 * </ul></p>
 *
 * @author Dennis Hofs (RRD)
 */
public class SSOProjectUserValidator implements SSOUserValidator {
	private String tokenProject;
	private String requestedProject;

	public SSOProjectUserValidator(String tokenProject,
			String requestedProject) {
		this.tokenProject = tokenProject;
		this.requestedProject = requestedProject;
	}

	@Override
	public User findAuthenticatedUser(ProtocolVersion version,
			HttpServletResponse response, Database authDb, String subject)
			throws HttpException, Exception {
		if (requestedProject != null &&
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
		if (user.getRole() == Role.PROFESSIONAL && requestedProject == null)
			return null;
		if (!user.findProjects(authDb).contains(tokenProject))
			return null;
		return user;
	}
}
