package nl.rrd.senseeact.dao.sync;

import nl.rrd.senseeact.dao.AbstractDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

public class SyncTestUserObject extends AbstractDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING)
	private String user;
	@DatabaseField(value=DatabaseType.STRING)
	private String source;
	@DatabaseField(value=DatabaseType.INT)
	private int intField;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public int getIntField() {
		return intField;
	}

	public void setIntField(int intField) {
		this.intField = intField;
	}
}
