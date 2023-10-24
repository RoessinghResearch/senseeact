package nl.rrd.senseeact.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import nl.rrd.utils.AppComponent;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.beans.PropertyReader;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.listener.DatabaseEvent;
import nl.rrd.senseeact.dao.listener.DatabaseListenerRepository;
import nl.rrd.senseeact.dao.sync.DatabaseActionMerger;
import nl.rrd.senseeact.dao.sync.MergeException;
import nl.rrd.senseeact.dao.sync.SyncProgressTableDef;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The base class for a database that can store {@link DatabaseObject
 * DatabaseObject}s. It was designed so that it can be implemented for SQL
 * databases as well as other databases such as MongoDB.
 * 
 * <p>Instances can be obtained from a {@link DatabaseConnection
 * DatabaseConnection}, which can be obtained from the {@link DatabaseFactory
 * DatabaseFactory}. The factory can be configured as an {@link AppComponent
 * AppComponent}.</p>
 *
 * <p>The database can log actions for synchronisation with a remote database.
 * If you enable synchronisation logging, every insert, update and delete action
 * is logged. The data associated with insert and update actions is also logged.
 * Finally it can log the user name to which the data is related. This is done
 * automatically when the data table has a column named "user". It can be used
 * for partial synchronisation of the data for one particular user only.</p>
 *
 * <p>It only logs actions on tables that are not reserved (start with an
 * underscore). The logs are written to the table _action_log (see {@link
 * DatabaseActionTable DatabaseActionTableDef}). You should normally enable
 * action logging after initialising the tables with {@link
 * #initTable(DatabaseTableDef) initTable()}. This is done by {@link
 * DatabaseConnection DatabaseConnection}.</p>
 * 
 * <p><b>Logical and physical tables</b></p>
 * 
 * <p>When you create a table with {@link #initTable(DatabaseTableDef)
 * initTable()}, you specify the name of the logical table. The table may
 * actually be stored in several physical tables in the database. This happens
 * in two cases:</p>
 * 
 * <p><ul>
 * <li>A table is split by user. This is defined by the {@link DatabaseTableDef
 * DatabaseTableDef} that you pass to {@link #initTable(DatabaseTableDef)
 * initTable()}. The database will have a separate physical table per user.
 * The name of the physical table will be the logical name plus a key
 * string.</li>
 * 
 * <li>A table has a complex field that some databases store as a separate
 * physical table or subtable, while other databases can store complex data
 * within the same table.</li>
 * </ul></p>
 * 
 * <p>The distinction is important because methods may expect the name of a
 * logical table or a physical table. This is specified in the documentation
 * of each method. There are some general guidelines determined by the
 * following two factors:</p>
 * 
 * <p><ul>
 * <li>Has the database been initialized? This is determined by {@link
 * #setDatabaseInitialised(boolean) setDatabaseInitialized()}. It is set
 * automatically by {@link DatabaseConnection DatabaseConnection} from {@link
 * DatabaseConnection#initDatabase(String, List, boolean) initDatabase()} or
 * {@link DatabaseConnection#getDatabase(String) getDatabase()}. It indicates
 * that any database upgrade has been run and the definition of the existing
 * database tables won't change anymore while the process is running. It's
 * still possible to create new tables though. As a simple rule, when you call
 * read or write methods from {@link
 * DatabaseTableDef#upgradeTable(int, Database, String)
 * DatabaseTableDef.upgradeTable()}, the database is not initialized. Otherwise
 * it is initialized.</li>
 * 
 * <li>Is the read or write method public or protected? The public methods
 * are called by a client. The protected methods are meant to be implemented
 * by a subclass. Their names start with "do".</li>
 * </ul></p>
 * 
 * <p>Now the public read/write methods when the database has been initialized,
 * expect logical table names. In other cases (database not initialized,
 * read/write method is protected), the methods expect physical table
 * names.</p>
 * 
 * <style type="text/css">
 *   table.bordered {
 *     border-collapse: collapse;
 *   }
 *   table.bordered th, table.bordered td {
 *     text-align: left;
 *     border: 1px solid black;
 *     padding: 4px 8px;
 *     vertical-align: top;
 *   }
 * </style>
 * <p><table class="bordered">
 * <caption>Read/write methods</caption>
 * <tbody><tr>
 * <th></th>
 * <th>read/write method is public</th>
 * <th>read/write method is protected</th>
 * </tr><tr>
 * <th>database not initialized</th>
 * <td>physical table name</td>
 * <td>physical table name</td>
 * </tr><tr>
 * <th>database initialized</th>
 * <td>logical table name</td>
 * <td>physical table name</td>
 * </tr></tbody>
 * </table>
 * 
 * @author Dennis Hofs (RRD)
 */
public abstract class Database {
	public static final String LOGTAG = Database.class.getSimpleName();
	public static final String TABLE_TOKEN_SEP = "__";

	private String name;
	
	private boolean syncEnabled = false;
	private boolean saveSyncedRemoteActions = true;

	////////////////////////////////////////////////////////////////////////////
	// flags to enable caching
	private boolean metaInitialised = false;
	private boolean databaseInitialised = false;

	/**
	 * Constructs a new database.
	 * 
	 * @param name the database name
	 */
	public Database(String name) {
		this.name = name;
	}

	/**
	 * Returns the database name.
	 * 
	 * @return the database name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns whether action logging is enabled for synchronization with
	 * a remote database.
	 *
	 * @return true if synchronization logging is enabled, false otherwise
	 */
	public boolean isSyncEnabled() {
		return syncEnabled;
	}

	/**
	 * Returns whether action logging should be enabled for synchronization
	 * with a remote database.
	 *
	 * @param syncEnabled true if synchronization logging is enabled, false
	 * otherwise
	 */
	public void setSyncEnabled(boolean syncEnabled) {
		this.syncEnabled = syncEnabled;
	}

	/**
	 * If synchronization logging is enabled (see {@link #isSyncEnabled()
	 * isSyncEnabled()}), this method returns whether database actions received
	 * from the remote database should be logged as well. If this is false,
	 * then only local database actions are logged. The default is true.
	 * 
	 * @return true if remote database actions are logged, false if only local
	 * database actions are logged
	 */
	public boolean isSaveSyncedRemoteActions() {
		return saveSyncedRemoteActions;
	}

	/**
	 * If synchronization logging is enabled (see {@link
	 * #setSyncEnabled(boolean) setSyncEnabled()}), this method determines
	 * whether database actions received from the remote database should be
	 * logged as well. If this is false, then only local database actions are
	 * logged. The default is true.
	 * 
	 * @param saveSyncedRemoteActions true if remote database actions are
	 * logged, false if only local database actions are logged
	 */
	public void setSaveSyncedRemoteActions(boolean saveSyncedRemoteActions) {
		this.saveSyncedRemoteActions = saveSyncedRemoteActions;
	}

	/**
	 * Returns whether initialisation of the database has been finished. This
	 * means that the database structure will not change anymore and related
	 * queries can be cached. This method just returns what has been set with
	 * {@link #setDatabaseInitialised(boolean) setDatabaseInitialised()}
	 * (default false). The {@link DatabaseConnection DatabaseConnection}
	 * controls this when you obtain a database from it.
	 *
	 * @return true if the initialisation of the database has been finished
	 */
	public boolean isDatabaseInitialised() {
		return databaseInitialised;
	}

	/**
	 * Sets whether initialisation of the database has been finished. This
	 * means that the database structure will not change anymore and related
	 * queries can be cached. The default is false. The {@link
	 * DatabaseConnection DatabaseConnection} controls this when you obtain a
	 * database from it.
	 *
	 * @param databaseInitialised true if the initialisation has been finished,
	 * false otherwise
	 */
	public void setDatabaseInitialised(boolean databaseInitialised) {
		this.databaseInitialised = databaseInitialised;
	}

	/**
	 * Initialises a table according to the specified definition. If the table
	 * does not exist, it will be created. If it exists, this method will get
	 * its version from the metadata table and then upgrade it, until it has the
	 * current version that is obtained from the definition. The current version
	 * will be written to the metadata table.
	 * 
	 * <p>This method automatically initialises the special metadata table as
	 * well. In fact, it will call itself and pass {@link TableMetadataTableDef
	 * TableMetadataTableDef}, which is handled specially. Users should not
	 * pass this class.</p>
	 *
	 * <p>When you have initialised the last table, you can call {@link
	 * #setDatabaseInitialised(boolean) setDatabaseInitialised()} to enable
	 * caching and get better performance. This is done automatically when you
	 * obtain a database from {@link DatabaseConnection
	 * DatabaseConnection}.</p>
	 * 
	 * @param tableDef the table definition
	 * @throws DatabaseException if a database error occurs
	 */
	public void initTable(DatabaseTableDef<?> tableDef)
			throws DatabaseException {
		if ((tableDef instanceof TableMetadataTableDef) && metaInitialised)
			return;
		TableMetadataTableDef metaDef = new TableMetadataTableDef();
		List<Class<? extends DatabaseTableDef<?>>> reservedTables =
				new ArrayList<>();
		reservedTables.add(UserTableKeyTable.class);
		reservedTables.add(SyncProgressTableDef.class);
		reservedTables.add(DatabaseActionMetaTable.class);
		List<String> tables = getCachedDbTables();
		if (tableDef instanceof TableMetadataTableDef) {
			if (!tables.contains(tableDef.getName()))
				createMetaTable(tableDef);
		} else if (!reservedTables.contains(tableDef.getClass()) &&
				!metaInitialised) {
			initTable(metaDef);
		}
		DatabaseCache cache = DatabaseCache.getInstance();
		List<TableMetadata> metas = cache.getTableMetadata(this,
				tableDef.getName());
		TableMetadata versionMeta = TableMetadata.findKey(metas,
				TableMetadata.KEY_VERSION);
		if (versionMeta == null) {
			for (String table : tables) {
				if (table.equals(tableDef.getName()) ||
						table.startsWith(tableDef.getName() +
						TABLE_TOKEN_SEP)) {
					dropCachedDbTable(table);
				}
			}
			createMetaTable(tableDef);
		} else {
			upgradeTable(tableDef, versionMeta);
			List<TableMetadata> newMetas = new ArrayList<>();
			newMetas.add(versionMeta);
			newMetas.add(cache.setTableFields(this, tableDef.getName(),
					DatabaseFieldScanner.getDatabaseFieldNames(
					tableDef.getDataClass()), TableMetadata.findKey(metas,
					TableMetadata.KEY_FIELDS)));
			newMetas.add(cache.setTableDataClass(this, tableDef.getName(),
					tableDef.getDataClass(), TableMetadata.findKey(metas,
					TableMetadata.KEY_DATA_CLASS)));
			newMetas.add(cache.setTableCompoundIndexes(this, tableDef.getName(),
					tableDef.getCompoundIndexes(), TableMetadata.findKey(metas,
					TableMetadata.KEY_INDEXES)));
			newMetas.add(cache.setTableSplitByUser(this, tableDef.getName(),
					tableDef.isSplitByUser(), TableMetadata.findKey(metas,
					TableMetadata.KEY_SPLIT_BY_USER)));
			cache.setTableMetadata(this, tableDef.getName(), newMetas);
		}
		if (tableDef instanceof TableMetadataTableDef) {
			for (Class<? extends DatabaseTableDef<?>> reservedClass :
					reservedTables) {
				DatabaseTableDef<?> reserved = null;
				Throwable exception = null;
				try {
					reserved = reservedClass.getConstructor().newInstance();
				} catch (NoSuchMethodException | IllegalAccessException |
						InstantiationException ex) {
					exception = ex;
				} catch (InvocationTargetException ex) {
					exception = ex.getCause();
				}
				if (exception != null) {
					throw new RuntimeException(
							"Can't initialize reserved table \"" +
							reservedClass.getName() + "\": " +
							exception.getMessage(), exception);
				}
				initTable(reserved);
			}
			metaInitialised = true;
		}
	}
	
	/**
	 * Creates the specified physical database action table, if it doesn't
	 * already exist.
	 * 
	 * @param table the table
	 * @throws DatabaseException if a database error occurs
	 */
	protected void createActionTable(DatabaseActionTable table)
			throws DatabaseException {
		if (getCachedDbTables().contains(table.getName()))
			return;
		DatabaseActionMetaTable metaTable = new DatabaseActionMetaTable();
		createTable(table.getName(), metaTable.getDataClass(),
				metaTable.getCompoundIndexes());
	}

	/**
	 * Creates a table for storing the specified type of objects. The table
	 * will have an ID column (primary key) and a column for each class field
	 * that is annotated with {@link DatabaseField DatabaseField}. The "id"
	 * field should not be annotated.
	 * 
	 * <p>The database table is only created if {@link
	 * DatabaseTableDef#isSplitByUser() tableDef.isSplitByUser()} returns
	 * false. Otherwise the tables are created when a query on a user is
	 * performed.</p>
	 * 
	 * <p>This method is called from {@link #initTable(DatabaseTableDef)
	 * initTable()} if the table does not exist in the metadata table yet. It
	 * will save table information to the metadata table and the database
	 * cache.</p>
	 * 
	 * @param tableDef the table definition
	 * @throws DatabaseException if a database error occurs
	 */
	private void createMetaTable(DatabaseTableDef<?> tableDef)
			throws DatabaseException {
		if (!(tableDef instanceof DatabaseActionMetaTable) &&
				!tableDef.isSplitByUser()) {
			createTable(tableDef.getName(), tableDef.getDataClass(),
					tableDef.getCompoundIndexes());
		}
		List<TableMetadata> metas = new ArrayList<>();
		TableMetadata meta = new TableMetadata();
		meta.setTable(tableDef.getName());
		meta.setKey(TableMetadata.KEY_VERSION);
		meta.setValue(Integer.toString(tableDef.getCurrentVersion()));
		insert(TableMetadataTableDef.NAME, meta);
		metas.add(meta);
		DatabaseCache cache = DatabaseCache.getInstance();
		cache.addLogicalTable(this, tableDef.getName());
		metas.add(cache.setTableFields(this, tableDef.getName(),
				DatabaseFieldScanner.getDatabaseFieldNames(
				tableDef.getDataClass()), null));
		metas.add(cache.setTableDataClass(this, tableDef.getName(),
				tableDef.getDataClass(), null));
		metas.add(cache.setTableCompoundIndexes(this, tableDef.getName(),
				tableDef.getCompoundIndexes(), null));
		metas.add(cache.setTableSplitByUser(this, tableDef.getName(),
				tableDef.isSplitByUser(), null));
		cache.setTableMetadata(this, tableDef.getName(), metas);
	}
	
	/**
	 * Creates a physical table for storing the specified type of objects. The
	 * table will have an ID column (primary key) and a column for each class
	 * field that is annotated with {@link DatabaseField DatabaseField}. The
	 * "id" field should not be annotated. This method also adds the specified
	 * compound indexes.
	 * 
	 * @param name the (physical) table name
	 * @param dataClass the data class
	 * @param compoundIndexes the compound indexes
	 * @throws DatabaseException if a database error occurs
	 */
	private void createTable(String name,
			Class<? extends DatabaseObject> dataClass,
			List<DatabaseIndex> compoundIndexes) throws DatabaseException {
		createTable(name, getDatabaseObjectColumns(dataClass));
		for (DatabaseIndex index : compoundIndexes) {
			createIndex(name, index);
		}
		DatabaseCache cache = DatabaseCache.getInstance();
		cache.addPhysicalTable(this, name);
	}
	
	/**
	 * Checks whether the specified logical table is up-to-date. If not, it
	 * will upgrade the related physical tables until it's up-to-date. If
	 * the table is upgraded, the specified version record from the metadata
	 * table will be updated.
	 * 
	 * @param tableDef the table definition
	 * @param versionMeta the version record from the metadata table (may be
	 * updated)
	 * @throws DatabaseException if a database error occurs
	 */
	private void upgradeTable(DatabaseTableDef<?> tableDef,
			TableMetadata versionMeta) throws DatabaseException {
		int version = Integer.parseInt(versionMeta.getValue());
		while (version < tableDef.getCurrentVersion()) {
			version = upgradeTable(tableDef, versionMeta, version);
		}
	}
	
	/**
	 * Upgrades the specified logical table by one step. The specified version
	 * must be the current table version according to the metadata table. It
	 * must be less than the current version according to the table definition.
	 * This method upgrades all related physical tables. After upgrading the
	 * tables it will update the version record in the metadata table and
	 * return the new version.
	 * 
	 * @param tableDef the table definition
	 * @param versionMeta the version record from the metadata table (will be
	 * updated)
	 * @param version the current version according to the metadata table
	 * @return the version after the upgrade 
	 * @throws DatabaseException if a database error occurs
	 */
	private int upgradeTable(DatabaseTableDef<?> tableDef,
			TableMetadata versionMeta, int version) throws DatabaseException {
		List<String> dbTables = getCachedDbTables();
		List<String> physTables = new ArrayList<>();
		// map from action table name to DatabaseActionTable
		Map<String,DatabaseActionTable> actionTables = new LinkedHashMap<>();
		if (tableDef instanceof DatabaseActionMetaTable) {
			List<UserTableKey> userTableKeys = select(new UserTableKeyTable(),
					null, 0, null);
			for (UserTableKey userTableKey : userTableKeys) {
				DatabaseActionTable table = new DatabaseActionTable(
						userTableKey);
				physTables.add(table.getName());
			}
		} else {
			physTables.add(tableDef.getName());
			if (!tableDef.getName().startsWith("_")) {
				DatabaseCriteria criteria = new DatabaseCriteria.Equal(
						"table", tableDef.getName());
				List<UserTableKey> userTableKeys = select(new UserTableKeyTable(),
						criteria, 0, null);
				for (UserTableKey userTableKey : userTableKeys) {
					physTables.add(tableDef.getName() + TABLE_TOKEN_SEP +
							userTableKey.getKey());
					DatabaseActionTable actionTable = new DatabaseActionTable(
							userTableKey);
					actionTables.put(actionTable.getName(), actionTable);
				}
			}
		}
		Integer minUpgradedVersion = null;
		for (String physTable : physTables) {
			if (dbTables.contains(physTable) || (!tableDef.isSplitByUser() &&
					physTable.equals(tableDef.getName()))) {
				if (!dbTables.contains(physTable)) {
					createTable(physTable, tableDef.getDataClass(),
							tableDef.getCompoundIndexes());
				}
				int upgradedVersion = tableDef.upgradeTable(version, this,
						physTable);
				if (minUpgradedVersion == null ||
						upgradedVersion < minUpgradedVersion) {
					minUpgradedVersion = upgradedVersion;
				}
			}
		}
		for (String actionTable : actionTables.keySet()) {
			if (dbTables.contains(actionTable)) {
				int upgradedVersion = tableDef.upgradeActionTable(version,
						this, actionTables.get(actionTable));
				if (minUpgradedVersion == null ||
						upgradedVersion < minUpgradedVersion) {
					minUpgradedVersion = upgradedVersion;
				}
			}
		}
		if (minUpgradedVersion == null)
			minUpgradedVersion = version + 1;
		versionMeta.setValue(minUpgradedVersion.toString());
		update(TableMetadataTableDef.NAME, versionMeta);
		return minUpgradedVersion;
	}

	/**
	 * Creates the database column definition for the specified database
	 * object class. This includes all class fields that are annotated with
	 * {@link DatabaseField DatabaseField} (this is not the id field).
	 *
	 * @param clazz the database object class
	 * @return the column definitions
	 */
	protected List<DatabaseColumnDef> getDatabaseObjectColumns(
			Class<? extends DatabaseObject> clazz) {
		List<DatabaseFieldSpec> fields =
				DatabaseFieldScanner.getDatabaseFields(clazz);
		List<DatabaseColumnDef> cols = new ArrayList<>();
		for (DatabaseFieldSpec fieldSpec : fields) {
			Field field = fieldSpec.getPropSpec().getField();
			DatabaseField annot = field.getAnnotation(DatabaseField.class);
			if (annot != null) {
				DatabaseColumnDef colDef = new DatabaseColumnDef(
						field.getName(), annot.value(), annot.index());
				cols.add(colDef);
			}
		}
		return cols;
	}
	
	/**
	 * Creates a physical table with an ID column (primary key) and the
	 * specified list of columns.
	 * 
	 * @param table the (physical) table name (lower case)
	 * @param columns the columns to create (without the ID column)
	 * @throws DatabaseException if a database error occurs
	 */
	protected abstract void createTable(String table,
			List<DatabaseColumnDef> columns)
			throws DatabaseException;
	
	/**
	 * Creates an index on a (physical) table. Single-field indices can be
	 * defined in the {@link DatabaseColumnDef DatabaseColumnDef} and they are
	 * created automatically in {@link #initTable(DatabaseTableDef)
	 * initTable()} or {@link #createTable(String, List) createTable()}.
	 * 
	 * @param table the (physical) table name (lower case)
	 * @param index the index
	 * @throws DatabaseException if a database error occurs
	 */
	public abstract void createIndex(String table, DatabaseIndex index)
			throws DatabaseException;
	
	/**
	 * Drops an index from a (physical) table.
	 * 
	 * @param table the (physical) table name (lower case)
	 * @param name the index name
	 * @throws DatabaseException if a database error occurs
	 */
	public abstract void dropIndex(String table, String name)
			throws DatabaseException;
	
	/**
	 * Adds a column to the specified (physical) table.
	 * 
	 * @param table the (physical) table name (lower case)
	 * @param column the column definition
	 * @throws DatabaseException if a database error occurs
	 */
	public abstract void addColumn(String table, DatabaseColumnDef column)
			throws DatabaseException;
	
	/**
	 * Drops a column from the specified (physical) table. The ID column should
	 * never be dropped.
	 * 
	 * @param table the (physical) table name (lower case)
	 * @param column the name of the column to drop
	 * @throws DatabaseException if a database error occurs
	 */
	public abstract void dropColumn(String table, String column)
			throws DatabaseException;
	
	/**
	 * Renames a column in the specified (physical) table. The ID column should
	 * never be renamed.
	 * 
	 * @param table the (physical) table name (lower case)
	 * @param oldName the old column name
	 * @param newName the new column name
	 * @throws DatabaseException if a database error occurs
	 */
	public abstract void renameColumn(String table, String oldName,
			String newName) throws DatabaseException;
	
	/**
	 * Selects all (logical) tables that have been created using {@link
	 * #initTable(DatabaseTableDef) initTable()}, including reserved tables
	 * (whose name start with an underscore). It reads the table names from
	 * the metadata table. It creates the metadata table if it doesn't exist.
	 * The returned table names are ordered by name.
	 * 
	 * @return the (logical) table names (lower case)
	 * @throws DatabaseException if a database error occurs
	 */
	public List<String> selectTables() throws DatabaseException {
		List<String> selectedTables = DatabaseCache.getInstance()
				.getLogicalTables(this);
		if (selectedTables != null)
			return selectedTables;
		TableMetadataTableDef metaDef = new TableMetadataTableDef();
		initTable(metaDef);
		List<TableMetadata> metas = select(metaDef, null, 0, null);
		List<String> tables = new ArrayList<>();
		for (TableMetadata meta : metas) {
			if (!tables.contains(meta.getTable()))
				tables.add(meta.getTable());
		}
		Collections.sort(tables);
		DatabaseCache.getInstance().setLogicalTables(this, tables);
		return new ArrayList<>(tables);
	}
	
	/**
	 * Returns the database tables. This is like {@link #selectDbTables()
	 * selectDbTables()} as it returns the physical tables in the underlying
	 * database, but it gets them from the cache and they may exclude subtables
	 * (a physical table for a database field within a {@link DatabaseObject
	 * DatabaseObject}).
	 * 
	 * <p>If the database tables are not in the cache yet, it will run {@link
	 * #selectDbTables() selectDbTables()} and save the result to the cache.
	 * This happens at the first call, or when the database is created again,
	 * either because it was dropped with {@link
	 * DatabaseConnection#dropDatabase(String)
	 * DatabaseConnection.dropDatabase()} or because it was dropped manually
	 * outside the application.</p>
	 * 
	 * <p>The returned list includes the following tables:</p>
	 * 
	 * <p><ul>
	 * <li>All tables that existed in the database when it was first
	 * initialized (including subtables)</li>
	 * <li>All tables where property splitByUser is false</li>
	 * <li>All user tables (for logical tables where property splitByUser is
	 * true)</li>
	 * </ul></p>
	 * 
	 * <p>Some databases don't return empty tables.</p>
	 * 
	 * @return the physical database tables
	 * @throws DatabaseException if a database error occurs
	 */
	private List<String> getCachedDbTables() throws DatabaseException {
		List<String> dbTables = DatabaseCache.getInstance()
				.getPhysicalTables(this);
		if (dbTables != null)
			return dbTables;
		dbTables = selectDbTables();
		DatabaseCache.getInstance().setPhysicalTables(this, dbTables);
		return new ArrayList<>(dbTables);
	}
	
	/**
	 * Selects the names of all physical tables, ordered by name. Some
	 * databases don't return empty tables.
	 * 
	 * @return the physical tables (lower case)
	 * @throws DatabaseException if a database error occurs
	 */
	protected abstract List<String> selectDbTables() throws DatabaseException;
	
	/**
	 * Drops the specified (logical) table. It deletes metadata, it drops
	 * related physical tables and action log tables, and it deletes the table
	 * from the {@link DatabaseCache DatabaseCache}.
	 * 
	 * @param table the (logical) table name (lower case)
	 * @throws DatabaseException if a database error occurs
	 */
	public void dropTable(String table) throws DatabaseException {
		List<String> dbTables = getCachedDbTables();
		for (String dbTable : dbTables) {
			if (table.equals(DatabaseActionMetaTable.NAME)) {
				if (dbTable.startsWith(table))
					dropCachedDbTable(dbTable);
			} else {
				if (dbTable.equals(table) || dbTable.startsWith(
						table + TABLE_TOKEN_SEP)) {
					dropCachedDbTable(dbTable);
				}
			}
		}
		DatabaseCriteria criteria = new DatabaseCriteria.Equal("table", table);
		delete(TableMetadataTableDef.NAME, null, criteria);
		List<UserTableKey> userTableKeys = select(new UserTableKeyTable(),
				criteria, 0, null);
		for (UserTableKey key : userTableKeys) {
			DatabaseActionTable actionTable = new DatabaseActionTable(key);
			if (dbTables.contains(actionTable.getName()))
				dropCachedDbTable(actionTable.getName());
		}
		delete(UserTableKeyTable.NAME, null, criteria);
		DatabaseCache.getInstance().removeLogicalTable(this, table);
	}
	
	/**
	 * Drops the specified physical table. This method calls {@link
	 * #dropDbTable(String) dropDbTable()} and then removes the table from the
	 * cache.
	 * 
	 * @param table the (physical) table name (lower case)
	 * @throws DatabaseException if a database error occurs
	 */
	public void dropCachedDbTable(String table) throws DatabaseException {
		dropDbTable(table);
		DatabaseCache.getInstance().removePhysicalTable(this, table);
	}

	/**
	 * Drops the specified physical table. It does not delete any related
	 * subtables or metadata or cached data. This method is called from {@link
	 * #dropCachedDbTable(String) dropCachedDbTable()} and should never be
	 * called from anywhere else.
	 * 
	 * @param table the (physical) table name (lower case)
	 * @throws DatabaseException if a database error occurs
	 */
	protected abstract void dropDbTable(String table)
			throws DatabaseException;
	
	/**
	 * Begins a transaction. Currently there is no rollback functionality, but
	 * a transaction can be used to speed up a sequence of write queries. At
	 * the end call {@link #commitTransaction() commitTransaction()}.
	 * 
	 * @throws DatabaseException if a database error occurs
	 */
	public abstract void beginTransaction() throws DatabaseException;

	/**
	 * Commits a transaction. This should be called after {@link
	 * #beginTransaction() beginTransaction()}.
	 * 
	 * @throws DatabaseException if a database error occurs
	 */
	public abstract void commitTransaction() throws DatabaseException;
	
	/**
	 * Inserts an object into a table. If the object ID is null, the database
	 * will generate an ID and set it in the object. This method writes the
	 * object fields that are annotated with {@link DatabaseField
	 * DatabaseField}. The "id" field should not be annotated.
	 * 
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table. Otherwise it should be
	 * a physical table.</p>
	 * 
	 * <p>If the table is a logical table split by user, then the object must
	 * define a field "user". The object will be inserted into the physical
	 * table for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the insert action to the table
	 * _action_log.</p>
	 *
	 * @param table the table name (lower case)
	 * @param value the object to insert
	 * @throws DatabaseException if a database error occurs
	 * @see DatabaseObjectMapper
	 */
	public void insert(String table, DatabaseObject value)
			throws DatabaseException {
		List<DatabaseObject> values = new ArrayList<>();
		values.add(value);
		insert(table, values);
	}

	/**
	 * Inserts one or more objects into a table. If an object ID is null, the
	 * database will generate an ID and set it in the object. This method
	 * writes the object fields that are annotated with {@link DatabaseField
	 * DatabaseField}. The "id" field should not be annotated.
	 * 
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table. Otherwise it should be
	 * a physical table.</p>
	 * 
	 * <p>If the table is a logical table split by user, then all objects must
	 * define a field "user" and all must have the same user. This is
	 * validated. The objects are inserted into the table for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the insert action to the table
	 * _action_log.</p>
	 *
	 * @param table the table name (lower case)
	 * @param values the objects to insert
	 * @throws DatabaseException if a database error occurs
	 * @see DatabaseObjectMapper
	 */
	public void insert(String table, List<? extends DatabaseObject> values)
			throws DatabaseException {
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		List<Map<String,Object>> maps = new ArrayList<>();
		for (DatabaseObject value : values) {
			maps.add(mapper.objectToMap(value, false));
		}
		insertMaps(table, maps);
		Iterator<Map<String,Object>> mapIt = maps.iterator();
		Iterator<? extends DatabaseObject> objIt = values.iterator();
		while (mapIt.hasNext()) {
			Map<String,Object> map = mapIt.next();
			DatabaseObject obj = objIt.next();
			obj.setId((String)map.get("id"));
		}
	}

	/**
	 * Inserts one or more records into a table. Each record is specified as
	 * a map. The keys are the column names. The values should be obtained with
	 * {@link DatabaseObjectMapper DatabaseObjectMapper}. If the map has a key
	 * "id", it will be used as the ID. Otherwise the database should generate
	 * an ID and then set it in the map.
	 * 
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table. Otherwise it should be
	 * a physical table.</p>
	 * 
	 * <p>If the table is a logical table split by user, then all maps must
	 * define a field "user" and all must have the same user. This is
	 * validated. The maps are inserted into the table for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the insert action to the table _action_log
	 * with source "local". See also {@link #insertMaps(String, List, String)
	 * insert(table, values, source)}.</p>
	 *
	 * @param table the table name (lower case)
	 * @param values the records to insert
	 * @throws DatabaseException if a database error occurs
	 */
	public void insertMaps(String table, List<Map<String,Object>> values)
			throws DatabaseException {
		insertMaps(table, values, DatabaseAction.SOURCE_LOCAL);
	}

	/**
	 * Inserts one or more records into a table. Each record is specified as
	 * a map. The keys are the column names. The values should be obtained with
	 * {@link DatabaseObjectMapper DatabaseObjectMapper}. If the map has a key
	 * "id", it will be used as the ID. Otherwise the database should generate
	 * an ID and then set it in the map.
	 * 
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table. Otherwise it should be
	 * a physical table.</p>
	 * 
	 * <p>If the table is a logical table split by user, then all maps must
	 * define a field "user" and all must have the same user. This is
	 * validated. The maps are inserted into the table for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the insert action to the table _action_log
	 * with the specified source.</p>
	 *
	 * <p>You should normally call {@link #insertMaps(String, List)
	 * insertMaps(table, values)}. This method with source parameter is used
	 * by synchronisers.</p>
	 *
	 * @param table the table name (lower case)
	 * @param values the records to insert
	 * @param source the source of the action. See {@link
	 * DatabaseAction#setSource(String) DatabaseAction.setSource()}.
	 * @throws DatabaseException if a database error occurs
	 */
	public void insertMaps(String table, List<Map<String,Object>> values,
			String source) throws DatabaseException {
		if (values.isEmpty())
			return;
		String dbTable = table;
		if (useSplitUserTable(table)) {
			String insertUser = null;
			for (Map<String,Object> map : values) {
				String currUser = getInsertUser(table, map);
				if (insertUser == null) {
					insertUser = currUser;
				} else if (!currUser.equals(insertUser)) {
					throw new DatabaseException(String.format(
							"Can't insert objects with different users into table \"%s\" that is split by user: %s, %s",
							table, insertUser, currUser));
				}
			}
			dbTable = getSplitUserTable(table, insertUser);
		}
		doInsertMaps(dbTable, values);
		boolean syncLog = syncEnabled && (
				source.equals(DatabaseAction.SOURCE_LOCAL) ||
				saveSyncedRemoteActions);
		if (!table.startsWith("_") && syncLog) {
			writeDatabaseActions(table, DatabaseAction.Action.INSERT, values,
					values, source);
		}
		if (!table.startsWith("_")) {
			DatabaseListenerRepository.getInstance().notifyDatabaseEvent(
					new DatabaseEvent.Insert(name, table, values));
		}
	}
	
	/**
	 * Inserts one or more records into a physical table. Each record is
	 * specified as a map. The keys are the column names. The values should be
	 * obtained with {@link DatabaseObjectMapper DatabaseObjectMapper}. If the
	 * map has a key "id", it will be used as the ID. Otherwise the database
	 * should generate an ID and then set it in the map.
	 *
	 * @param table the (physical) table name (lower case)
	 * @param values the records to insert
	 * @throws DatabaseException if a database error occurs
	 */
	protected abstract void doInsertMaps(String table,
			List<Map<String,Object>> values) throws DatabaseException;
	
	/**
	 * Returns whether a read/write query on the specified table should operate
	 * on a logical table that is split by user. If the database is not
	 * initialized or the table is reserved (its name starts with an
	 * underscore), this method always returns false. Otherwise it checks if
	 * the table is split by user.
	 * 
	 * @param table the table (lower case)
	 * @return true if the database is initialized, the table is not reserved,
	 * and the table is split by user
	 * @throws DatabaseException if the table is not found
	 */
	private boolean useSplitUserTable(String table) throws DatabaseException {
		if (!databaseInitialised || table.startsWith("_"))
			return false;
		if (!selectTables().contains(table))
			throw new DatabaseException("Table \"" + table + "\" not found");
		return DatabaseCache.getInstance().isTableSplitByUser(this, table);
	}
	
	/**
	 * This method takes the name of a logical table that is split by user. It
	 * returns the name of the physical table for the specified user. If the
	 * physical table doesn't exist, it will be created.
	 * 
	 * <p>This method requires that the metadata table contains the data class
	 * and compound indexes, so it should not be called before a table
	 * upgrade.</p>
	 * 
	 * @param table the name of the logical table that is split by user
	 * @param user the user
	 * @return the physical table name
	 * @throws DatabaseException if a database error occurs
	 */
	private String getSplitUserTable(String table, String user)
			throws DatabaseException {
		Object lock = DatabaseLockCollection.getLock(name, table);
		synchronized (lock) {
			DatabaseCache cache = DatabaseCache.getInstance();
			UserTableKey userTableKey = cache.getUserTableKey(this, user, table);
			String physTable = table + TABLE_TOKEN_SEP + userTableKey.getKey();
			if (cache.getPhysicalTables(this).contains(physTable))
				return physTable;
			createTable(physTable, cache.getTableDataClass(this, table),
					cache.getTableCompoundIndexes(this, table));
			return physTable;
		}
	}
	
	/**
	 * This method takes the definition of a table that is split by user. It
	 * returns the name of the physical table for the specified user. If the
	 * physical table doesn't exist, it will be created.
	 * 
	 * @param tableDef the definition of the table that is split by user
	 * @param user the user
	 * @return the physical table name
	 * @throws DatabaseException if a database error occurs
	 */
	public String getSplitUserTable(DatabaseTableDef<?> tableDef, String user)
			throws DatabaseException {
		DatabaseCache cache = DatabaseCache.getInstance();
		UserTableKey userTableKey = cache.getUserTableKey(this, user,
				tableDef.getName());
		String physTable = tableDef.getName() + TABLE_TOKEN_SEP +
				userTableKey.getKey();
		if (cache.getPhysicalTables(this).contains(physTable))
			return physTable;
		createTable(physTable, tableDef.getDataClass(),
				tableDef.getCompoundIndexes());
		return physTable;
	}
	
	/**
	 * Returns the value of the "user" field from the specified map that is to
	 * be inserted into the specified table. If the map doesn't have a "user"
	 * field or the field is empty, then this method throws a
	 * DatabaseException.
	 * 
	 * @param table the (logical) table name
	 * @param value the value to insert into the table
	 * @return the user
	 * @throws DatabaseException if the map doesn't defined a user
	 */
	private String getInsertUser(String table, Map<String,?> value)
			throws DatabaseException {
		Object userObj = value.get("user");
		if (userObj == null) {
			throw new DatabaseException(String.format(
					"Can't insert database object without field \"user\" into table \"%s\" that is split by user",
					table));
		}
		String user = userObj.toString();
		if (user.length() == 0) {
			throw new DatabaseException(String.format(
					"Can't insert database object with empty field \"user\" into table \"%s\" that is split by user",
					table));
		}
		return user;
	}

	/**
	 * Selects objects from a database table. This method returns a list of
	 * objects of the table's data class. It will write the "id" field and
	 * fields that are annotated with {@link DatabaseField DatabaseField}. The
	 * "id" field should not be annotated.
	 * 
	 * <p>This method should only be called if the database is initialized.</p>
	 * 
	 * <p>If the table is split by user, then the criteria must contain a
	 * {@link DatabaseCriteria.Equal DatabaseCriteria.Equal} on field "user".
	 * This method will select from the physical table for that user.</p>
	 *
	 * @param table the table (lower case name)
	 * @param criteria the criteria for the objects to return. This can be
	 * null.
	 * @param limit the maximum number of objects to return. Set this to 0 or
	 * less to get all records.
	 * @param sort the order in which the objects are returned. This can be
	 * null or an empty array if no sorting is needed.
	 * @param <T> the type of database object
	 * @return the objects
	 * @throws DatabaseException if a database error occurs
	 * @see DatabaseObjectMapper
	 */
	public <T extends DatabaseObject> List<T> select(DatabaseTableDef<T> table,
			DatabaseCriteria criteria, int limit, DatabaseSort[] sort)
			throws DatabaseException {
		List<Map<String,?>> maps = selectMaps(table.getName(),
				table.getDataClass(), criteria, limit, sort);
		List<T> result = new ArrayList<>();
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		for (Map<String,?> map : maps) {
			result.add(mapper.mapToObject(map, table.getDataClass(), false));
		}
		return result;
	}

	/**
	 * Selects objects from a database table. This method returns a list of
	 * objects of the specified data class. It will write the "id" field and
	 * fields that are annotated with {@link DatabaseField DatabaseField}. The
	 * "id" field should not be annotated.
	 *
	 * <p>This method should only be called if the database is initialized.</p>
	 *
	 * <p>If the table is split by user, then the criteria must contain a
	 * {@link DatabaseCriteria.Equal DatabaseCriteria.Equal} on field "user".
	 * This method will select from the physical table for that user.</p>
	 *
	 * @param table the table name (lower case)
	 * @param dataClass the data class
	 * @param criteria the criteria for the objects to return. This can be
	 * null.
	 * @param limit the maximum number of objects to return. Set this to 0 or
	 * less to get all records.
	 * @param sort the order in which the objects are returned. This can be
	 * null or an empty array if no sorting is needed.
	 * @param <T> the type of database object
	 * @return the objects
	 * @throws DatabaseException if a database error occurs
	 * @see DatabaseObjectMapper
	 */
	public <T extends DatabaseObject> List<T> select(String table,
			Class<T> dataClass, DatabaseCriteria criteria, int limit,
			DatabaseSort[] sort) throws DatabaseException {
		List<Map<String,?>> maps = selectMaps(table, dataClass, criteria, limit,
				sort);
		List<T> result = new ArrayList<>();
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		for (Map<String,?> map : maps) {
			result.add(mapper.mapToObject(map, dataClass, false));
		}
		return result;
	}

	/**
	 * Selects one object from a database table. If more than one object
	 * matches, it returns the first object according to the specified sort
	 * order. If no object matches, it returns null. This method returns an
	 * object of the table's data class. It will write the "id" field and
	 * fields that are annotated with {@link DatabaseField DatabaseField}. The
	 * "id" field should not be annotated.
	 * 
	 * <p>This method should only be called if the database is initialized.</p>
	 * 
	 * <p>If the table is split by user, then the criteria must contain a
	 * {@link DatabaseCriteria.Equal DatabaseCriteria.Equal} on field "user".
	 * This method will select from the physical table for that user.</p>
	 *
	 * @param table the table (lower case)
	 * @param criteria the criteria for the object to return. This can be
	 * null.
	 * @param sort the order in which the objects are returned. This can be
	 * null or an empty array if no sorting is needed.
	 * @param <T> the type of database object
	 * @return the object or null
	 * @throws DatabaseException if a database error occurs
	 * @see DatabaseObjectMapper
	 */
	public <T extends DatabaseObject> T selectOne(DatabaseTableDef<T> table,
			DatabaseCriteria criteria, DatabaseSort[] sort)
			throws DatabaseException {
		List<T> list = select(table, criteria, 1, sort);
		if (list.isEmpty())
			return null;
		else
			return list.get(0);
	}

	/**
	 * Selects records from a database table. This method returns a list of
	 * data maps. Each map should at least have a key "id". The keys are the
	 * column names. In some databases the column names are in lower case, so
	 * they may not exactly match the field names of a {@link DatabaseObject
	 * DatabaseObject}.
	 *
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table, and the "dataClass"
	 * should be set for efficiency. Otherwise, for example during a table
	 * upgrade, it should be a physical table and "dataClass" should be
	 * null.</p>
	 *
	 * <p>If the table is a logical table split by user, then the criteria must
	 * contain a {@link DatabaseCriteria.Equal DatabaseCriteria.Equal} on field
	 * "user". This method will select from the physical table for that
	 * user.</p>
	 *
	 * <p>Users should normally not call this method but use one of the
	 * select() methods, but this method needs to be used if the database is
	 * not initialized, typically during a database upgrade.</p>
	 *
	 * @param table the table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria for the records to return. This can be
	 * null.
	 * @param limit the maximum number of records to return. Set this to 0 or
	 * less to get all records.
	 * @param sort the order in which the records are returned. This can be
	 * null or an empty array if no sorting is needed.
	 * @return the records (keys may be in lower case)
	 * @throws DatabaseException if a database error occurs
	 */
	public List<Map<String,?>> selectMaps(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria, int limit, DatabaseSort[] sort)
			throws DatabaseException {
		String physTable = table;
		DatabaseCriteria physCriteria = criteria;
		if (useSplitUserTable(table)) {
			String selectUser = getSelectUser(table, criteria);
			physTable = getSplitUserTable(table, selectUser);
			physCriteria = removeUserCriteria(criteria, selectUser);
		}
		return doSelectMaps(physTable, dataClass, physCriteria, limit, sort);
	}

	/**
	 * Selects records from a physical database table. This method returns a
	 * list of data maps. Each map should at least have a key "id". The keys
	 * are the column names. In some databases the column names are in lower
	 * case, so they may not exactly match the field names of a {@link
	 * DatabaseObject DatabaseObject}.
	 *
	 * <p>If the database is initialized, the "dataClass" should be set for
	 * efficiency. Otherwise, for example during a table upgrade, it should be a
	 * physical table and "dataClass" should be null.</p>
	 *
	 * @param table the (physical) table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria for the records to return. This can be
	 * null.
	 * @param limit the maximum number of records to return. Set this to 0 or
	 * less to get all records.
	 * @param sort the order in which the records are returned. This can be
	 * null or an empty array if no sorting is needed.
	 * @return the records (keys may be in lower case)
	 * @throws DatabaseException if a database error occurs
	 */
	protected abstract List<Map<String,?>> doSelectMaps(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria, int limit, DatabaseSort[] sort)
			throws DatabaseException;
	
	/**
	 * Returns the user that is selected by the specified database criteria.
	 * If the criteria don't select one user, then this method throws a
	 * DatabaseException.
	 * 
	 * @param table the (logical) table name
	 * @param criteria the database criteria
	 * @return the user
	 * @throws DatabaseException if the criteria don't select one user
	 */
	private String getSelectUser(String table, DatabaseCriteria criteria)
			throws DatabaseException {
		String user = findSelectUser(criteria);
		if (user == null) {
			throw new DatabaseException("Select from table \"" + table +
				"\", which is split by user, requires criteria that select a user (field \"user\"); criteria: " +
				criteria);
		}
		return user;
	}
	
	/**
	 * Tries to find a DatabaseCriteria.Equal on field "user" in the specified
	 * criteria. If an instance is found and the value is not null or empty,
	 * then this method returns the user value. This is used to resolve a
	 * physical table from a logical table that is split by user.
	 * 
	 * @param criteria the database criteria (can be null)
	 * @return the user or null
	 */
	private String findSelectUser(DatabaseCriteria criteria) {
		if (criteria == null)
			return null;
		if (criteria instanceof DatabaseCriteria.And) {
			DatabaseCriteria.And and = (DatabaseCriteria.And)criteria;
			for (DatabaseCriteria op : and.getOperands()) {
				String user = findSelectUser(op);
				if (user != null)
					return user;
			}
			return null;
		} else if (criteria instanceof DatabaseCriteria.Or) {
			DatabaseCriteria.Or or = (DatabaseCriteria.Or)criteria;
			for (DatabaseCriteria op : or.getOperands()) {
				String user = findSelectUser(op);
				if (user != null)
					return user;
			}
			return null;
		} else if (criteria instanceof DatabaseCriteria.Equal) {
			DatabaseCriteria.Equal equal = (DatabaseCriteria.Equal)criteria;
			if (!equal.getColumn().equals("user") || equal.getValue() == null)
				return null;
			String user = equal.getValue().toString();
			if (user.isEmpty())
				return null;
			return user;
		} else {
			return null;
		}
	}
	
	/**
	 * Removes any instances of {@link DatabaseCriteria.Equal
	 * DatabaseCriteria.Equal} on field "user" with the specified user value
	 * and returns the result.
	 * 
	 * @param criteria the criteria
	 * @param user the user value
	 * @return the new criteria
	 */
	private DatabaseCriteria removeUserCriteria(DatabaseCriteria criteria,
			String user) {
		if (criteria == null)
			return null;
		if (criteria instanceof DatabaseCriteria.And) {
			DatabaseCriteria.And and = (DatabaseCriteria.And)criteria;
			List<DatabaseCriteria> newOps = new ArrayList<>();
			for (DatabaseCriteria op : and.getOperands()) {
				DatabaseCriteria newOp = removeUserCriteria(op, user);
				if (newOp != null)
					newOps.add(newOp);
			}
			if (newOps.isEmpty())
				return null;
			if (newOps.size() == 1)
				return newOps.get(0);
			return new DatabaseCriteria.And(newOps.toArray(
					new DatabaseCriteria[0]));
		} else if (criteria instanceof DatabaseCriteria.Or) {
			DatabaseCriteria.Or or = (DatabaseCriteria.Or)criteria;
			List<DatabaseCriteria> newOps = new ArrayList<>();
			for (DatabaseCriteria op : or.getOperands()) {
				DatabaseCriteria newOp = removeUserCriteria(op, user);
				if (newOp == null)
					return null;
				newOps.add(newOp);
			}
			if (newOps.isEmpty())
				return null;
			if (newOps.size() == 1)
				return newOps.get(0);
			return new DatabaseCriteria.Or(newOps.toArray(
					new DatabaseCriteria[0]));
		} else if (criteria instanceof DatabaseCriteria.Equal) {
			DatabaseCriteria.Equal equal = (DatabaseCriteria.Equal)criteria;
			if (equal.getColumn().equals("user") &&
					user.equals(equal.getValue())) {
				return null;
			}
			return equal;
		} else {
			return criteria;
		}
	}

	/**
	 * Counts the number of records in a table that match the specified
	 * criteria.
	 *
	 * <p>If the table is split by user, then the criteria must contain a {@link
	 * DatabaseCriteria.Equal DatabaseCriteria.Equal} on field "user". This
	 * method will select from the physical table for that user.</p>
	 *
	 * @param table the table name (lower case name)
	 * @param criteria the criteria. This can be null.
	 * @return the number of records
	 * @throws DatabaseException if a database error occurs
	 */
	public int count(DatabaseTableDef<?> table, DatabaseCriteria criteria)
			throws DatabaseException {
		return count(table.getName(), table.getDataClass(), criteria);
	}

	/**
	 * Counts the number of records in a table that match the specified
	 * criteria.
	 *
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table, and the "dataClass"
	 * should be set for efficiency. Otherwise, for example during a table
	 * upgrade, it should be a physical table and "dataClass" should be
	 * null.</p>
	 * 
	 * <p>If the table is a logical table split by user, then the criteria must
	 * contain a {@link DatabaseCriteria.Equal DatabaseCriteria.Equal} on field
	 * "user". This method will select from the physical table for that
	 * user.</p>
	 * 
	 * @param table the table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria. This can be null.
	 * @return the number of records
	 * @throws DatabaseException if a database error occurs
	 */
	public int count(String table, Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria) throws DatabaseException {
		String physTable = table;
		DatabaseCriteria physCriteria = criteria;
		if (useSplitUserTable(table)) {
			String selectUser = getSelectUser(table, criteria);
			physTable = getSplitUserTable(table, selectUser);
			physCriteria = removeUserCriteria(criteria, selectUser);
		}
		return doCount(physTable, dataClass, physCriteria);
	}
	
	/**
	 * Counts the number of records in a physical table that match the
	 * specified criteria.
	 *
	 * <p>If the database is initialized, the "dataClass" should be set for
	 * efficiency. Otherwise, for example during a table upgrade, it should be a
	 * physical table and "dataClass" should be null.</p>
	 *
	 * @param table the (physical) table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria. This can be null.
	 * @return the number of records
	 * @throws DatabaseException if a database error occurs
	 */
	protected abstract int doCount(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria) throws DatabaseException;

	/**
	 * Updates the specified object in the database. The object should already
	 * be in the specified table.
	 *
	 * <p>This should only be called if the database is initialized. The
	 * specified table should be a logical table or a physical database action
	 * table.</p>
	 *
	 * <p>If the table is a logical table split by user, then the object must
	 * define a field "user". This method will update the object in the
	 * physical table for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the update action to the table
	 * _action_log.</p>
	 *
	 * @param table the table name (lower case)
	 * @param object the object
	 * @throws DatabaseException if a database error occurs
	 */
	public void update(String table, DatabaseObject object)
			throws DatabaseException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal("id",
				object.getId());
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		Map<String,Object> map = mapper.objectToMap(object, false);
		map.remove("id");
		update(table, object.getClass(), criteria, map);
	}

	/**
	 * Updates all records that match the specified criteria. The keys in the
	 * map "values" are the columns that should be set. The values should be
	 * obtained with {@link DatabaseObjectMapper DatabaseObjectMapper}.
	 *
	 * <p>If the table is a logical table split by user, then either the
	 * criteria must contain a {@link DatabaseCriteria.Equal
	 * DatabaseCriteria.Equal} on field "user", or the map must define a field
	 * "user". If both the criteria and the map specify a user, then they must
	 * be the same. This method will update the object in the physical table
	 * for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the update action to the table _action_log
	 * with source "local". See also {@link
	 * #update(String, Class, DatabaseCriteria, Map, String)
	 * update(table, dataClass, criteria, values, source)}.</p>
	 *
	 * @param table the table name (lower case name)
	 * @param criteria the criteria for the records to update. This can be
	 * null.
	 * @param values the column values that should be set
	 * @throws DatabaseException if a database error occurs
	 */
	public void update(DatabaseTableDef<?> table, DatabaseCriteria criteria,
			Map<String,?> values) throws DatabaseException {
		update(table.getName(), table.getDataClass(), criteria, values);
	}

	/**
	 * Updates all records that match the specified criteria. The keys in the
	 * map "values" are the columns that should be set. The values should be
	 * obtained with {@link DatabaseObjectMapper DatabaseObjectMapper} and they
	 * should not include the "id" field.
	 *
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table, and the "dataClass"
	 * should be set for efficiency. Otherwise, for example during a table
	 * upgrade, it should be a physical table and "dataClass" should be
	 * null.</p>
	 * 
	 * <p>If the table is a logical table split by user, then either the
	 * criteria must contain a {@link DatabaseCriteria.Equal
	 * DatabaseCriteria.Equal} on field "user", or the map must define a field
	 * "user". If both the criteria and the map specify a user, then they must
	 * be the same. This method will update the object in the physical table
	 * for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the update action to the table _action_log
	 * with source "local". See also {@link
	 * #update(String, Class, DatabaseCriteria, Map, String)
	 * update(table, dataClass, criteria, values, source)}.</p>
	 *
	 * @param table the table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria for the records to update. This can be
	 * null.
	 * @param values the column values that should be set
	 * @throws DatabaseException if a database error occurs
	 */
	public void update(String table, Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria, Map<String,?> values)
			throws DatabaseException {
		update(table, dataClass, criteria, values, DatabaseAction.SOURCE_LOCAL);
	}

	/**
	 * Updates all records that match the specified criteria. The keys in the
	 * map "values" are the columns that should be set. The values should be
	 * obtained with {@link DatabaseObjectMapper DatabaseObjectMapper} and they
	 * should not include the "id" field.
	 *
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table, and the "dataClass"
	 * should be set for efficiency. Otherwise, for example during a table
	 * upgrade, it should be a physical table and "dataClass" should be
	 * null.</p>
	 * 
	 * <p>If the table is a logical table split by user, then either the
	 * criteria must contain a {@link DatabaseCriteria.Equal
	 * DatabaseCriteria.Equal} on field "user", or the map must define a field
	 * "user". If both the criteria and the map specify a user, then they must
	 * be the same. This method will update the object in the physical table
	 * for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the update action to the table _action_log
	 * with the specified source.</p>
	 *
	 * <p>You should normally call {@link (String, Class, DatabaseCriteria, Map)
	 * update(table, dataClass, criteria, values)}. This method with source
	 * parameter is used by synchronisers.</p>
	 *
	 * @param table the table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria for the records to update. This can be
	 * null.
	 * @param values the column values that should be set
	 * @param source the source of the action. See {@link
	 * DatabaseAction#setSource(String) DatabaseAction.setSource()}.
	 * @throws DatabaseException if a database error occurs
	 */
	public void update(String table, Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria, Map<String,?> values, String source)
			throws DatabaseException {
		if (values.containsKey("id")) {
			throw new DatabaseException(
					"Field \"id\" cannot be changed at update");
		}
		String physTable = table;
		DatabaseCriteria physCriteria = criteria;
		if (useSplitUserTable(table)) {
			String updateUser = getUpdateUser(table, criteria, values);
			physTable = getSplitUserTable(table, updateUser);
			physCriteria = removeUserCriteria(criteria, updateUser);
		}
		doUpdate(physTable, dataClass, physCriteria, values);
		boolean syncLog = syncEnabled && (
				source.equals(DatabaseAction.SOURCE_LOCAL) ||
				saveSyncedRemoteActions);
		if (!table.startsWith("_") && syncLog) {
			List<? extends Map<String,?>> records = selectLogRecords(physTable,
					dataClass, physCriteria);
			List<Map<String,?>> valueList = new ArrayList<>();
			for (int i = 0; i < records.size(); i++) {
				valueList.add(values);
			}
			writeDatabaseActions(table, DatabaseAction.Action.UPDATE, records,
					valueList, source);
		}
		if (!table.startsWith("_")) {
			DatabaseListenerRepository.getInstance().notifyDatabaseEvent(
					new DatabaseEvent.Update(name, table, criteria, values));
		}
	}

	/**
	 * Updates all records that match the specified criteria in a physical
	 * database table. The keys in the map "values" are the columns that should
	 * be set. The values should be obtained with {@link DatabaseObjectMapper
	 * DatabaseObjectMapper}.
	 *
	 * <p>If the database is initialized, the "dataClass" should be set for
	 * efficiency. Otherwise, for example during a table upgrade, it should be a
	 * physical table and "dataClass" should be null.</p>
	 *
	 * @param table the (physical) table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria for the records to update. This can be
	 * null.
	 * @param values the column values that should be set
	 * @throws DatabaseException if a database error occurs
	 */
	protected abstract void doUpdate(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria, Map<String,?> values)
			throws DatabaseException;
	
	/**
	 * Returns the user on which an update query should be run in a table that
	 * is split by user. It tries to get the user from the database criteria
	 * and from the value map. If a user is specified more than once, they
	 * should be the same. If no user is specified or the user is empty, then
	 * this method throws a DatabaseException.
	 * 
	 * @param table the (logical) table name
	 * @param criteria the database criteria
	 * @param values the column values that should be set
	 * @return the user
	 * @throws DatabaseException if the criteria and values don't specify one
	 * user
	 */
	private String getUpdateUser(String table, DatabaseCriteria criteria,
			Map<String,?> values) throws DatabaseException {
		String criteriaUser = findSelectUser(criteria);
		String valuesUser = null;
		Object userObj = values.get("user");
		if (userObj != null) {
			valuesUser = userObj.toString();
		}
		if (valuesUser != null && valuesUser.isEmpty()) {
			throw new DatabaseException(String.format(
					"User in update values is empty at update in table \"%s\" that is split by user",
					table));
		}
		if (criteriaUser != null && valuesUser != null) {
			if (!criteriaUser.equals(valuesUser)) {
				throw new DatabaseException(String.format(
						"User in select criteria (%s) does not match user in update values (%s) at update in table \"%s\" that is split by user" +
						"; criteria: %s",
						criteriaUser, valuesUser, table, criteria));
			}
			return valuesUser;
		}
		if (criteriaUser != null)
			return criteriaUser;
		if (valuesUser != null)
			return valuesUser;
		throw new DatabaseException(String.format(
				"Update in table \"%s\" that is split by user requires selection of a user in select criteria, or definition of field \"user\" in the update values" +
				"; criteria: %s",
				table, criteria));
	}

	/**
	 * Deletes the specified object in the database.
	 *
	 * <p>This should only be called if the database is initialized. The
	 * specified table should be a logical table or a physical database action
	 * table.</p>
	 *
	 * <p>If the table is a logical table split by user, then the object must
	 * define a field "user". The object will be deleted from the physical
	 * table for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the delete action to the table
	 * _action_log.</p>
	 *
	 * @param table the table name (lower case)
	 * @param object the object
	 * @throws DatabaseException if a database error occurs
	 */
	public void delete(String table, DatabaseObject object)
			throws DatabaseException {
		DatabaseCriteria criteria;
		if (useSplitUserTable(table)) {
			String user = (String)PropertyReader.readProperty(object, "user");
			criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("id", object.getId()),
				new DatabaseCriteria.Equal("user", user)
			);
		} else {
			criteria = new DatabaseCriteria.Equal("id", object.getId());
		}
		delete(table, object.getClass(), criteria);
	}
	
	/**
	 * Purges all data for a user from the specified table. This method also
	 * deletes related data from the action table and sync progress table.
	 * Existing tables will not be dropped, only cleared.
	 * 
	 * @param table the table name (lower case)
	 * @param user the user
	 * @throws DatabaseException if a database error occurs
	 */
	public void purgeUserTable(String table, String user)
			throws DatabaseException {
		purgeUserTable(table, user, false);
	}
	
	/**
	 * Purges a user from the database. This method will delete all data for the
	 * user from all data tables, action tables, the sync progress table and the
	 * user-table keys. Any user-specific tables (action tables and data tables
	 * that are split by user) will be dropped. References to the user and
	 * dropped tables will be removed from the cache.
	 * 
	 * @param user the user
	 * @throws DatabaseException if a database error occurs
	 */
	public void purgeUser(String user) throws DatabaseException {
		List<String> tables = selectTables();
		for (String table : tables) {
			if (!table.startsWith("_")) {
				purgeUserTable(table, user, true);
			}
		}
	}

	/**
	 * Purges all data for a user from the specified table. This method also
	 * deletes related data from the action table and sync progress table.
	 * 
	 * <p>If purgeUser is true, this method will additionally delete the
	 * user-table key. The action table will be dropped. If the data table is
	 * split by user, it will also be dropped, including any subtables.
	 * References to the user and table, and references to dropped tables,
	 * will be removed from the cache.</p>
	 * 
	 * <p>If purgeUser is false, existing tables will not be dropped, only
	 * cleared.</p>
	 * 
	 * @param table the table name (lower case)
	 * @param user the user
	 * @throws DatabaseException if a database error occurs
	 */
	private void purgeUserTable(String table, String user, boolean purgeUser)
			throws DatabaseException {
		DatabaseCache cache = DatabaseCache.getInstance();
		List<String> physTables = cache.getPhysicalTables(this);
		DatabaseCriteria userCriteria = new DatabaseCriteria.Equal(
				"user", user);
		DatabaseCriteria tableUserCriteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("user", user),
				new DatabaseCriteria.Equal("table", table)
		);
		UserTableKey key = selectOne(new UserTableKeyTable(), tableUserCriteria,
				null);
		String physTable = null;
		if (key != null)
			physTable = table + Database.TABLE_TOKEN_SEP + key.getKey();
		boolean isSplitByUser = cache.isTableSplitByUser(this, table);
		if (isSplitByUser && physTable != null && purgeUser) {
			for (String existingTable : physTables) {
				if (existingTable.startsWith(physTable))
					dropCachedDbTable(existingTable);
			}
		} else if (isSplitByUser && physTable != null &&
				physTables.contains(physTable)) {
			delete(table, null, userCriteria, DatabaseAction.SOURCE_LOCAL,
					true);
		} else if (!isSplitByUser) {
			delete(table, null, userCriteria, DatabaseAction.SOURCE_LOCAL,
					true);
		}
		if (key != null) {
			String actionTable = DatabaseActionTable.NAME_PREFIX +
					key.getKey();
			if (physTables.contains(actionTable)) {
				if (purgeUser)
					dropCachedDbTable(actionTable);
				else
					delete(actionTable, null, userCriteria);
			}
		}
		delete(SyncProgressTableDef.NAME, null, tableUserCriteria);
		if (purgeUser) {
			cache.removeUserTable(this, user, table);
			delete(UserTableKeyTable.NAME, null, tableUserCriteria);
		}
	}

	/**
	 * Deletes all records that match the specified criteria.
	 *
	 * <p>If the table is split by user, then the criteria must contain a
	 * {@link DatabaseCriteria.Equal DatabaseCriteria.Equal} on field "user".
	 * This method will delete from the physical table for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the delete action to the table _action_log
	 * with source "local". See also {@link
	 * #delete(String, Class, DatabaseCriteria, String)
	 * delete(table, dataClass, criteria, source)}.</p>
	 *
	 * @param table the table name (lower case name)
	 * @param criteria the criteria. This can be null.
	 * @throws DatabaseException if a database error occurs
	 */
	public void delete(DatabaseTableDef<?> table, DatabaseCriteria criteria)
			throws DatabaseException {
		delete(table.getName(), table.getDataClass(), criteria);
	}

	/**
	 * Deletes all records that match the specified criteria.
	 *
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table, and the "dataClass"
	 * should be set for efficiency. Otherwise, for example during a table
	 * upgrade, it should be a physical table and "dataClass" should be
	 * null.</p>
	 * 
	 * <p>If the table is split by user, then the criteria must contain a
	 * {@link DatabaseCriteria.Equal DatabaseCriteria.Equal} on field "user".
	 * This method will delete from the physical table for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the delete action to the table _action_log
	 * with source "local". See also {@link
	 * #delete(String, Class, DatabaseCriteria, String)
	 * delete(table, dataClass, criteria, source)}.</p>
	 *
	 * @param table the table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria. This can be null.
	 * @throws DatabaseException if a database error occurs
	 */
	public void delete(String table, Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria) throws DatabaseException {
		delete(table, dataClass, criteria, DatabaseAction.SOURCE_LOCAL);
	}

	/**
	 * Deletes all records that match the specified criteria.
	 *
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table, and the "dataClass"
	 * should be set for efficiency. Otherwise, for example during a table
	 * upgrade, it should be a physical table and "dataClass" should be
	 * null.</p>
	 * 
	 * <p>If the table is split by user, then the criteria must contain a
	 * {@link DatabaseCriteria.Equal DatabaseCriteria.Equal} on field "user".
	 * This method will delete from the physical table for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the delete action to the table _action_log
	 * with the specified source.</p>
	 *
	 * <p>You should normally call {@link
	 * #delete(String, Class, DatabaseCriteria)
	 * delete(table, dataClass, criteria)}. This method with source parameter is
	 * used by synchronisers.</p>
	 *
	 * @param table the table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria. This can be null.
	 * @param source the source of the action. See {@link
	 * DatabaseAction#setSource(String) DatabaseAction.setSource()}.
	 * @throws DatabaseException if a database error occurs
	 */
	public void delete(String table, Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria, String source)
			throws DatabaseException {
		delete(table, dataClass, criteria, source, false);
	}

	/**
	 * Deletes all records that match the specified criteria.
	 *
	 * <p>If the database is initialized, the specified table should be a
	 * logical table or a physical database action table, and the "dataClass"
	 * should be set for efficiency. Otherwise, for example during a table
	 * upgrade, it should be a physical table and "dataClass" should be
	 * null.</p>
	 * 
	 * <p>If the table is split by user, then the criteria must contain a
	 * {@link DatabaseCriteria.Equal DatabaseCriteria.Equal} on field "user".
	 * This method will delete from the physical table for that user.</p>
	 *
	 * <p>If sync logging is enabled and the table is not one of the reserved
	 * tables, this method will add the delete action to the table _action_log
	 * with the specified source.</p>
	 * 
	 * <p>This method allows to disable sync logging, which should only be
	 * used on a purge action.</p>
	 *
	 * @param table the table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria. This can be null.
	 * @param source the source of the action. See {@link
	 * DatabaseAction#setSource(String) DatabaseAction.setSource()}.
	 * @throws DatabaseException if a database error occurs
	 */
	private void delete(String table, Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria, String source, boolean disableSyncLog)
			throws DatabaseException {
		String physTable = table;
		DatabaseCriteria physCriteria = criteria;
		if (useSplitUserTable(table)) {
			String deleteUser = getSelectUser(table, criteria);
			physTable = getSplitUserTable(table, deleteUser);
			physCriteria = removeUserCriteria(criteria, deleteUser);
		}
		List<? extends Map<String,?>> records = new ArrayList<>();
		boolean syncLog = !disableSyncLog && syncEnabled && (
				source.equals(DatabaseAction.SOURCE_LOCAL) ||
				saveSyncedRemoteActions);
		if (!table.startsWith("_") && syncLog) {
			records = selectLogRecords(physTable, dataClass, physCriteria);
		}
		doDelete(physTable, dataClass, physCriteria);
		if (!records.isEmpty() && !table.startsWith("_") && syncLog) {
			writeDatabaseActions(table, DatabaseAction.Action.DELETE, records,
					null, source);
		}
		if (!table.startsWith("_")) {
			DatabaseListenerRepository.getInstance().notifyDatabaseEvent(
					new DatabaseEvent.Delete(name, table, criteria));
		}
	}

	/**
	 * Deletes all records that match the specified criteria from a physical
	 * database table.
	 *
	 * <p>If the database is initialized, the "dataClass" should be set for
	 * efficiency. Otherwise, for example during a table upgrade, it should be a
	 * physical table and "dataClass" should be null.</p>
	 *
	 * @param table the (physical) table name (lower case)
	 * @param dataClass the data class or null. Specifying the data class can
	 * make the query more efficient, but it should only be specified if the
	 * database is initialized.
	 * @param criteria the criteria. This can be null.
	 * @throws DatabaseException if a database error occurs
	 */
	protected abstract void doDelete(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria) throws DatabaseException;
	
	/**
	 * Writes database actions to the {@link DatabaseActionTable
	 * DatabaseActionTable}s. The lists "records" and "values" must
	 * have the same length.
	 * 
	 * <p>A record should have the following keys:</p>
	 *
	 * <p><ul>
	 * <li>id (required): the record ID</li>
	 * <li>user (optional): if the record has a "user" field, its value should
	 * be set here. Otherwise it can be omitted or set to null.</li>
	 * </ul></p>
	 *
	 * <p>A value map should contain the data that is associated with the
	 * action. It will be written as a JSON object. For an insert, it contains
	 * the complete record including "id". For an update it contains the
	 * updated columns. For a select or delete, a value map is always null and
	 * you can set the entire "values" list to null.</p>
	 * 
	 * @param table the (logical) table where the action was written
	 * @param action the action
	 * @param records the affected records
	 * @param values the action data or null
	 * @param source the source
	 * @throws DatabaseException if a database error occurs
	 */
	private void writeDatabaseActions(String table,
			DatabaseAction.Action action,
			List<? extends Map<String,?>> records,
			List<? extends Map<String,?>> values, String source)
			throws DatabaseException {
		Object tableLock = DatabaseLockCollection.getLock(name, table);
		synchronized (tableLock) {
			List<DatabaseAction> actions = new ArrayList<>();
			String currUser = null;
			DatabaseActionTable actionTable = null;
			Iterator<? extends Map<String,?>> recordIt = records.iterator();
			Iterator<? extends Map<String,?>> valuesIt = null;
			if (values != null)
				valuesIt = values.iterator();
			while (recordIt.hasNext()) {
				Map<String,?> record = recordIt.next();
				Map<String,?> data = null;
				if (valuesIt != null)
					data = valuesIt.next();
				String recUser = (String)record.get("user");
				if (actionTable == null ||
						!isEqualNullString(recUser, currUser)) {
					if (actionTable != null && actions.size() > 0)
						insertActions(actionTable.getName(), actions);
					actions.clear();
					currUser = recUser;
					actionTable = DatabaseCache.getInstance()
							.initActionTable(this, currUser, table);
				}
				addDatabaseAction(actionTable, actions, action, record, data,
						source);
			}
			if (actionTable != null && actions.size() > 0) {
				insertActions(actionTable.getName(), actions);
				DatabaseListenerRepository.getInstance()
						.notifyAddDatabaseActions(name, table, actions);
			}
		}
	}

	private boolean isEqualNullString(String s1, String s2) {
		if ((s1 == null) != (s2 == null))
			return false;
		if (s1 != null && !s1.equals(s2))
			return false;
		return true;
	}

	/**
	 * Inserts a list of database actions of the same type. If they are update
	 * or delete actions, it will merge each action with any previous actions on
	 * the same record.
	 *
	 * @param table the action table
	 * @param actions the database actions
	 * @throws DatabaseException if a database error occurs
	 */
	private void insertActions(String table, List<DatabaseAction> actions)
			throws DatabaseException {
		insert(table, actions);
		DatabaseAction.Action action = actions.get(0).getAction();
		if (action != DatabaseAction.Action.INSERT) {
			List<DatabaseAction> mergeActions = new ArrayList<>(actions);
			mergeActions(table, mergeActions);
		}
	}

	private void mergeActions(String table, List<DatabaseAction> actions)
			throws DatabaseException {
		Logger logger = AppComponents.getLogger(Database.class.getSimpleName());
		for (DatabaseAction action : actions) {
			DatabaseCriteria criteria = new DatabaseCriteria.Equal("recordId",
					action.getRecordId());
			DatabaseSort[] sort = new DatabaseSort[] {
					new DatabaseSort("time", false),
					new DatabaseSort("order", false)
			};
			logger.debug("Start merge record actions: " + table + ", " +
					action.getAction());
			long start = System.currentTimeMillis();
			List<DatabaseAction> recActions = select(table,
					DatabaseAction.class, criteria, 0, sort);
			if (recActions.size() < 2)
				continue;
			logger.debug("Merge record actions count: " + recActions.size());
			DatabaseActionMerger merger = new DatabaseActionMerger();
			try {
				merger.mergeRecordActions(this, table, recActions);
				long end = System.currentTimeMillis();
				logger.debug("End merge record actions: " +
						(end - start) + " ms");
			} catch (MergeException ex) {
				throw new DatabaseException(
						"Can't merge database actions: " + ex.getMessage(),
						ex);
			}
		}
	}

	/**
	 * Creates a database action for logging and adds it to the specified list
	 * "actions". The list should contain actions that occurred earlier than
	 * this action and that have not been written to the database yet. The
	 * parameter "record" should have the following keys:
	 *
	 * <p><ul>
	 * <li>id (required): the record ID</li>
	 * <li>user (optional): if the record has a "user" field, its value should
	 * be set here. Otherwise it can be omitted or set to null.</li>
	 * </ul></p>
	 *
	 * <p>The parameter "data" should contain the data that is associated with
	 * the action. It will be written as a JSON object. For an insert, it
	 * contains the complete record including "id". For an update it contains
	 * the updated columns. For a select or delete, the data is null.</p>
	 *
	 * @param actionTable the database action table to which the action should
	 * eventually be written. This method does not write to the table.
	 * @param actions the list to which the action should be added
	 * @param action the action
	 * @param record the record
	 * @param data the data that is associated with the action
	 * @throws DatabaseException if a database error occurs
	 */
	private void addDatabaseAction(DatabaseActionTable actionTable,
			List<DatabaseAction> actions, DatabaseAction.Action action,
			Map<String,?> record, Map<String,?> data, String source)
			throws DatabaseException {
		long now = System.currentTimeMillis();
		String table = actionTable.getUserTableKey().getTable();
		DatabaseAction prevAction = null;
		for (int i = actions.size() - 1; i >= 0; i--) {
			DatabaseAction current = actions.get(i);
			if (current.getTable().equals(table)) {
				prevAction = current;
				break;
			}
		}
		if (prevAction == null) {
			DatabaseCriteria criteria = new DatabaseCriteria.Equal(
					"table", table);
			DatabaseSort[] sort = new DatabaseSort[] {
					new DatabaseSort("time", false),
					new DatabaseSort("order", false)
			};
			List<Map<String,?>> maps = selectMaps(actionTable.getName(),
					DatabaseAction.class, criteria, 1, sort);
			if (!maps.isEmpty()) {
				DatabaseObjectMapper mapper = new DatabaseObjectMapper();
				prevAction = mapper.mapToObject(maps.get(0),
						DatabaseAction.class, false);
			}
		}
		int order = 0;
		if (prevAction != null && prevAction.getTime() >= now) {
			now = prevAction.getTime();
			order = prevAction.getOrder() + 1;
		}
		DatabaseAction dbAction = new DatabaseAction();
		dbAction.setTable(table);
		if (record.containsKey("user"))
			dbAction.setUser((String)record.get("user"));
		dbAction.setAction(action);
		dbAction.setRecordId((String)record.get("id"));
		if (data != null && syncEnabled) {
			ObjectMapper mapper = JsonMapper.builder()
					.enable(JsonWriteFeature.ESCAPE_NON_ASCII)
					.build();
			try {
				dbAction.setJsonData(mapper.writeValueAsString(data));
			} catch (JsonProcessingException ex) {
				throw new RuntimeException("Can't convert data to JSON: " +
						ex.getMessage(), ex);
			}
		}
		dbAction.setTime(now);
		dbAction.setOrder(order);
		dbAction.setSource(source);
		setDatabaseActionSampleTime(record, dbAction);
		actions.add(dbAction);
	}
	
	private void setDatabaseActionSampleTime(Map<String,?> record,
			DatabaseAction dbAction) {
		Object sampleUtcTime = record.get("utcTime");
		if (sampleUtcTime instanceof Long) {
			dbAction.setSampleTime((Long)sampleUtcTime);
			return;
		}
		Object sampleLocalTimeObj = record.get("localTime");
		if (!(sampleLocalTimeObj instanceof String)) {
			return;
		}
		String sampleLocalTimeStr = (String)sampleLocalTimeObj;
		DateTimeFormatter parser = DateTimeFormatter.ofPattern(
				"yyyy-MM-dd'T'HH:mm:ss.SSS");
		LocalDateTime sampleLocalTime;
		try {
			sampleLocalTime = parser.parse(sampleLocalTimeStr,
					LocalDateTime::from);
		} catch (IllegalArgumentException ex) {
			return;
		}
		ZonedDateTime sampleTime = sampleLocalTime.atZone(ZoneOffset.UTC);
		dbAction.setSampleTime(sampleTime.toInstant().toEpochMilli());
	}

	/**
	 * Selects records that match the specified criteria from a physical
	 * database table. The result is used to log a database action. Each
	 * record should be a map with the following keys:
	 *
	 * <p><ul>
	 * <li>id (required): the record ID</li>
	 * <li>user (optional): if the record has a "user" field, its value should
	 * be set here. Otherwise it can be omitted or set to null.</li>
	 * <li>utcTime (optional): if the record has a "utcTime" field, its value
	 * should be set here. Otherwise it can be omitted or set to null.</li>
	 * <li>localTime (optional): if the record has a "localTime" field, its
	 * value should be set here. Otherwise it can be omitted or set to
	 * null.</li>
	 * </ul></p>
	 *
	 * @param table the (physical) table name (lower case)
	 * @param dataClass the data class
	 * @param criteria the criteria. This can be null.
	 * @return the records
	 * @throws DatabaseException if a database error occurs
	 */
	protected abstract List<? extends Map<String,?>> selectLogRecords(
			String table, Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria) throws DatabaseException;
}
