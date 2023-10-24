package nl.rrd.senseeact.client.model.compat;

import nl.rrd.utils.json.JsonObject;

/**
 * Specification of a sensor to use in a project. It can specify a sensor type
 * (e.g. activity, heart rate, weight) so the user can choose any supported
 * product, or a sensor product (e.g. Promove, Fitbit).
 *
 * @author Dennis Hofs (RRD)
 */
public class ProjectSensorV0 extends JsonObject {
	private SensorTypeCompat type = null;
	private String product = null;

	/**
	 * Constructs a new empty sensor specification.
	 */
	public ProjectSensorV0() {
	}

	/**
	 * Constructs a new sensor specification with a sensor type.
	 *
	 * @param type the sensor type
	 */
	public ProjectSensorV0(SensorTypeCompat type) {
		this.type = type;
	}

	/**
	 * Constructs a new sensor specification with a sensor product.
	 *
	 * @param product the sensor product or null
	 */
	public ProjectSensorV0(String product) {
		this.product = product;
	}

	/**
	 * Returns the sensor type. This should be null if a sensor product is
	 * specified.
	 *
	 * @return the sensor type or null
	 */
	public SensorTypeCompat getType() {
		return type;
	}

	/**
	 * Sets the sensor type. This should be null if a sensor product is
	 * specified.
	 *
	 * @param type the sensor type or null
	 */
	public void setType(SensorTypeCompat type) {
		this.type = type;
	}

	/**
	 * Returns the sensor product. This should be null if a sensor type is
	 * specified.
	 *
	 * @return the sensor product or null
	 */
	public String getProduct() {
		return product;
	}

	/**
	 * Sets the sensor product. This should be null if a sensor type is
	 * specified.
	 *
	 * @param product the sensor product or null
	 */
	public void setProduct(String product) {
		this.product = product;
	}
}
