package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.json.JsonObject;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;

/**
 * This class defines a project code and name. The class {@link BaseProject
 * BaseProject} provides more details about a project. You can get the {@link
 * BaseProject BaseProject} with {@link
 * ProjectRepository#findProjectByCode(String)
 * ProjectRepository.findProjectByCode()}.
 * 
 * @author Dennis Hofs (RRD)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class Project extends JsonObject {
	private String code;
	private String name;

	public Project() {
	}

	public Project(String code, String name) {
		this.code = code;
		this.name = name;
	}

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
	 * Returns the project name that can be presented to the user.
	 *
	 * @return the project name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the project name that can be presented to the user.
	 *
	 * @param name the project name
	 */
	public void setName(String name) {
		this.name = name;
	}
}
