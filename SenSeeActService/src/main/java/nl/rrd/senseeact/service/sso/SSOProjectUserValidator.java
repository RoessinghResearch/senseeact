package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This validator is used for tokens that grant complete access within certain
 * projects (the "tokenProjects"). It checks the following constraints:
 *
 * <p><ul>
 * <li>A user with the specified user ID or email address exists</li>
 * <li>The user is not an admin.</li>
 * <li>If the endpoint is related to a project, then it must be one of the
 * token projects.</li>
 * <li>The user must be a member of the token projects.</li>
 * </ul></p>
 *
 * @author Dennis Hofs (RRD)
 */
public class SSOProjectUserValidator implements SSOUserValidator {
	private List<String> tokenProjects;
	private String requestedProject;

	/**
	 * Constructs a new validator.
	 *
	 * @param tokenProjects the token projects
	 * @param requestedProject the requested project or null (if the request is
	 * not related to a project)
	 */
	public SSOProjectUserValidator(List<String> tokenProjects,
			String requestedProject) {
		this.tokenProjects = tokenProjects;
		this.requestedProject = requestedProject;
	}

	@Override
	public User findAuthenticatedUser(ProtocolVersion version,
			HttpServletResponse response, Database authDb, String subject)
			throws HttpException, Exception {
		if (requestedProject != null && !tokenProjects.contains(
				requestedProject)) {
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
		if (user.getRole() == Role.PROFESSIONAL && requestedProject == null) {
			return null;
		}
		Set<String> matchProjects = new HashSet<>(user.findProjects(authDb));
		matchProjects.retainAll(tokenProjects);
		if (matchProjects.isEmpty())
			return null;
		return user;
	}
}
