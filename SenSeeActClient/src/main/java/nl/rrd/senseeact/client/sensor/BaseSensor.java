package nl.rrd.senseeact.client.sensor;

import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.dao.DatabaseTableDef;

import java.util.List;

/**
 * Base class for sensor details. There is a subclass for each sensor product
 * (e.g. FITBIT). Instances can be obtained from the sensors in a {@link
 * BaseProject BaseProject}.
 * 
 * <p>A sensor may implement specific sensor type interfaces to define extra
 * properties, such as step counting sensors.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public abstract class BaseSensor {
	private final String product;

	/**
	 * Constructs a new instance.
	 * 
	 * @param product the sensor product ID. For example "FITBIT".
	 */
	protected BaseSensor(String product) {
		this.product = product;
	}

	/**
	 * Returns the sensor product ID. For example "FITBIT".
	 * 
	 * @return the sensor product ID
	 */
	public String getProduct() {
		return product;
	}

	/**
	 * Returns the database tables to which this sensor writes data. If a
	 * project doesn't have these tables (see {@link
	 * BaseProject#getDatabaseTables() BaseProject.getDatabaseTables()}, then
	 * the sensor cannot be used in that project.
	 * 
	 * @return the database tables
	 */
	public abstract List<DatabaseTableDef<?>> getTables();
	
	@Override
	public String toString() {
		return product;
	}
}
