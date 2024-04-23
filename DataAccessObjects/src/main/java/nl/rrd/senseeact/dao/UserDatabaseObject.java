package nl.rrd.senseeact.dao;

public class UserDatabaseObject extends BaseDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String user;

	public UserDatabaseObject() {
	}

	public UserDatabaseObject(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
}
