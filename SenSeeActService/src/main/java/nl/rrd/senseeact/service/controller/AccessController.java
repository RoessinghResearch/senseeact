package nl.rrd.senseeact.service.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.ProjectDataModule;
import nl.rrd.senseeact.client.model.ProjectUserAccessRule;
import nl.rrd.senseeact.service.QueryRunner;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.PermissionRecord;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v{version}/access")
public class AccessController {
	private static final Object WRITE_LOCK = new Object();

	private AccessControllerExecution exec = new AccessControllerExecution();

	@RequestMapping(value="/project/{project}/modules",
			method=RequestMethod.GET)
	public List<ProjectDataModule> getModuleList(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, srvProject) ->
				exec.getModuleList(project),
				versionName, project, request, response);
	}

	@RequestMapping(value="/project/{project}/grantee/list",
			method=RequestMethod.GET)
	public List<ProjectUserAccessRule> getGranteeList(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="subject", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, srvProject) ->
				exec.getGranteeList(version, authDb, user, project, subject),
				versionName, project, request, response);
	}

	@RequestMapping(value="/project/{project}/subject/list",
			method=RequestMethod.GET)
	public List<ProjectUserAccessRule> getSubjectList(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="grantee", required=false, defaultValue="")
			String grantee) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, srvProject) ->
				exec.getSubjectList(version, authDb, user, project, grantee),
				versionName, project, request, response);
	}

	@RequestMapping(value="/project/{project}", method=RequestMethod.POST)
	@RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public void setAccessRule(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="granteeEmail")
			String granteeEmail,
			@RequestParam(value="subject", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		synchronized (WRITE_LOCK) {
			QueryRunner.runProjectQuery(
					(version, authDb, projectDb, user, srvProject) ->
					exec.setAccessRule(version, authDb, user, project,
							granteeEmail, subject, request),
					versionName, project, request, response);
		}
	}

	@RequestMapping(value="/project/{project}", method=RequestMethod.DELETE)
	public void deleteAccessRule(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="grantee")
			String grantee,
			@RequestParam(value="subject", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		synchronized (WRITE_LOCK) {
			QueryRunner.runProjectQuery(
					(version, authDb, projectDb, user, srvProject) ->
					exec.deleteAccessRule(version, authDb, user, project,
							grantee, subject),
					versionName, project, request, response);
		}
	}

	@RequestMapping(value="/subject", method=RequestMethod.POST)
	public void addSubject(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user")
			final String user,
			@RequestParam(value="subject")
			final String subject) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, currUser) ->
				exec.addSubject(version, authDb, currUser, user, subject),
				versionName, request, response);
	}

	@RequestMapping(value="/subject", method=RequestMethod.DELETE)
	public void removeSubject(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user")
			final String user,
			@RequestParam(value="subject")
			final String subject) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, currUser) ->
				exec.removeSubject(version, authDb, currUser, user,
						subject),
				versionName, request, response);
	}

	@RequestMapping(value="/permission/list", method=RequestMethod.GET)
	public List<Map<?,?>> getPermissions(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden=true)
			String versionName,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		return QueryRunner.runAuthQuery((version, authDb, user) ->
			exec.getPermissions(version, authDb, user, subject),
			versionName, request, response
		);
	}

	@RequestMapping(value="/permission", method=RequestMethod.POST)
	@RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public void grantPermission(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden=true)
			String versionName,
			@RequestParam(value="user")
			String subject,
			@RequestParam(value="permission")
			String permission) throws HttpException, Exception {
		QueryRunner.runAuthQuery((version, authDb, user) ->
				exec.grantPermission(version, authDb, user, request, subject,
						permission),
				versionName, request, response);
	}

	@RequestMapping(value="/permission", method=RequestMethod.DELETE)
	@RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public void revokePermission(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden=true)
			String versionName,
			@RequestParam(value="user")
			String subject,
			@RequestParam(value="permission")
			String permission) throws HttpException, Exception {
		QueryRunner.runAuthQuery((version, authDb, user) ->
				exec.revokePermission(version, authDb, user, request, subject,
						permission),
				versionName, request, response);
	}

	@RequestMapping(value="/permission/all", method=RequestMethod.DELETE)
	public void revokePermissionAll(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden=true)
			String versionName,
			@RequestParam(value="user")
			String subject,
			@RequestParam(value="permission")
			String permission) throws HttpException, Exception {
		QueryRunner.runAuthQuery((version, authDb, user) ->
				exec.revokePermissionAll(version, authDb, user, subject,
						permission),
				versionName, request, response);
	}
}
