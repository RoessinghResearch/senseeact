package nl.rrd.senseeact.exampleclient.sensor.examplestepcounter;

import nl.rrd.senseeact.client.model.sample.IntListSample;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.utils.exception.DatabaseException;

public class ExampleStepsMinuteTable extends DatabaseTableDef<IntListSample> {
	public static final String NAME = "example_steps_minute";

	private static final int VERSION = 0;

	public ExampleStepsMinuteTable() {
		super(NAME, IntListSample.class, VERSION, true);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
