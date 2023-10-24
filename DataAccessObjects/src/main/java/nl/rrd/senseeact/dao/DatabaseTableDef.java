package nl.rrd.senseeact.dao;

import nl.rrd.utils.exception.DatabaseException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class models the definition of a (logical) database table. It consists
 * of a table name, data class and current version number. Subclasses should
 * implement {@link #upgradeTable(int, Database, String) upgradeTable()} so
 * that a database can be automatically upgraded from any previous version to
 * the latest version. Table names that start with an underscore are reserved.
 *
 * <p>At construction you should specify parameter "splitByUser". If this is
 * false, the logical table will be stored in one physical table in the
 * underlying database. If "splitByUser" is true, there will be a separate
 * physical table per user. This can significantly improve performance on large
 * tables. As a rule of thumb, set "splitByUser" to false if you store only one
 * or a few records per user in the table. Otherwise set it to true. A
 * prerequisite is that the table must have a field "user" and the field must
 * always be assigned.</p>
 *
 * <p>The name specified in this class is a logical table name. The {@link
 * Database Database} resolves the physical table names by adding a key to the
 * logical name.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public abstract class DatabaseTableDef<T extends DatabaseObject> {
	private String name;
	private Class<T> dataClass;
	private List<DatabaseIndex> compoundIndexes;
	private int currentVersion;
	private boolean splitByUser;

	/**
	 * Constructs a new database table definition.
	 * 
	 * @param name the (logical) table name (lower case). Names that start with
	 * an underscore are reserved.
	 * @param dataClass the class of database objects that are stored in the
	 * table
	 * @param currentVersion the current version of this table definition
	 * @param splitByUser true if the table should be split by user, false
	 * otherwise. If this is true, the table must have a field "user" and the
	 * field must always be assigned.
	 */
	public DatabaseTableDef(String name, Class<T> dataClass,
			int currentVersion, boolean splitByUser) {
		this.name = name;
		this.dataClass = dataClass;
		this.currentVersion = currentVersion;
		this.compoundIndexes = new ArrayList<>();
		this.splitByUser = splitByUser;
	}
	
	/**
	 * Returns the (logical) table name (lower case). Names that start with an
	 * underscore are reserved.
	 * 
	 * @return the (logical) table name (lower case)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the class of database objects that are stored in the table.
	 * 
	 * @return the data class
	 */
	public Class<T> getDataClass() {
		return dataClass;
	}

	/**
	 * Returns the current version of this table definition.
	 * 
	 * @return the current version of this table definition
	 */
	public int getCurrentVersion() {
		return currentVersion;
	}
	
	/**
	 * Adds a compound index to this table. Single-field indexes can be defined
	 * with {@link DatabaseField DatabaseField}.
	 * 
	 * @param index the compound index
	 */
	public void addCompoundIndex(DatabaseIndex index) {
		compoundIndexes.add(index);
	}
	
	/**
	 * Returns the compound indexes for this table.
	 * 
	 * @return the compound indexes
	 */
	public List<DatabaseIndex> getCompoundIndexes() {
		return compoundIndexes;
	}
	
	/**
	 * Upgrades a physical database table from the specified version to the
	 * next version. This method should support all previous versions, so the
	 * table can be upgraded to the current version, possibly through multiple
	 * calls of this method with increasing versions.
	 * 
	 * <p>The table may already have been (partially or completely) upgraded.
	 * This happens if a previous upgrade was run, but it stopped before the
	 * new version was saved in the metadata table. This method should be able
	 * to handle partially upgraded tables.</p>
	 * 
	 * <p>Since upgrades are run before the database is initialized, you can
	 * pass the physical table name to methods of the {@link Database
	 * Database}.</p>
	 * 
	 * @param version the version of this table in the specified database
	 * @param db the database
	 * @param physTable the physical table name
	 * @return the version after the upgrade
	 * @throws DatabaseException if a database error occurs
	 */
	public abstract int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException;
	
	/**
	 * Upgrades a database action table ({@link DatabaseActionTable
	 * DatabaseActionTable}) from the specified version to the next version.
	 * This method should support all previous versions, so the table can be
	 * upgraded to the current version, possibly through multiple calls of this
	 * method with increasing versions.
	 * 
	 * <p>The table may already have been (partially or completely) upgraded.
	 * This happens if a previous upgrade was run, but it stopped before the
	 * new version was saved in the metadata table. This method should be able
	 * to handle partially upgraded tables.</p>
	 * 
	 * <p>The default implementation of this method just returns version + 1,
	 * until the current version. Subclasses can override this method.</p>
	 * 
	 * @param version the version of this table in the specified database
	 * @param db the database
	 * @param table the action table
	 * @return the version after the upgrade
	 * @throws DatabaseException if a database error occurs
	 */
	public int upgradeActionTable(int version, Database db,
			DatabaseActionTable table) throws DatabaseException {
		if (version < getCurrentVersion())
			return version + 1;
		else
			return getCurrentVersion();
	}

	/**
	 * Returns whether the table should be split by user. If true, the
	 * underlying database will have a separate table for each user. This can
	 * significantly improve performance on large tables.
	 * 
	 * <p>If this is true, the table must have a field "user" and the field
	 * must always be assigned.</p>
	 * 
	 * @return true if the table should be split by user, false otherwise
	 */
	public boolean isSplitByUser() {
		return splitByUser;
	}
}
