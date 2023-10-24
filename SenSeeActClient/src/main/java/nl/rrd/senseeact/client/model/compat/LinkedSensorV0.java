package nl.rrd.senseeact.client.model.compat;

import nl.rrd.senseeact.client.model.LinkedSensor;
import nl.rrd.senseeact.client.model.sample.UTCSample;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.sensor.BaseSensor;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.ZonedDateTime;

/**
 * This sample indicates that the user linked or unlinked a sensor within a
 * project. The sensor should be derived from a {@link BaseSensor BaseSensor}
 * within a {@link BaseProject BaseProject}. The sample time indicates the time
 * when the sensor was linked or unlinked.
 *
 * <p>An unlinked sensor event only occurs if the user explicitly unlinks a
 * specific sensor, not when the user logs out from the app. A cloud sensor such
 * as Fitbit for example, remains linked when a user logs out.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class LinkedSensorV0 extends UTCSample {
	@DatabaseField(DatabaseType.STRING)
	private String sensorId;
	
	@DatabaseField(DatabaseType.STRING)
	private String sensor;
	
	@DatabaseField(DatabaseType.BYTE)
	private boolean linked;
	
	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public LinkedSensorV0() {
	}
	
	/**
	 * Constructs a new sample at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 * 
	 * @param user the user (user ID)
	 * @param tzTime the time
	 */
	public LinkedSensorV0(String user, ZonedDateTime tzTime) {
		super(user, tzTime);
	}

	/**
	 * Returns the sensor ID. This field is retained for backward compatibility
	 * but should no longer be used.
	 * 
	 * @return the sensor ID
	 */
	public String getSensorId() {
		return sensorId;
	}

	/**
	 * Sets the sensor ID. This field is retained for backward compatibility
	 * but should no longer be used.
	 * 
	 * @param sensorId the sensor ID
	 */
	public void setSensorId(String sensorId) {
		this.sensorId = sensorId;
	}

	/**
	 * Returns the sensor product ID. For example "FITBIT".
	 * 
	 * @return the sensor product ID
	 */
	public String getSensor() {
		return sensor;
	}

	/**
	 * Sets the sensor product ID. For example "FITBIT".
	 * 
	 * @param sensor the sensor product
	 */
	public void setSensor(String sensor) {
		this.sensor = sensor;
	}

	/**
	 * Returns whether the sensor was linked or unlinked.
	 * 
	 * @return true if the sensor was linked, false if it was unlinked
	 */
	public boolean isLinked() {
		return linked;
	}

	/**
	 * Sets whether the sensor was linked or unlinked.
	 * 
	 * @param linked true if the sensor was linked, false if it was unlinked
	 */
	public void setLinked(boolean linked) {
		this.linked = linked;
	}
	
	/**
	 * Converts this object to a {@link LinkedSensor LinkedSensor} object. The
	 * result will not have an ID and sensorSpecs.
	 * 
	 * @return the LinkedSensor object
	 */
	public LinkedSensor toLinkedSensor() {
		LinkedSensor result = new LinkedSensor();
		result.updateDateTime(toDateTime());
		result.setSensorId(sensorId);
		result.setSensor(sensor);
		result.setLinked(linked);
		return result;
	}
}
