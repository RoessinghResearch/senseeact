package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.utils.exception.DatabaseException;

public class MobileWakeRequestTable
		extends DatabaseTableDef<MobileWakeRequest> {
	public static final String NAME = "wake_mobile_requests";

	private static final int VERSION = 0;

	public MobileWakeRequestTable() {
		super(NAME, MobileWakeRequest.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
