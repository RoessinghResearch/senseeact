package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.client.model.PermissionRecord;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.utils.exception.DatabaseException;

public class PermissionTable extends DatabaseTableDef<PermissionRecord> {
	public static final String NAME = "permissions";

	private static final int VERSION = 0;

	public PermissionTable() {
		super(NAME, PermissionRecord.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
