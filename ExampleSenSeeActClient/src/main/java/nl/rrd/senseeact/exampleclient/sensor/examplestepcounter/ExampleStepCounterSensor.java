package nl.rrd.senseeact.exampleclient.sensor.examplestepcounter;

import nl.rrd.senseeact.client.sensor.BaseSensor;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.senseeact.exampleclient.sensor.SensorProduct;

import java.util.ArrayList;
import java.util.List;

public class ExampleStepCounterSensor extends BaseSensor {
	public ExampleStepCounterSensor() {
		super(SensorProduct.EXAMPLE_STEP_COUNTER);
	}

	@Override
	public List<DatabaseTableDef<?>> getTables() {
		List<DatabaseTableDef<?>> tables = new ArrayList<>();
		tables.add(new ExampleStepsMinuteTable());
		tables.add(new ExampleStepsDayTable());
		return tables;
	}
}
