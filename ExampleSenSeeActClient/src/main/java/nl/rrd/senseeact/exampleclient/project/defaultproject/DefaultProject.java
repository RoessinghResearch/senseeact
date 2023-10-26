package nl.rrd.senseeact.exampleclient.project.defaultproject;

import nl.rrd.senseeact.client.model.LinkedSensorTable;
import nl.rrd.senseeact.client.model.notification.AppNotificationTable;
import nl.rrd.senseeact.client.model.questionnaire.QuestionnaireDataTable;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.sensor.BaseSensor;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.senseeact.exampleclient.sensor.examplestepcounter.ExampleStepCounterSensor;

import java.util.ArrayList;
import java.util.List;

public class DefaultProject extends BaseProject {
	public DefaultProject() {
		super("default", "Default");
	}

	@Override
	public List<BaseSensor> getSensors() {
		List<BaseSensor> sensors = new ArrayList<>();
		sensors.add(new ExampleStepCounterSensor());
		return sensors;
	}

	@Override
	public List<DatabaseTableDef<?>> getDatabaseTables() {
		List<DatabaseTableDef<?>> result = new ArrayList<>();
		result.addAll(getSensorDatabaseTables());
		result.add(new QuestionnaireDataTable());
		result.add(new LinkedSensorTable());
		result.add(new AppNotificationTable());
		return result;
	}
}
