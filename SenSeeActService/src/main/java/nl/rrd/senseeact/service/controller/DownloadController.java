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
import nl.rrd.senseeact.service.export.DataExporterFactory;
import nl.rrd.senseeact.service.export.DataExporterManager;
import nl.rrd.senseeact.service.model.DataExportRecord;
import nl.rrd.senseeact.service.model.DataExportTable;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.utils.AppComponents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
}
