package nl.rrd.senseeact.dao.sync;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class SyncTestUser1Table extends DatabaseTableDef<SyncTestUserObject> {
	public static final String NAME = "sync_user1";

	private static final int VERSION = 0;

	public SyncTestUser1Table() {
		super(NAME, SyncTestUserObject.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
