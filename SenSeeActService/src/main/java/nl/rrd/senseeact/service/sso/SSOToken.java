package nl.rrd.senseeact.service.sso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.ValidateTokenResult;
import nl.rrd.senseeact.service.exception.HttpException;

/**
 * Implementations of this class can check whether a HTTP request contains a
 * matching SSO token and check whether the token is valid.
 *
 * @author Dennis Hofs (RRD)
 */
public abstract class SSOToken {
	private String project;

	/**
	 * Constructs a new instance. If SSO tokens for this instance should only be
	 * valid for a specific project, then you should specify the project code.
	 * If you set the project code to null, the tokens will be valid for any
	 * project.
	 *
	 * @param project the project code or null
	 */
	public SSOToken(String project) {
		this.project = project;
	}

	/**
	 * If SSO tokens for this instance should only be valid for a specific
	 * project, then this method returns the project code. If the tokens are
	 * valid for any project, then it returns null.
	 *
	 * @return the project code or null
	 */
	public String getProject() {
		return project;
	}

	/**
	 * Checks whether the specified HTTP request contains a token that can be
	 * validated by this instance.
	 *
	 * @param request the HTTP request
	 * @return true if the request contains a token for this instance, false
	 * otherwise
	 */
	public abstract boolean requestHasToken(HttpServletRequest request);

	/**
	 * Validates the token in the specified request. If the request is related
	 * to a project, then you should specify the project code. This method will
	 * check if the token is valid and identify the authenticated user. If the
	 * request is related to a project and the SSO tokens for this instance are
	 * only valid for a specific project, then this method will also check
	 * whether the project matches. If the validation fails, then this method
	 * throws an HTTPException.
	 *
	 * @param version the protocol version
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param authDb the authentication database
	 * @param project the project code or null
	 * @return the authenticated user and details
	 * @throws HttpException if the token is invalid
	 * @throws Exception if an unexpected error occurs
	 */
	public abstract ValidateTokenResult validateToken(ProtocolVersion version,
			HttpServletRequest request, HttpServletResponse response,
			Database authDb, String project) throws HttpException, Exception;
}
