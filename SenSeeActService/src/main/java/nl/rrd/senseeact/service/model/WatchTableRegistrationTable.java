package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.dao.*;
import nl.rrd.utils.exception.DatabaseException;

public class WatchTableRegistrationTable extends
DatabaseTableDef<WatchTableRegistration> {
	public static final String NAME = "watch_table_registrations";
	
	private static final int VERSION = 2;
	
	public WatchTableRegistrationTable() {
		super(NAME, WatchTableRegistration.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		if (version == 0)
			return upgradeTableV0(db, physTable);
		else if (version == 1)
			return upgradeTableV1(db, physTable);
		else
			return 2;
	}

	private int upgradeTableV0(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("callbackUrl",
				DatabaseType.STRING));
		db.addColumn(physTable, new DatabaseColumnDef("callbackFailStart",
				DatabaseType.LONG));
		db.addColumn(physTable, new DatabaseColumnDef("callbackFailCount",
				DatabaseType.INT));
		return 1;
	}

	private int upgradeTableV1(Database db, String physTable)
			throws DatabaseException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal(
				"isObjectTable", 1);
		db.delete(physTable, null, criteria);
		db.dropColumn(physTable, "isObjectTable");
		return 2;
	}
}
