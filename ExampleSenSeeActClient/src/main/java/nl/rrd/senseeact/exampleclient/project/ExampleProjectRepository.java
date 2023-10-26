package nl.rrd.senseeact.exampleclient.project;

import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.exampleclient.project.defaultproject.DefaultProject;

import java.util.List;

public class ExampleProjectRepository extends ProjectRepository {
	@Override
	protected List<BaseProject> createProjects() {
		return List.of(
			new DefaultProject()
		);
	}
}
