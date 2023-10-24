package nl.rrd.senseeact.dao;

import nl.rrd.utils.exception.DatabaseException;

/**
 * This is a special database table that is used internally in {@link Database
 * Database}. It contains {@link UserTableKey UserTableKey}s.
 * 
 * @author Dennis Hofs (RRD)
 */
public class UserTableKeyTable extends DatabaseTableDef<UserTableKey> {
	public static final String NAME = "_user_table_keys";
	
	private static final int VERSION = 0;

	public UserTableKeyTable() {
		super(NAME, UserTableKey.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
