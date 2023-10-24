package nl.rrd.senseeact.client.model.compat;

import nl.rrd.senseeact.client.model.Project;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.utils.json.JsonObject;

/**
 * This class defines a project code and further details received from the
 * server. The class {@link BaseProject BaseProject} provides more details
 * about a project. You can get the {@link BaseProject BaseProject} with {@link
 * ProjectRepository#findProjectByCode(String)
 * ProjectRepository.findProjectByCode()}.
 *
 * @author Dennis Hofs (RRD)
 */
public class ProjectV1 extends JsonObject {
	private String code;
	private String syncUser;

	/**
	 * Returns the project code. This serves as a unique identifier.
	 *
	 * @return the project code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Sets the project code. This serves as a unique identifier.
	 *
	 * @param code the project code
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Returns the email address of the user that the database synchronization
	 * should operate on. This is usually the user who is logged in, but may be
	 * different for some projects. An example is when the current user is a
	 * health care professional who should receive the data of a patient.
	 *
	 * @return the synchronization user
	 */
	public String getSyncUser() {
		return syncUser;
	}

	/**
	 * Sets the email address of the user that the database synchronization
	 * should operate on. This is usually the user who is logged in, but may be
	 * different for some projects. An example is when the current user is a
	 * health care professional who should receive the data of a patient.
	 *
	 * @param syncUser the synchronization user
	 */
	public void setSyncUser(String syncUser) {
		this.syncUser = syncUser;
	}

	public Project toProject() {
		Project project = new Project();
		project.setCode(code);
		return project;
	}
}
