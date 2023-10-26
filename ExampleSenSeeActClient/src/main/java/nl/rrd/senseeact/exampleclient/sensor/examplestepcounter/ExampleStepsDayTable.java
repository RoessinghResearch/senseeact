package nl.rrd.senseeact.exampleclient.sensor.examplestepcounter;

import nl.rrd.senseeact.client.model.sample.IntLocalSample;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.utils.exception.DatabaseException;

public class ExampleStepsDayTable extends DatabaseTableDef<IntLocalSample> {
	public static final String NAME = "example_steps_day";

	private static final int VERSION = 0;

	public ExampleStepsDayTable() {
		super(NAME, IntLocalSample.class, VERSION, true);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
