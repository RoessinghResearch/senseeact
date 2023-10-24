package nl.rrd.senseeact.client.project;

import nl.rrd.utils.i18n.I18n;
import nl.rrd.utils.i18n.I18nLoader;
import nl.rrd.senseeact.client.sensor.BaseSensor;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.senseeact.dao.sync.DatabaseSynchronizer;

import java.util.*;

/**
 * This class defines project-specific details that are needed on the client
 * side. This includes the project name that can be presented to the user,
 * sensor specifications and the database structure. You can obtain instances
 * of this class from the {@link ProjectRepository ProjectRepository}.
 * 
 * <p><b>Project name</b></p>
 * 
 * <p>Every project has a default name that can be obtained with {@link
 * #getName() getName()}. But an app should call {@link #getLocaleName(Locale)
 * getLocaleName()}, passing the preferred locale of the user interface.
 * Locale-dependent names can be defined in the messages*.properties resources
 * of the RRDSenSeeActClient, at key project.CODE.name. The resource is located using
 * {@link I18nLoader I18nLoader}.</p>
 * 
 * <p><b>Database</b></p>
 * 
 * <p>SenSeeAct uses DataAccessObjects (see {@link Database Database}) for its
 * databases. This class defines the data tables for each database. To enable
 * synchronisation using {@link DatabaseSynchronizer DatabaseSynchronizer},
 * the database on the client side and the server side should have the same
 * structure.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public abstract class BaseProject {
	private final String code;
	private final String name;

	/**
	 * Constructs a new project.
	 * 
	 * @param code the project code
	 * @param name the default name (this is used if no locale-dependent name
	 * is defined)
	 */
	public BaseProject(String code, String name) {
		this.code = code;
		this.name = name;
	}

	/**
	 * Returns the project code.
	 * 
	 * @return the project code
	 */
	public String getCode() {
		return code;
	}
	
	/**
	 * Returns the default name to present to the user. An app should not call
	 * this method, but {@link #getLocaleName(Locale) getLocaleName()}. It
	 * returns the default name if no locale-dependent name is defined.
	 * 
	 * @return the default name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * If this project supports logins by username (only the local part of an
	 * email address), then this method should return the domain. For example
	 * if this method returns "myproject.example.com", then user
	 * myprojectuser01@myproject.example.com can log in with myprojectuser01.
	 * The default implementation returns null.
	 * 
	 * @return the username domain or null
	 */
	public String getUsernameDomain() {
		return null;
	}
	
	/**
	 * Returns the project name to present to the user in the specified locale.
	 * It will use {@link I18nLoader I18nLoader} to locate the
	 * messages*.properties for the locale and it finds key project.CODE.name,
	 * where CODE is the project code. If no locale is specified or the string
	 * is not found, this method returns the default name.
	 * 
	 * @param locale the locale or null
	 * @return the project name
	 */
	public String getLocaleName(Locale locale) {
		if (locale == null)
			return name;
		I18n i18n = I18nLoader.getInstance().getI18n("messages", locale, true,
				null);
		String msgCode = "project." + code + ".name";
		String name = i18n.get(msgCode);
		if (name.equals(msgCode))
			return getName();
		else
			return name;
	}

	/**
	 * Finds the table with the specified name and returns the table definition.
	 * If the table doesn't exist, this method returns null.
	 * 
	 * @param name the table name
	 * @return the table definition or null
	 */
	public DatabaseTableDef<?> findTable(String name) {
		List<DatabaseTableDef<?>> tables = getDatabaseTables();
		if (tables == null)
			return null;
		for (DatabaseTableDef<?> table : tables) {
			if (table.getName().equals(name))
				return table;
		}
		return null;
	}

	/**
	 * Returns the sensors that are used in this project.
	 *
	 * @return the sensors (can be empty or null)
	 */
	public abstract List<BaseSensor> getSensors();
	
	/**
	 * Tries to find the sensor details for the specified product. If this
	 * project does not support the product, then this method returns null.
	 * 
	 * @param product the sensor product ID. For example "FITBIT".
	 * @return the sensor or null
	 */
	public BaseSensor findSensor(String product) {
		for (BaseSensor sensor : getSensors()) {
			if (sensor.getProduct().equals(product))
				return sensor;
		}
		return null;
	}

	/**
	 * Returns the sensors that are used in this project for the specified
	 * user. By default it returns the same list as {@link #getSensors()
	 * getSensors()}. This method may remove sensors if the user has simulated
	 * data for example.
	 *
	 * @param user the user
	 * @return the sensors (can be empty or null)
	 */
	public List<BaseSensor> getUserSensors(String user) {
		return getSensors();
	}

	/**
	 * Returns the database tables. An implementation of this method can call
	 * {@link #getSensorDatabaseTables() getSensorDatabaseTables()} and then add
	 * any additional tables for this project.
	 * 
	 * @return the database tables (can be empty or null)
	 */
	public abstract List<DatabaseTableDef<?>> getDatabaseTables();
	
	/**
	 * Returns the tables that are needed for the sensors returned by
	 * {@link #getSensors() getSensors()}. This can be useful for the
	 * implementation of {@link #getDatabaseTables() getDatabaseTables()}.
	 * 
	 * @return the sensor database tables
	 */
	protected List<DatabaseTableDef<?>> getSensorDatabaseTables() {
		List<DatabaseTableDef<?>> tables = new ArrayList<>();
		List<String> tableNames = new ArrayList<>();
		for (BaseSensor product : getSensors()) {
			for (DatabaseTableDef<?> table : product.getTables()) {
				if (!tableNames.contains(table.getName())) {
					tables.add(table);
					tableNames.add(table.getName());
				}
			}
		}
		return tables;
	}

	/**
	 * Returns the names of the tables.
	 *
	 * @return the names of the tables (can be empty)
	 */
	public List<String> getDatabaseTableNames() {
		List<String> result = new ArrayList<>();
		List<? extends DatabaseTableDef<?>> tables = getDatabaseTables();
		if (tables == null)
			return result;
		for (DatabaseTableDef<?> table : tables) {
			result.add(table.getName());
		}
		return result;
	}

	/**
	 * Returns a map with all data modules supported by this project. Each value
	 * is a list of database tables that belong to the module. This can be used
	 * for access control by module.
	 *
	 * @return the modules
	 */
	public Map<String,List<DatabaseTableDef<?>>> getModuleTables() {
		return new LinkedHashMap<>();
	}

	/**
	 * Finds the names of the modules that contain the specified table.
	 *
	 * @param table the table name
	 * @return the module names
	 */
	public Set<String> findModulesForTable(String table) {
		Set<String> modules = new HashSet<>();
		Map<String,List<DatabaseTableDef<?>>> moduleTables = getModuleTables();
		for (String module : moduleTables.keySet()) {
			List<DatabaseTableDef<?>> tables = moduleTables.get(module);
			for (DatabaseTableDef<?> moduleTable : tables) {
				if (moduleTable.getName().equals(table)) {
					modules.add(module);
					break;
				}
			}
		}
		return modules;
	}
}
