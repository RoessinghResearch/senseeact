package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.client.model.TokenResult;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.AuthToken;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.UserListenerRepository;
import nl.rrd.senseeact.service.controller.AuthControllerExecution;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;
import nl.rrd.senseeact.service.model.UserProject;
import nl.rrd.senseeact.service.model.UserProjectTable;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.validation.ValidationException;
import org.slf4j.Logger;

import java.time.ZonedDateTime;
import java.util.UUID;

public class SSOTokenUserCreator {
	public static TokenResult create(ProtocolVersion version,
			HttpServletResponse response, Database authDb, String project,
			String email) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			Logger logger = AppComponents.getLogger(
					SSOTokenUserCreator.class.getSimpleName());
			logger.info("CREATE USER: " + email);
			UserCache userCache = UserCache.getInstance();
			User user = userCache.findByEmail(email);
			if (user == null) {
				logger.info("CREATE NEW USER: " + email);
				return createNewUser(version, response, authDb, project, email);
			} else if (project != null) {
				logger.info("ADD USER TO PROJECT: " + user.getUserid());
				return addUserToProject(version, response, authDb, project,
						user);
			} else {
				logger.info("CREATE TOKEN: " + user.getUserid());
				return createToken(version, response, user);
			}
		}
	}

	private static TokenResult createNewUser(ProtocolVersion version,
			HttpServletResponse response, Database authDb, String project,
			String email) throws HttpException, Exception {
		String password = UUID.randomUUID().toString().toLowerCase()
				.replaceAll("-", "");
		return AuthControllerExecution.signupSSO(version, response, email,
				password, project, authDb);
	}

	private static TokenResult addUserToProject(ProtocolVersion version,
			HttpServletResponse response, Database authDb, String projectCode,
			User user) throws HttpException, DatabaseException {
		ProjectRepository projects = AppComponents.get(ProjectRepository.class);
		BaseProject project = projects.findProjectByCode(projectCode);
		if (!User.isProjectUser(authDb, project.getCode(), user.getUserid(),
				Role.PATIENT)) {
			try {
				project.validateAddUser(user, user, authDb);
			} catch (ValidationException ex) {
				throw new ForbiddenException(ex.getMessage());
			}
			UserProject userProject = new UserProject();
			userProject.setUser(user.getUserid());
			userProject.setProjectCode(project.getCode());
			userProject.setAsRole(Role.PATIENT);
			authDb.insert(UserProjectTable.NAME, userProject);
			UserListenerRepository.getInstance().notifyUserAddedToProject(
					user, project.getCode(), Role.PATIENT);
		}
		return createToken(version, response, user);
	}

	private static TokenResult createToken(ProtocolVersion version,
			HttpServletResponse response, User user) {
		ZonedDateTime now = DateTimeUtils.nowMs();
		String token = AuthToken.createToken(version, user, now, null,
				false, false, response);
		return new TokenResult(user.getUserid(), token);
	}
}
