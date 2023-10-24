package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;

public interface SSOUserValidator {
	User findAuthenticatedUser(ProtocolVersion version,
			HttpServletResponse response, Database authDb, String subject)
			throws HttpException, Exception;
}
