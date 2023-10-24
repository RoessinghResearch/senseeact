package nl.rrd.senseeact.service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.utils.AppComponents;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import nl.rrd.senseeact.client.model.NullableResponse;
import nl.rrd.senseeact.client.model.TableSpec;
import nl.rrd.senseeact.dao.DatabaseObject;
import nl.rrd.senseeact.service.QueryRunner;
import nl.rrd.senseeact.service.SenSeeActContext;
import nl.rrd.senseeact.service.exception.HttpException;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v{version}/project")
public class ProjectController {
	private ProjectControllerExecution exec = new ProjectControllerExecution();

	@RequestMapping(value="/list", method= RequestMethod.GET)
	public List<?> list(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName) throws HttpException, Exception {
		return QueryRunner.runAuthQuery(exec::list, versionName, request,
				response);
	}
	
	@RequestMapping(value="/list/all", method=RequestMethod.GET)
	public List<String> listAll(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName) throws HttpException, Exception {
		return QueryRunner.runAuthQuery(
				(version, authDb, user) -> exec.listAll(),
				versionName, request, response);
	}
	
	@RequestMapping(value="/{project}/check", method=RequestMethod.GET)
	public void checkProject(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			final String subject) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
						exec.checkProject(version, authDb, user, subject),
				versionName, project, request, response);
	}

	@RequestMapping(value="/{project}/users", method=RequestMethod.GET)
	public List<DatabaseObject> getUsers(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			final String forUser,
			@RequestParam(value="role", required=false, defaultValue="")
			final String role,
			@RequestParam(value="includeInactive", required=false, defaultValue="true")
			final String includeInactive) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getUsers(version, authDb, user, baseProject, forUser,
						role, includeInactive),
				versionName, project, request, response);
	}
	
	@Hidden
	@RequestMapping(value="/{project}/addUser", method=RequestMethod.POST)
	public void addUserDeprecated(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			final String userid,
			@RequestParam(value="email", required=false, defaultValue="")
			final String compatEmail,
			@RequestParam(value="asRole", defaultValue="PATIENT")
			String asRole) throws HttpException, Exception {
		addUser(request, response, versionName, project, userid, compatEmail,
				asRole);
	}

	@RequestMapping(value="/{project}/user", method=RequestMethod.POST)
	public void addUser(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			final String userid,
			@Parameter(hidden = true)
			@RequestParam(value="email", required=false, defaultValue="")
			final String compatEmail,
			@RequestParam(value="asRole", defaultValue="PATIENT")
			final String asRole) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, user) ->
				exec.addUser(version, authDb, user, project, userid,
						compatEmail, asRole),
				versionName, request, response);
	}

	@RequestMapping(value="/{project}/user", method=RequestMethod.DELETE)
	public void removeUser(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			final String userid,
			@Parameter(hidden = true)
			@RequestParam(value="email", required=false, defaultValue="")
			final String compatEmail,
			@RequestParam(value="asRole", required=false, defaultValue="")
			final String asRole) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, user) ->
				exec.removeUser(version, authDb, user, project, userid,
						compatEmail, asRole),
				versionName, request, response);
	}

	@Hidden
	@RequestMapping(value="/{project}/subjects", method=RequestMethod.GET)
	public List<DatabaseObject> getSubjectsDeprecated(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			final String forUser,
			@RequestParam(value="includeInactive", required=false, defaultValue="true")
			final String includeInactive) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getSubjects(version, authDb, user, baseProject, forUser,
						includeInactive),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/subjects/watch/register",
			method=RequestMethod.POST)
	public String registerWatchSubjects(
			final HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="reset", required=false, defaultValue="false")
			boolean reset) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.registerWatchSubjects(authDb, user, baseProject, reset),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/subjects/watch/{id}",
			method=RequestMethod.GET)
	public void watchSubjects(
			final HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("id")
			String id) throws HttpException, Exception {
		exec.watchSubjects(request, response, versionName, project, id);
	}
	
	@RequestMapping(value="/{project}/subjects/watch/unregister/{id}",
			method=RequestMethod.POST)
	public void unregisterWatchSubjects(
			final HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("id")
			String id) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.unregisterWatchSubjects(authDb, user, baseProject, id),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/tables", method=RequestMethod.GET)
	public List<String> getTableList(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="logid", required=false, defaultValue="")
			String logId) throws HttpException, Exception {
		Logger logger = AppComponents.getLogger(SenSeeActContext.LOGTAG);
		if (logId != null && logId.length() == 0)
			logId = null;
		if (logId != null) {
			logger.info("Start getTableList {}, project {}", logId, project);
		}
		List<String> result = QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
						exec.getTableList(baseProject),
				versionName, project, request, response, logId);
		if (logId != null) {
			logger.info("End getTableList {}, project {}", logId, project);
		}
		return result;
	}
	
	@RequestMapping(value="/{project}/table/{table}/spec", method=RequestMethod.GET)
	public TableSpec getTableSpec(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
						exec.getTableSpec(baseProject, table),
				versionName, project, request, response);
	}

	@RequestMapping(value="/{project}/table/{table}",
			method=RequestMethod.GET)
	public void getRecords(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="start", required=false, defaultValue="")
			String start,
			@RequestParam(value="end", required=false, defaultValue="")
			String end) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getRecords(version, authDb, projectDb, user, baseProject,
						table, subject, start, end, null, response),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/table/{table}/filter/get",
			method=RequestMethod.POST)
	@RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public void getRecordsWithFilter(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="start", required=false, defaultValue="")
			String start,
			@RequestParam(value="end", required=false, defaultValue="")
			String end) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getRecords(version, authDb, projectDb, user, baseProject,
						table, subject, start, end, request, response),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/table/{table}/{recordId}",
			method=RequestMethod.GET)
	public Map<?,?> getRecord(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@PathVariable("recordId")
			String recordId,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getRecord(version, authDb, projectDb, user, baseProject,
						table, recordId, subject),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/table/{table}",
			method=RequestMethod.POST)
	@RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public List<String> insertRecords(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.insertRecords(version, request, authDb, projectDb, user,
						baseProject, table, subject),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/table/{table}/{recordId}",
			method=RequestMethod.PUT)
	@RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public void updateRecord(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@PathVariable("recordId")
			String recordId,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.updateRecord(version, request, authDb, projectDb, user,
						baseProject, table, recordId, subject),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/table/{table}",
			method=RequestMethod.DELETE)
	public void deleteRecords(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="start", required=false, defaultValue="")
			String start,
			@RequestParam(value="end", required=false, defaultValue="")
			String end) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.deleteRecords(version, authDb, projectDb, user,
						baseProject, table, subject, start, end, null),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/table/{table}/filter/delete",
			method=RequestMethod.POST)
	@RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public void deleteRecordsWithFilter(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="start", required=false, defaultValue="")
			String start,
			@RequestParam(value="end", required=false, defaultValue="")
			String end) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.deleteRecords(version, authDb, projectDb, user,
						baseProject, table, subject, start, end, request),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/table/{table}/{recordId}",
			method=RequestMethod.DELETE)
	public void deleteRecord(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@PathVariable("recordId")
			String recordId,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.deleteRecord(version, authDb, projectDb, user, baseProject,
						table, recordId, subject),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/table/{table}/purge",
			method=RequestMethod.DELETE)
	public void purgeTable(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.purgeTable(version, authDb, projectDb, user, baseProject,
						table, subject),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/table/{table}/first",
			method=RequestMethod.GET)
	public NullableResponse<Map<?,?>> getFirstRecord(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="start", required=false, defaultValue="")
			String start,
			@RequestParam(value="end", required=false, defaultValue="")
			String end) throws HttpException, Exception {
		Map<?,?> result = QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getFirstLastRecord(version, authDb, projectDb, user,
						baseProject, table, subject, start, end, true, null),
				versionName, project, request, response);
		return new NullableResponse<>(result);
	}
	
	@RequestMapping(value="/{project}/table/{table}/last",
			method=RequestMethod.GET)
	public NullableResponse<Map<?,?>> getLastRecord(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="start", required=false, defaultValue="")
			String start,
			@RequestParam(value="end", required=false, defaultValue="")
			String end) throws HttpException, Exception {
		Map<?,?> result = QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getFirstLastRecord(version, authDb, projectDb, user,
						baseProject, table, subject, start, end, false, null),
				versionName, project, request, response);
		return new NullableResponse<>(result);
	}
	
	@RequestMapping(value="/{project}/table/{table}/filter/get/first",
			method=RequestMethod.POST)
	@RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public NullableResponse<Map<?,?>> getFirstRecordWithFilter(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="start", required=false, defaultValue="")
			String start,
			@RequestParam(value="end", required=false, defaultValue="")
			String end) throws HttpException, Exception {
		Map<?,?> result = QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getFirstLastRecord(version, authDb, projectDb, user,
						baseProject, table, subject, start, end, true, request),
				versionName, project, request, response);
		return new NullableResponse<>(result);
	}
	
	@RequestMapping(value="/{project}/table/{table}/filter/get/last",
			method=RequestMethod.POST)
	@RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public NullableResponse<Map<?,?>> getLastRecordWithFilter(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="start", required=false, defaultValue="")
			String start,
			@RequestParam(value="end", required=false, defaultValue="")
			String end) throws HttpException, Exception {
		Map<?,?> result = QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getFirstLastRecord(version, authDb, projectDb, user,
						baseProject, table, subject, start, end, false,
						request),
				versionName, project, request, response);
		return new NullableResponse<>(result);
	}
	
	@RequestMapping(value="/{project}/table/{table}/watch/register",
			method=RequestMethod.POST)
	public String registerWatchTable(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="anyUser", required=false, defaultValue="false")
			boolean anySubject,
			@RequestParam(value="callbackUrl", required=false, defaultValue="")
			String callbackUrl,
			@RequestParam(value="reset", required=false, defaultValue="false")
			boolean reset) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.registerWatchTable(version, authDb, projectDb, user,
						baseProject, table, subject, anySubject, callbackUrl,
						reset),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/{project}/table/{table}/watch/{id}",
			method=RequestMethod.GET)
	public void watchTable(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@PathVariable("id")
			String id) throws HttpException, Exception {
		exec.watchTable(request, response, versionName, project, table, id);
	}
	
	@RequestMapping(value="/{project}/table/{table}/watch/unregister/{id}",
			method=RequestMethod.POST)
	public void unregisterWatchTable(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@PathVariable("table")
			String table,
			@PathVariable("id")
			String id) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.unregisterWatchTable(authDb, projectDb, user, baseProject,
						table, id),
				versionName, project, request, response);
	}
}
