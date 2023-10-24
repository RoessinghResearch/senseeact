package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.client.model.TokenResult;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.AuthToken;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.UserListenerRepository;
import nl.rrd.senseeact.service.controller.AuthControllerExecution;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;
import nl.rrd.senseeact.service.model.UserProject;
import nl.rrd.senseeact.service.model.UserProjectTable;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;

import java.time.ZonedDateTime;
import java.util.UUID;

public class SSOTokenUserCreator {
	public static TokenResult create(ProtocolVersion version,
			HttpServletResponse response, Database authDb, String project,
			String email) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			UserCache userCache = UserCache.getInstance();
			User user = userCache.findByEmail(email);
			if (user == null) {
				return createNewUser(version, response, authDb, project, email);
			} else {
				return addUserToProject(version, response, authDb, project,
						user);
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
			HttpServletResponse response, Database authDb, String project,
			User user) throws DatabaseException {
		if (!User.isProjectUser(authDb, project, user.getUserid(),
				Role.PATIENT)) {
			UserProject userProject = new UserProject();
			userProject.setUser(user.getUserid());
			userProject.setProjectCode(project);
			userProject.setAsRole(Role.PATIENT);
			authDb.insert(UserProjectTable.NAME, userProject);
			UserListenerRepository.getInstance().notifyUserAddedToProject(
					user, project, Role.PATIENT);
		}
		ZonedDateTime now = DateTimeUtils.nowMs();
		String token = AuthToken.createToken(version, user, now, null,
				false, false, response);
		return new TokenResult(user.getUserid(), token);
	}
}
