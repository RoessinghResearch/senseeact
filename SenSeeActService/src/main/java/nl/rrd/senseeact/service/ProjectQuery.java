package nl.rrd.senseeact.service;

import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;

/**
 * Implementations of this interface can be passed to {@link QueryRunner
 * QueryRunner} to run a query on a project.
 * 
 * @author Dennis Hofs (RRD)
 *
 * @param <T> the type of the query result
 */
public interface ProjectQuery<T> {
	
	/**
	 * Runs the query. The {@link QueryRunner QueryRunner} has already
	 * validated whether an authentication token was provided and whether it
	 * was valid. The parameter "user" specifies the authenticated user. If
	 * the token was invalid, this method will never be called. The {@link
	 * QueryRunner QueryRunner} has also validated that the user can access
	 * the project.
	 * 
	 * <p>A project may not have a database. In that case the database is null.
	 * This is determined by {@link BaseProject#getDatabaseTables()
	 * BaseProject.getDatabaseTables()}.</p>
	 * 
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param projectDb the project database or null
	 * @param user the user
	 * @param project the project
	 * @return the query result
	 * @throws HttpException if the query should return an HTTP error status
	 * @throws Exception if an unexpected error occurs. This results in HTTP
	 * error status 500 Internal Server Error.
	 */
	T runQuery(ProtocolVersion version, Database authDb, Database projectDb,
			User user, BaseProject project) throws HttpException, Exception;
}
