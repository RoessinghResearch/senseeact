package nl.rrd.senseeact.dao;

import nl.rrd.utils.exception.DatabaseException;

public class PrimitiveTestTable extends DatabaseTableDef<PrimitiveTestObject> {
	public static final String NAME = "primitivetest";

	private static final int VERSION = 0;

	public PrimitiveTestTable(boolean splitByUser) {
		super(NAME, PrimitiveTestObject.class, VERSION, splitByUser);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
