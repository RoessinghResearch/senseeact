package nl.rrd.senseeact.client.model;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class LinkedSensorTable extends DatabaseTableDef<LinkedSensor> {
	public static final String NAME = "linked_sensors_v1";
	
	private static final int VERSION = 0;
	
	public LinkedSensorTable() {
		super(NAME, LinkedSensor.class, VERSION, true);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
