package nl.rrd.senseeact.client.model;

import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.senseeact.dao.UserDatabaseObject;

public class RegexUserAccess extends UserDatabaseObject {
	@DatabaseField(value= DatabaseType.STRING)
	private String emailRegex;

	public RegexUserAccess() {
	}

	public RegexUserAccess(String user, String emailRegex) {
		super(user);
		this.emailRegex = emailRegex;
	}

	public String getEmailRegex() {
		return emailRegex;
	}

	public void setEmailRegex(String emailRegex) {
		this.emailRegex = emailRegex;
	}
}
