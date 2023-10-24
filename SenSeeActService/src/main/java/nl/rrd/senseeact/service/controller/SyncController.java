package nl.rrd.senseeact.service.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.SyncWatchResult;
import nl.rrd.senseeact.dao.DatabaseAction;
import nl.rrd.senseeact.dao.sync.SyncActionStats;
import nl.rrd.senseeact.dao.sync.SyncProgress;
import nl.rrd.senseeact.service.QueryRunner;
import nl.rrd.senseeact.service.exception.HttpException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v{version}/sync")
public class SyncController {
	private SyncControllerExecution exec = new SyncControllerExecution();

	@RequestMapping(value="/project/{project}/get-read-stats",
			method=RequestMethod.POST)
	public SyncActionStats getReadStats(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getReadStats(version, request, authDb, projectDb, user,
						subject),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/project/{project}/read",
			method=RequestMethod.POST)
	public List<DatabaseAction> read(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.read(version, request, authDb, projectDb, user, subject),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/project/{project}/register-push",
			method=RequestMethod.POST)
	public void registerPush(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.registerPush(version, request, authDb, projectDb, user,
						baseProject, subject),
				versionName, project, request, response);
	}

	@RequestMapping(value="/project/{project}/unregister-push",
			method=RequestMethod.POST)
	public void syncUnregisterPush(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="deviceId")
			String deviceId) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.unregisterPush(version, authDb, projectDb, user,
						baseProject, subject, deviceId),
				versionName, project, request, response);
	}
	
	@RequestMapping(value="/project/{project}/watch",
			method=RequestMethod.POST)
	public SyncWatchResult syncWatch(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		return exec.watch(request, response, versionName, project, subject);
	}

	@RequestMapping(value="/project/{project}/get-progress",
			method=RequestMethod.POST)
	public List<SyncProgress> getSyncProgress(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		return QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.getProgress(version, request, authDb, projectDb, user,
						subject),
				versionName, project, request, response);
	}

	@RequestMapping(value="/project/{project}/write",
			method=RequestMethod.POST)
	public void syncWrite(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("project")
			String project,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject) throws HttpException, Exception {
		QueryRunner.runProjectQuery(
				(version, authDb, projectDb, user, baseProject) ->
				exec.write(version, request, authDb, projectDb, user, subject),
				versionName, project, request, response);
	}
}
