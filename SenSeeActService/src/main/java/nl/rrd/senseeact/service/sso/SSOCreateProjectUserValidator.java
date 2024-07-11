package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.TokenResult;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;
import nl.rrd.utils.AppComponents;
import org.slf4j.Logger;

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
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("FIND AUTHENTICATED USER: " + subject);
		if (requestedProject != null &&
				!requestedProject.equals(tokenProject)) {
			logger.info("NO PROJECT MATCH");
			return null;
		}
		String email = getSubjectEmail(subject);
		if (email == null) {
			logger.info("NO EMAIL");
			return null;
		}
		logger.info("FIND AUTHENTICATED USER FOR EMAIL: " + email);
		TokenResult tokenResult = SSOTokenUserCreator.create(version,
				response, authDb, tokenProject, email);
		logger.info("TOKEN: " + tokenResult.getUser());
		UserCache userCache = UserCache.getInstance();
		return userCache.findByUserid(tokenResult.getUser());
	}
}
