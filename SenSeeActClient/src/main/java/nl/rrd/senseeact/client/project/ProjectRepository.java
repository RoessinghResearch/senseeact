package nl.rrd.senseeact.client.project;

import nl.rrd.utils.AppComponent;
import nl.rrd.senseeact.client.SenSeeActClient;

import java.util.List;

/**
 * Repository of all known projects in SenSeeAct. It contains instances of
 * {@link BaseProject BaseProject} which provides any project-specific details
 * that are needed on the client side. Projects are identified by a code. The
 * codes of accessible projects for a specific user can be obtained with {@link
 * SenSeeActClient#getProjectList() getProjectList()}.
 *
 * @author Dennis Hofs (RRD)
 */
@AppComponent
public abstract class ProjectRepository {
	private final Object lock = new Object();
	private List<BaseProject> projects = null;

	/**
	 * Returns all projects. Note that a user may not be allowed to access
	 * every project on the server. See also {@link
	 * SenSeeActClient#getProjectList() getProjectList()}.
	 * 
	 * @return the projects
	 */
	public List<BaseProject> getProjects() {
		synchronized (lock) {
			if (projects != null)
				return projects;
			projects = createProjects();
			return projects;
		}
	}

	protected abstract List<BaseProject> createProjects();

	/**
	 * Finds the project with the specified code. If no such project exists,
	 * this method returns null. Note that a user may not be allowed to access
	 * the project on the server. See also {@link
	 * SenSeeActClient#getProjectList() RRDSenSeeActClient.getProjectList()}.
	 * 
	 * @param code the project code
	 * @return the project or null
	 */
	public BaseProject findProjectByCode(String code) {
		List<BaseProject> projects = getProjects();
		for (BaseProject project : projects) {
			if (project.getCode().equals(code))
				return project;
		}
		return null;
	}
}
