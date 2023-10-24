package nl.rrd.senseeact.service.controller.model;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.validation.MapReader;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.sync.SyncProgress;
import nl.rrd.senseeact.dao.sync.SyncTimeRangeRestriction;
import nl.rrd.senseeact.service.HttpContentReader;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.QueryRunner;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;

import java.util.List;
import java.util.Map;

/**
 * This class models validated input from the sync watch query.
 * 
 * @author Dennis Hofs (RRD)
 */
public class SyncWatchInput {
	private User subjectUser;
	private String databaseName;
	private int maxCount = 0;
	private List<SyncProgress> progress = null;
	private List<String> includeTables = null;
	private List<String> excludeTables = null;
	private List<SyncTimeRangeRestriction> timeRangeRestrictions = null;

	/**
	 * Returns the subject user.
	 * 
	 * @return the subject user
	 */
	public User getSubjectUser() {
		return subjectUser;
	}
	
	/**
	 * Sets the subject user.
	 * 
	 * @param subjectUser the subject user
	 */
	public void setSubjectUser(User subjectUser) {
		this.subjectUser = subjectUser;
	}

	/**
	 * Returns the database name.
	 * 
	 * @return the database name
	 */
	public String getDatabaseName() {
		return databaseName;
	}
	
	/**
	 * Sets the database name.
	 * 
	 * @param databaseName the database name
	 */
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
	
	/**
	 * Returns the maximum number of database actions to return. This can be 0
	 * if there is no maximum.
	 * 
	 * @return the maximum number of database actions to return or 0
	 */
	public int getMaxCount() {
		return maxCount;
	}
	
	/**
	 * Sets the maximum number of database actions to return. This can be 0 if
	 * there is no maximum.
	 * 
	 * @param maxCount the maximum number of database actions to return or 0
	 */
	public void setMaxCount(int maxCount) {
		this.maxCount = maxCount;
	}
	
	/**
	 * Returns the progress of database actions that were synchronized before.
	 * This can be an empty list or null.
	 * 
	 * @return the progress of database actions that were synchronized before
	 * (can be an empty list or null)
	 */
	public List<SyncProgress> getProgress() {
		return progress;
	}
	
	/**
	 * Sets the progress of database actions that were synchronized before. This
	 * can be an empty list or null.
	 * 
	 * @param progress the progress of database actions that were synchronized
	 * before (can be an empty list or null)
	 */
	public void setProgress(List<SyncProgress> progress) {
		this.progress = progress;
	}
	
	/**
	 * Returns the include tables. This can be an empty list or null.
	 * 
	 * @return the include tables (can be an empty list or null)
	 */
	public List<String> getIncludeTables() {
		return includeTables;
	}
	
	/**
	 * Sets the include tables. This can be an empty list or null.
	 * 
	 * @param includeTables the include tables (can be an empty list or null)
	 */
	public void setIncludeTables(List<String> includeTables) {
		this.includeTables = includeTables;
	}
	
	/**
	 * Returns the exclude tables. This can be an empty list or null.
	 * 
	 * @return the exclude tables (can be an empty list or null)
	 */
	public List<String> getExcludeTables() {
		return excludeTables;
	}
	
	/**
	 * Sets the exclude tables. This can be an empty list or null.
	 * 
	 * @param excludeTables the exclude tables (can be an empty list or null)
	 */
	public void setExcludeTables(List<String> excludeTables) {
		this.excludeTables = excludeTables;
	}
	
	/**
	 * Returns the time range restrictions. This can be an empty list or null.
	 * 
	 * @return the time range restrictions (can be an empty list or null)
	 */
	public List<SyncTimeRangeRestriction> getTimeRangeRestrictions() {
		return timeRangeRestrictions;
	}
	
	/**
	 * Sets the time range restrictions. This can be an empty list or null.
	 * 
	 * @param timeRangeRestrictions the time range restrictions (can be an empty
	 * list or null)
	 */
	public void setTimeRangeRestrictions(
			List<SyncTimeRangeRestriction> timeRangeRestrictions) {
		this.timeRangeRestrictions = timeRangeRestrictions;
	}
	
	/**
	 * Parses and validates input from the sync watch query. If the desired
	 * database (object or sample database) is not defined for the project, this
	 * method returns null, meaning that there will never be any database
	 * actions.
	 *
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param versionName the protocol version
	 * @param project the project code
	 * @param subject the user ID of the subject (can be an empty string or
	 * null)
	 * @return the query input or null
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	public static SyncWatchInput parse(HttpServletRequest request,
			HttpServletResponse response, String versionName, String project,
			String subject) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, srvProject) ->
				parse(version, request, authDb, projectDb, user, subject),
				versionName, project, request, response);
	}

	/**
	 * Parses and validates input from the sync watch query. If the database is
	 * null, this method returns null, meaning that there will never be any
	 * database actions.
	 *
	 * @param version the protocol version
	 * @param request the HTTP request
	 * @param authDb the authentication database
	 * @param database the object database or sample database (can be null)
	 * @param user the user
	 * @param subject the email address of the subject (can be an empty string
	 * or null)
	 * @return the query input or null
	 * @throws HttpException if the request is invalid
	 * @throws Exception if any other error occurs
	 */
	private static SyncWatchInput parse(ProtocolVersion version,
			HttpServletRequest request, Database authDb, Database database,
			User user, String subject) throws HttpException, Exception {
		SyncWatchInput result = new SyncWatchInput();
		result.subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		if (database == null)
			return null;
		result.databaseName = database.getName();
		try {
			Map<String,?> params = HttpContentReader.readJsonParams(request,
					true);
			if (params != null) {
				MapReader paramReader = new MapReader(params);
				result.maxCount = paramReader.readInt("maxCount", 0);
				result.progress = paramReader.readJson("progress",
						new TypeReference<>() {}, null);
				result.includeTables = paramReader.readJson("includeTables",
						new TypeReference<>() {}, null);
				result.excludeTables = paramReader.readJson("excludeTables",
						new TypeReference<>() {}, null);
				if (params.containsKey("timeRangeRestrictions")) {
					result.timeRangeRestrictions = paramReader.readJson(
							"timeRangeRestrictions", new TypeReference<>() {},
							null);
				} else {
					result.timeRangeRestrictions = paramReader.readJson(
							"sampleTimeRangeRestrictions",
							new TypeReference<>() {}, null);
				}
			}
			return result;
		} catch (ParseException ex) {
			throw new BadRequestException("Invalid content: " +
					ex.getMessage());
		}
	}
}
