package nl.rrd.senseeact.client.model.compat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.utils.json.JsonObject;

/**
 * This class defines a project code. The endpoint GET /project/list returns
 * this class rather than just the project code. This way it is possible to
 * include further details as was done in old versions. The class {@link
 * BaseProject BaseProject} provides more details about a project. You can get
 * he {@link BaseProject BaseProject} with {@link
 * ProjectRepository#findProjectByCode(String)
 * ProjectRepository.findProjectByCode()}.
 * 
 * @author Dennis Hofs (RRD)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ProjectV3 extends JsonObject {
	private String code;

	public ProjectV3() {
	}

	public ProjectV3(String code) {
		this.code = code;
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
}
