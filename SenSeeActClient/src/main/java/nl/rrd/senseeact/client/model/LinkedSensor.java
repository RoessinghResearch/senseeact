package nl.rrd.senseeact.client.model;

import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpClientException;
import nl.rrd.senseeact.client.SenSeeActClient;
import nl.rrd.senseeact.client.exception.SenSeeActClientException;
import nl.rrd.senseeact.client.model.compat.LinkedSensorV0;
import nl.rrd.senseeact.client.model.compat.LinkedSensorV0Table;
import nl.rrd.senseeact.client.model.sample.UTCSample;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.sensor.BaseSensor;
import nl.rrd.senseeact.dao.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
 * <p>This class is an upgrade to {@link LinkedSensorV0 LinkedSensorV0}, which
 * did not contain sensor specifications. This class is used since Activity
 * Coach v3.5, which has a new method of linking sensors. Any running projects
 * kept the older table of linked sensors in addition to the new table. The
 * findLinkedSensor methods check the older table as well.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class LinkedSensor extends UTCSample {
	@DatabaseField(DatabaseType.STRING)
	private String sensorId = null;
	
	@DatabaseField(DatabaseType.STRING)
	private String sensor;
	
	@DatabaseField(DatabaseType.TEXT)
	private String sensorSpecs = null;
	
	@DatabaseField(DatabaseType.BYTE)
	private boolean linked;
	
	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public LinkedSensor() {
	}
	
	/**
	 * Constructs a new sample at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 * 
	 * @param user the user (user ID)
	 * @param tzTime the time
	 */
	public LinkedSensor(String user, ZonedDateTime tzTime) {
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
	 * Sets the sensor ID. This field is retained for backward compatibility but
	 * should no longer be used.
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
	 * @param sensor the sensor product ID
	 */
	public void setSensor(String sensor) {
		this.sensor = sensor;
	}

	/**
	 * Returns details about the linked sensor. This could be a Bluetooth MAC
	 * address or a JSON string with a more complex structure. The format
	 * depends on the sensor product. It could also be null (default).
	 * 
	 * @return the sensor specifications or null
	 */
	public String getSensorSpecs() {
		return sensorSpecs;
	}

	/**
	 * Sets details about the linked sensor. This could be a Bluetooth MAC
	 * address or a JSON string with a more complex structure. The format
	 * depends on the sensor product. It could also be null (default).
	 * 
	 * @param sensorSpecs the sensor specifications or null
	 */
	public void setSensorSpecs(String sensorSpecs) {
		this.sensorSpecs = sensorSpecs;
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
	 * Returns details about the sensor that was last linked at or before the
	 * specified date (if specified). It considers sensors from the specified
	 * list of products. It does not consider whether the sensor has been
	 * unlinked since it was last linked. If no sensor was linked, this method
	 * returns null.
	 * 
	 * <p>This method checks both the new table "linked_sensors_v1" and the old
	 * table "linked_sensors" if they exist.</p>
	 * 
	 * <p>If the sensor is read from the old table, the returned object will not
	 * have a database object ID and sensorSpecs.</p>
	 * 
	 * @param db the database
	 * @param user the user
	 * @param date the date or null
	 * @param products the sensor products
	 * @return the sensor or null
	 * @throws DatabaseException if a database error occurs
	 */
	public static LinkedSensor findLinkedSensor(Database db, String user,
			LocalDate date, String... products)
			throws DatabaseException {
		List<String> tables = db.selectTables();
		boolean hasNewTable = tables.contains(LinkedSensorTable.NAME);
		boolean hasOldTable = tables.contains(LinkedSensorV0Table.NAME);
		List<DatabaseCriteria> orList = new ArrayList<>();
		for (String product : products) {
			orList.add(new DatabaseCriteria.Equal("sensor", product));
		}
		List<DatabaseCriteria> andCriteria = new ArrayList<>();
		andCriteria.add(new DatabaseCriteria.Equal("user", user));
		andCriteria.add(new DatabaseCriteria.Or(orList.toArray(
				new DatabaseCriteria[0])));
		andCriteria.add(new DatabaseCriteria.Equal("linked", 1));
		if (date != null) {
			String timeSuffix = "T00:00:00.000";
			DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(
					"yyyy-MM-dd");
			String end = date.plusDays(1).format(dateFormat) + timeSuffix;
			andCriteria.add(new DatabaseCriteria.LessThan("localTime", end));
		}
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				andCriteria.toArray(new DatabaseCriteria[0]));
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("utcTime", false)
		};
		if (hasNewTable) {
			LinkedSensor linkedSensor = db.selectOne(new LinkedSensorTable(),
					criteria, sort);
			if (linkedSensor != null)
				return linkedSensor;
		}
		if (hasOldTable) {
			LinkedSensorV0 linkedSensor = db.selectOne(
					new LinkedSensorV0Table(), criteria, sort);
			if (linkedSensor != null)
				return linkedSensor.toLinkedSensor();
		}
		return null;
	}

	public static LinkedSensor findLinkedSensorWithClass(Database db,
			BaseProject project, String user, LocalDate date, Class<?> clazz)
			throws DatabaseException {
		String[] products = getProductsForSensorClass(project, clazz);
		if (products.length == 0)
			return null;
		return findLinkedSensor(db, user, date, products);
	}

	/**
	 * Returns details about the sensor that was last linked at or before the
	 * specified date (if specified). It considers sensors from the specified
	 * list of products. It does not consider whether the sensor has been
	 * unlinked since it was last linked. If no sensor was linked, this method
	 * returns null.
	 * 
	 * <p>This method checks both the new table "linked_sensors_v1" and the old
	 * table "linked_sensors" if they exist.</p>
	 * 
	 * <p>If the sensor is read from the old table, the returned object will not
	 * have a database object ID and sensorSpecs.</p>
	 * 
	 * @param client the SenSeeAct client
	 * @param project the project
	 * @param user the user
	 * @param date the date or null
	 * @param products the sensor products
	 * @return the sensor or null
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public static LinkedSensor findLinkedSensor(SenSeeActClient client,
			BaseProject project, String user, LocalDate date,
			String... products) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		List<String> tables = project.getDatabaseTableNames();
		boolean hasNewTable = tables.contains(LinkedSensorTable.NAME);
		boolean hasOldTable = tables.contains(LinkedSensorV0Table.NAME);
		List<DatabaseCriteria> orList = new ArrayList<>();
		for (String product : products) {
			orList.add(new DatabaseCriteria.Equal("sensor", product));
		}
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("linked", 1),
				new DatabaseCriteria.Or(orList.toArray(new DatabaseCriteria[0]))
		);
		if (hasNewTable) {
			LinkedSensor linkedSensor = client.getLastRecord(project.getCode(),
					LinkedSensorTable.NAME, user, null,
					date == null ? null : date.plusDays(1), criteria, null,
					LinkedSensor.class);
			if (linkedSensor != null)
				return linkedSensor;
		}
		if (hasOldTable) {
			LinkedSensorV0 linkedSensor = client.getLastRecord(
					project.getCode(), LinkedSensorV0Table.NAME, user, null,
					date == null ? null : date.plusDays(1), criteria, null,
					LinkedSensorV0.class);
			if (linkedSensor != null)
				return linkedSensor.toLinkedSensor();
		}
		return null;
	}

	public static LinkedSensor findLinkedSensorWithClass(SenSeeActClient client,
			BaseProject project, String user, LocalDate date, Class<?> clazz)
			throws SenSeeActClientException, HttpClientException, ParseException,
			IOException {
		String[] products = getProductsForSensorClass(project, clazz);
		if (products.length == 0)
			return null;
		return findLinkedSensor(client, project, user, date, products);
	}

	private static String[] getProductsForSensorClass(
			BaseProject project, Class<?> clazz) {
		List<String> products = new ArrayList<>();
		for (BaseSensor sensor : project.getSensors()) {
			if (clazz.isInstance(sensor))
				products.add(sensor.getProduct());
		}
		return products.toArray(new String[0]);
	}
}
