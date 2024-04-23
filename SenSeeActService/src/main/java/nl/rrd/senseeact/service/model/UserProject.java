package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.dao.BaseDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

public class UserProject extends BaseDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String user;

	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String projectCode;
	
	@DatabaseField(value=DatabaseType.STRING)
	private Role asRole;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getProjectCode() {
		return projectCode;
	}

	public void setProjectCode(String projectCode) {
		this.projectCode = projectCode;
	}

	public Role getAsRole() {
		return asRole;
	}

	public void setAsRole(Role asRole) {
		this.asRole = asRole;
	}
}
