package nl.rrd.senseeact.client.model;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class SystemStatTable extends DatabaseTableDef<SystemStat> {
	public static final String NAME = "system_stats";
	
	private static final int VERSION = 0;

	public SystemStatTable() {
		super(NAME, SystemStat.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
