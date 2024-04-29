package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PushMessageData extends JsonObject {
	private String project;
	private String user;
	private String table;
	
	public PushMessageData() {
	}

	/**
	 * Constructs a new instance.
	 *
	 * @param project the project code
	 * @param user the user or null if it is a table without a user field
	 * @param table the table name
	 */
	public PushMessageData(String project, String user, String table) {
		this.project = project;
		this.user = user;
		this.table = table;
	}
	
	public String getProject() {
		return project;
	}
	
	public void setProject(String project) {
		this.project = project;
	}

	/**
	 * Returns the user or null if it is a table without a user field.
	 *
	 * @return the user or null if it is a table without a user field.
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the user or null if it is a table without a user field.
	 *
	 * @param user the user or null if it is a table without a user field
	 */
	public void setUser(String user) {
		this.user = user;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}
}
