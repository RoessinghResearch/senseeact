package nl.rrd.senseeact.service.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.Project;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.dao.DatabaseSort;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.QueryRunner;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.exception.NotFoundException;
import nl.rrd.senseeact.service.export.DataExportStatus;
import nl.rrd.senseeact.service.export.DataExporterFactory;
import nl.rrd.senseeact.service.export.DataExporterManager;
import nl.rrd.senseeact.service.model.DataExportRecord;
import nl.rrd.senseeact.service.model.DataExportTable;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v{version}/download")
public class DownloadController {
	@Autowired
	private DataExporterManager exporterManager;

	@RequestMapping(value="/projects", method=RequestMethod.GET)
	public List<Project> getProjects(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName) throws HttpException, Exception {
		return QueryRunner.runAuthQuery(this::doGetProjects, versionName,
				request, response);
	}

	@RequestMapping(value="/list", method=RequestMethod.GET)
	public List<DataExportRecord> getDownloadList(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName) throws HttpException, Exception {
		return QueryRunner.runAuthQuery(this::doGetDownloadList, versionName,
				request, response);
	}

	@RequestMapping(value="/start", method=RequestMethod.POST)
	public void startDownload(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="project")
			String project) throws HttpException, Exception {
		QueryRunner.runAuthQuery((version, authDb, user) ->
				doStartDownload(authDb, user, project),
				versionName, request, response);
	}

	@RequestMapping(value="/{exportId}", method=RequestMethod.GET)
	public void downloadExport(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("exportId")
			String exportId) throws HttpException, Exception {
		QueryRunner.runAuthQuery((version, authDb, user) ->
				doDownloadExport(authDb, user, exportId, response),
				versionName, request, response);
	}

	private List<Project> doGetProjects(ProtocolVersion version,
			Database authDb, User user) throws HttpException, Exception {
		List<String> codes = user.findProjects(authDb);
		DataExporterFactory exportFactory = AppComponents.get(
				DataExporterFactory.class);
		List<String> exportProjects = exportFactory.getProjectCodes();
		List<Project> result = new ArrayList<>();
		ProjectRepository projectRepo = AppComponents.get(
				ProjectRepository.class);
		for (String code : codes) {
			if (!exportProjects.contains(code))
				continue;
			BaseProject project = projectRepo.findProjectByCode(code);
			result.add(new Project(code, project.getName()));
		}
		return result;
	}

	private List<DataExportRecord> doGetDownloadList(ProtocolVersion version,
			Database authDb, User user) throws HttpException, Exception {
		DataExportTable table = new DataExportTable();
		DatabaseCriteria criteria = new DatabaseCriteria.Equal(
				"user", user.getUserid());
		DatabaseSort[] sort = new DatabaseSort[] {
			new DatabaseSort("utcTime", true)
		};
		return authDb.select(table, criteria, 0, sort);
	}

	private Object doStartDownload(Database authDb, User user, String project)
			throws HttpException, Exception {
		ProjectControllerExecution.findUserProject(project, authDb, user);
		DataExporterFactory exportFactory = AppComponents.get(
				DataExporterFactory.class);
		List<String> exportProjects = exportFactory.getProjectCodes();
		if (!exportProjects.contains(project)) {
			throw new NotFoundException(String.format(
					"Export for project \"%s\" not available", project));
		}
		exporterManager.startExport(project, user);
		return null;
	}

	private Object doDownloadExport(Database authDb, User user, String exportId,
			HttpServletResponse response) throws HttpException, Exception {
		DataExportTable table = new DataExportTable();
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("id", exportId),
				new DatabaseCriteria.Equal("user", user.getUserid())
		);
		DataExportRecord export = authDb.selectOne(table, criteria, null);
		if (export == null)
			throw new NotFoundException("Data export not found: " + exportId);
		if (!DataExportStatus.COMPLETED.name().equals(export.getStatus())) {
			throw new NotFoundException("Data export not completed: " +
					exportId);
		}
		File zip = exporterManager.getExportZip(export);
		response.setContentType("application/x-zip");
		response.setContentLengthLong(zip.length());
		response.setHeader("Content-Disposition", "attachment; filename=" +
				zip.getName());
		try (FileInputStream in = new FileInputStream(zip)) {
			try (OutputStream out = response.getOutputStream()) {
				FileUtils.copyStream(in, out, 0, null);
			}
		}
		return null;
	}
}
