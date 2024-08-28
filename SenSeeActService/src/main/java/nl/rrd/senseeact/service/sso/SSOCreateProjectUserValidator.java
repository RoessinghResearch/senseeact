package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.TokenResult;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;

import java.util.List;

public abstract class SSOCreateProjectUserValidator
		implements SSOUserValidator {
	private List<String> tokenProjects;
	private String requestedProject;

	public SSOCreateProjectUserValidator(List<String> tokenProjects,
			String requestedProject) {
		this.tokenProjects = tokenProjects;
		this.requestedProject = requestedProject;
	}

	public abstract String getSubjectEmail(String subject);

	@Override
	public User findAuthenticatedUser(ProtocolVersion version,
			HttpServletResponse response, Database authDb, String subject)
			throws HttpException, Exception {
		if (requestedProject != null && !tokenProjects.contains(
				requestedProject)) {
			return null;
		}
		String email = getSubjectEmail(subject);
		if (email == null)
			return null;
		TokenResult tokenResult = SSOTokenUserCreator.create(version,
				response, authDb, tokenProjects, email);
		UserCache userCache = UserCache.getInstance();
		return userCache.findByUserid(tokenResult.getUser());
	}
}
