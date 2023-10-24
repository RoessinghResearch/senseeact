package nl.rrd.senseeact.dao;

import nl.rrd.utils.exception.DatabaseException;

public class SimpleTestTable extends DatabaseTableDef<SimpleTestObject> {
	public static final String NAME = "simpletest";

	private static final int VERSION = 0;

	public SimpleTestTable(boolean splitByUser) {
		super(NAME, SimpleTestObject.class, VERSION, splitByUser);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
