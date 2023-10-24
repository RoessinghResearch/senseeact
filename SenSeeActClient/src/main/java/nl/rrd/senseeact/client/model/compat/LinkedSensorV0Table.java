package nl.rrd.senseeact.client.model.compat;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class LinkedSensorV0Table extends DatabaseTableDef<LinkedSensorV0> {
	public static final String NAME = "linked_sensors";
	
	private static final int VERSION = 0;
	
	public LinkedSensorV0Table() {
		super(NAME, LinkedSensorV0.class, VERSION, true);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
