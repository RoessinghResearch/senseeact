package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class SyncPushRegistrationTable extends
DatabaseTableDef<SyncPushRegistration> {
	public static final String NAME = "sync_push_registrations";

	public static final int VERSION = 0;

	public SyncPushRegistrationTable() {
		super(NAME, SyncPushRegistration.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
