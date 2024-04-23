package nl.rrd.senseeact.dao;

import nl.rrd.utils.exception.DatabaseException;

public class ResourceTestTable extends DatabaseTableDef<ResourceTestObject> {
	public static final String NAME = "resourcetest";

	private static final int VERSION = 0;

	public ResourceTestTable() {
		super(NAME, ResourceTestObject.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
