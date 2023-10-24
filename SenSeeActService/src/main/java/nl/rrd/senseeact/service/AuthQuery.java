package nl.rrd.senseeact.service;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;

/**
 * Implementations of this interface can be passed to {@link QueryRunner
 * QueryRunner} to run a query using the authentication database.
 * 
 * @author Dennis Hofs (RRD)
 *
 * @param <T> the type of the query result
 */
public interface AuthQuery<T> {
	
	/**
	 * Runs the query. If a token was passed to the {@link QueryRunner
	 * QueryRunner}, then the token has been validated and the parameter "user"
	 * is specified. If the token was invalid, this method will never be
	 * called. If no token was specified, the user will be null.
	 * 
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param user the user or null
	 * @return the query result
	 * @throws HttpException if the query should return an HTTP error status
	 * @throws Exception if an unexpected error occurs. This results in HTTP
	 * error status 500 Internal Server Error.
	 */
	T runQuery(ProtocolVersion version, Database authDb, User user)
			throws HttpException, Exception;
}
