package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class UserActiveChangeTable extends DatabaseTableDef<UserActiveChange> {
	public static final String NAME = "user_active_changes";
	
	private static final int VERSION = 0;

	public UserActiveChangeTable() {
		super(NAME, UserActiveChange.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
