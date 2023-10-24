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

	public String getUser() {
		return user;
	}

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
