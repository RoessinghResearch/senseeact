package nl.rrd.senseeact.dao;

/**
 * This is a special database table that is used internally in {@link Database
 * Database}. It can log database actions for synchronisation with a remote
 * database. In the database it's stored in a table with name "_action_log".
 * 
 * <p>If you implement an upgrade of this table, you should also implement an
 * upgrade in {@link DatabaseActionMetaTable DatabaseActionMetaTable},
 * which triggers the upgrades for all action tables.</p>
 *
 * @author Dennis Hofs (RRD)
 */
public class DatabaseActionTable {
	public static final String NAME_PREFIX = "_action_log_";
	
	private String name;
	private UserTableKey userTableKey;

	public DatabaseActionTable(UserTableKey userTableKey) {
		this.name = NAME_PREFIX + userTableKey.getKey();
		this.userTableKey = userTableKey;
	}
	
	public String getName() {
		return name;
	}

	public UserTableKey getUserTableKey() {
		return userTableKey;
	}
}
