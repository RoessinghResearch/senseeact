package nl.rrd.senseeact.dao.sync;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;

/**
 * This is a special database table that is used for synchronisation with a
 * remote database. It keeps track of what data has already been synchronised
 * from the remote database to the local database. In the database it's stored
 * in a table with name "_sync_progress".
 *
 * @author Dennis Hofs (RRD)
 */
public class SyncProgressTableDef extends DatabaseTableDef<SyncProgress> {
	public static final String NAME = "_sync_progress";

	private static final int VERSION = 0;

	public SyncProgressTableDef() {
		super(NAME, SyncProgress.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
