package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.ValidateTokenResult;
import nl.rrd.senseeact.service.exception.HttpException;

public abstract class SSOToken {
	private String project;

	public SSOToken(String project) {
		this.project = project;
	}

	public String getProject() {
		return project;
	}

	public abstract boolean requestHasToken(HttpServletRequest request);

	public abstract ValidateTokenResult validateToken(ProtocolVersion version,
			HttpServletRequest request, HttpServletResponse response,
			Database authDb, String project) throws HttpException, Exception;
}
