package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.TokenResult;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;

public abstract class SSOCreateProjectUserValidator
		implements SSOUserValidator {
	private String tokenProject;
	private String requestedProject;

	public SSOCreateProjectUserValidator(String tokenProject,
			String requestedProject) {
		this.tokenProject = tokenProject;
		this.requestedProject = requestedProject;
	}

	public abstract String getSubjectEmail(String subject);

	@Override
	public User findAuthenticatedUser(ProtocolVersion version,
			HttpServletResponse response, Database authDb, String subject)
			throws HttpException, Exception {
		if (requestedProject != null &&
				!requestedProject.equals(tokenProject)) {
			return null;
		}
		String email = getSubjectEmail(subject);
		if (email == null)
			return null;
		TokenResult tokenResult = SSOTokenUserCreator.create(version,
				response, authDb, tokenProject, email);
		UserCache userCache = UserCache.getInstance();
		return userCache.findByUserid(tokenResult.getUser());
	}
}
