package nl.rrd.senseeact.dao;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rrd.utils.exception.DatabaseException;

/**
 * The database cache keeps metadata in memory per database. This is used by
 * the {@link Database Database} class to reduce the number of database
 * queries, which significantly improves performance.
 * 
 * <p>When a database or table is deleted, it should also be removed from the
 * cache with {@link #removeDatabase(String) removeDatabase()} or {@link
 * #removeLogicalTable(Database, String) removeLogicalTable()}. This is done
 * automatically from {@link Database Database} or {@link DatabaseConnection
 * DatabaseConnection}.</p>
 * 
 * <p>All methods are thread-safe.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class DatabaseCache {
	private static final Object STATIC_LOCK = new Object();
	private static DatabaseCache instance = null;
	
	private Map<String,DatabaseCachedMetadata> databases =
			new LinkedHashMap<>();
	private SecureRandom random = new SecureRandom();
	
	/**
	 * Returns the database cache.
	 * 
	 * @return the database cache
	 */
	public static DatabaseCache getInstance() {
		synchronized (STATIC_LOCK) {
			if (instance == null)
				instance = new DatabaseCache();
			return instance;
		}
	}
	
	/**
	 * This private constructor is used in {@link #getInstance()
	 * getInstance()}.
	 */
	private DatabaseCache() {
	}

	/**
	 * Returns whether the cache returns the specified database.
	 *
	 * @param dbName the database name
	 * @return true if the cache contains the database, false otherwise
	 */
	public boolean containsDatabase(String dbName) {
		synchronized (STATIC_LOCK) {
			return databases.containsKey(dbName);
		}
	}
	
	/**
	 * Removes all cached metadata for the specified database.
	 * 
	 * @param dbName the database name
	 */
	public void removeDatabase(String dbName) {
		synchronized (STATIC_LOCK) {
			databases.remove(dbName);
		}
	}
	
	/**
	 * Returns the _action_log table for the specified user and table. If the
	 * table does not exist in the cache yet, this method will initialize it
	 * with {@link Database#initTable(DatabaseTableDef) Database.initTable()}.
	 * 
	 * @param db the database
	 * @param user the user (user ID)
	 * @param table the table name
	 * @return the database action table
	 * @throws DatabaseException if a database error occurs
	 */
	public DatabaseActionTable initActionTable(Database db, String user,
			String table) throws DatabaseException {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			UserTable userTable = new UserTable(user, table);
			DatabaseActionTable actionTable = cache.actionTables.get(
					userTable);
			if (actionTable != null)
				return actionTable;
			actionTable = new DatabaseActionTable(getUserTableKey(db, user,
					table));
			db.createActionTable(actionTable);
			cache.actionTables.put(userTable, actionTable);
			return actionTable;
		}
	}
	
	/**
	 * Returns the {@link UserTableKey UserTableKey} for the specified user and
	 * (logical) table. If the key does not exist in the cache, this method
	 * tries to retrieve it from the database. If it still does not exist, it
	 * will generate a new key and add it to the database and the cache.
	 * 
	 * @param db the database
	 * @param user the user (user ID)
	 * @param table the (logical) table name
	 * @return the user table key
	 * @throws DatabaseException if a database error occurs
	 */
	public UserTableKey getUserTableKey(Database db, String user, String table)
			throws DatabaseException {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			UserTable userTable = new UserTable(user, table);
			UserTableKey key = cache.userTableKeys.get(userTable);
			if (key != null)
				return key;
			DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", user),
					new DatabaseCriteria.Equal("table", table)
			);
			key = db.selectOne(new UserTableKeyTable(), criteria,
					null);
			if (key == null) {
				key = new UserTableKey();
				key.setUser(user);
				key.setTable(table);
				key.setKey(generateKey(db));
				db.insert(UserTableKeyTable.NAME, key);
			}
			cache.userTableKeys.put(userTable, key);
			return key;
		}
	}
	
	/**
	 * Returns the cached table names of all logical tables. That is all tables
	 * that occur in _table_metadata or all tables that have been initialized
	 * with {@link Database#initTable(DatabaseTableDef) Database.initTable()},
	 * including reserved tables (whose names start with an underscore).
	 * This is used to speed up {@link Database#selectTables()
	 * Database.selectTables()}. The tables are sorted by name.
	 * 
	 * <p>This method returns a copy of the list in the cache (to prevent
	 * concurrent modification exceptions). If the tables have not been set
	 * yet, this method returns null. In that case the first call of {@link
	 * Database#selectTables() Database.selectTables()} will read the table
	 * names from _table_metadata and then call {@link
	 * #setLogicalTables(Database, List) setLogicalTables()}.</p>
	 * 
	 * <p>If any table is created after that, it should be added with {@link
	 * #addLogicalTable(Database, String) addLogicalTable()}. If a table is
	 * removed, it should be removed with {@link
	 * #removeLogicalTable(Database, String) removeLogicalTable()}. The {@link
	 * Database Database} class takes care of this.</p>
	 * 
	 * @param db the database
	 * @return the table names (sorted by name) or null
	 */
	public List<String> getLogicalTables(Database db) {
		synchronized (STATIC_LOCK) {
			List<String> result = getCachedMetadata(db).logicalTables;
			if (result == null)
				return null;
			else
				return new ArrayList<>(result);
		}
	}
	
	/**
	 * Sets the table names of all logical tables. That is all tables that occur
	 * in _table_metadata or all tables that have been initialized with {@link
	 * Database#initTable(DatabaseTableDef) Database.initTable()}, including
	 * reserved tables (whose names start with an underscore). This is used to
	 * speed up {@link Database#selectTables() Database.selectTables()}. The
	 * tables should be sorted by name.
	 * 
	 * <p>The method {@link Database#selectTables() Database.selectTables()}
	 * calls this method at its first call. If any table is created after that,
	 * it should be added with {@link #addLogicalTable(Database, String)
	 * addLogicalTable()}. If a table is removed, it should be removed with
	 * {@link #removeLogicalTable(Database, String) removeLogicalTable()}. The
	 * {@link Database Database} class takes care of this.</p>
	 * 
	 * @param db the database
	 * @param tables the table names (sorted by name)
	 */
	public void setLogicalTables(Database db, List<String> tables) {
		synchronized (STATIC_LOCK) {
			getCachedMetadata(db).logicalTables = tables;
		}
	}
	
	/**
	 * Adds a table name to the list of logical tables. That is all tables that
	 * occur in _table_metadata. The list is initialized with {@link
	 * #setLogicalTables(Database, List) setLogicalTables()} at the first call
	 * of {@link Database#selectTables() Database.selectTables()}. This method
	 * must be called if any table is added after that, but it may be called
	 * whenever a table is created. If {@link #setLogicalTables(Database, List)
	 * setLogicalTables()} hasn't been called yet, or if the table name is
	 * already in the list, this method has no effect. This method is
	 * automatically called from {@link Database Database} when a table is
	 * created.
	 * 
	 * @param db the database
	 * @param table the table name
	 */
	public void addLogicalTable(Database db, String table) {
		synchronized (STATIC_LOCK) {
			List<String> tables = getCachedMetadata(db).logicalTables;
			if (tables == null || tables.contains(table))
				return;
			tables.add(table);
			Collections.sort(tables);
		}
	}
	
	/**
	 * Returns the physical database tables in the underlying database. They
	 * may exclude subtables (a physical table for a database field within a
	 * {@link DatabaseObject DatabaseObject}). It includes the following
	 * tables:
	 * 
	 * <p><ul>
	 * <li>All tables that existed in the database when it was first
	 * initialized (including subtables)</li>
	 * <li>All tables where property splitByUser is false</li>
	 * <li>All user tables (for logical tables where property splitByUser is
	 * true)</li>
	 * </ul></p>
	 * 
	 * <p>This method returns a copy of the list in the cache (to prevent
	 * concurrent modification exceptions). If the tables have not been set
	 * with {@link #setPhysicalTables(Database, List) setPhysicalTables()} yet,
	 * this method returns null. If any table (as listed above) is created
	 * later, it should be added with {@link #addPhysicalTable(Database, String)
	 * addPhysicalTable()}. Physical tables are removed from the cache when you
	 * remove a logical table with {@link #removeLogicalTable(Database, String)
	 * removeLogicalTable()}. The {@link Database Database} class takes care of
	 * this.</p>
	 * 
	 * @param db the database
	 * @return the table names or null
	 */
	public List<String> getPhysicalTables(Database db) {
		synchronized (STATIC_LOCK) {
			List<String> result = getCachedMetadata(db).physicalTables;
			if (result == null)
				return null;
			else
				return new ArrayList<>(result);
		}
	}
	
	/**
	 * Sets the physical database tables. This should be all physical tables
	 * that exist in the underlying database when the database is first
	 * initialized.
	 * 
	 * <p>If the following tables are created later, they should be added with
	 * {@link #addPhysicalTable(Database, String) addPhysicalTable()}:</p>
	 * 
	 * <p><ul>
	 * <li>All tables where property splitByUser is false</li>
	 * <li>All user tables (for logical tables where property splitByUser is
	 * true)</li>
	 * </ul></p>
	 * 
	 * <p>Physical tables are removed from the cache when you remove a logical
	 * table with {@link #removeLogicalTable(Database, String)
	 * removeLogicalTable()}. The {@link Database Database} class takes care of
	 * this.</p>
	 * 
	 * @param db the database
	 * @param tables the table names (sorted by name)
	 */
	public void setPhysicalTables(Database db, List<String> tables) {
		synchronized (STATIC_LOCK) {
			getCachedMetadata(db).physicalTables = tables;
		}
	}
	
	/**
	 * Adds the name of a physical database table to the cache. It's added to
	 * the list of physical tables in the underlying database. This method
	 * should be called if the following tables are created:
	 * 
	 * <p><ul>
	 * <li>All tables where property splitByUser is false</li>
	 * <li>All user tables (for logical tables where property splitByUser is
	 * true)</li>
	 * </ul></p>
	 * 
	 * <p>The list is initialized with {@link #setPhysicalTables(Database, List)
	 * setPhysicalTables()}. If the set method hasn't been called yet, or if the
	 * table name is already in the list, this method has no effect. This method
	 * is automatically called from {@link Database Database}.</p>
	 * 
	 * @param db the database
	 * @param table the table name
	 */
	public void addPhysicalTable(Database db, String table) {
		synchronized (STATIC_LOCK) {
			List<String> tables = getCachedMetadata(db).physicalTables;
			if (tables == null || tables.contains(table))
				return;
			tables.add(table);
			Collections.sort(tables);
		}
	}
	
	public List<TableMetadata> getTableMetadata(Database db, String table)
			throws DatabaseException {
		synchronized (STATIC_LOCK) {
			Map<String,List<TableMetadata>> tableMetadata =
					getTableMetadata(db);
			List<TableMetadata> metas = tableMetadata.get(table);
			if (metas == null)
				return new ArrayList<>();
			else
				return new ArrayList<>(metas);
		}
	}
	
	public void setTableMetadata(Database db, String table,
			List<TableMetadata> metas) throws DatabaseException {
		synchronized (STATIC_LOCK) {
			Map<String,List<TableMetadata>> tableMetadata =
					getTableMetadata(db);
			tableMetadata.put(table, metas);
		}
	}
	
	private Map<String,List<TableMetadata>> getTableMetadata(Database db)
			throws DatabaseException {
		Map<String,List<TableMetadata>> tableMetadata =
				getCachedMetadata(db).tableMetadata;
		if (tableMetadata != null)
			return tableMetadata;
		tableMetadata = new LinkedHashMap<>();
		TableMetadataTableDef metaDef = new TableMetadataTableDef();
		List<TableMetadata> allMeta = db.select(metaDef, null, 0, null);
		for (TableMetadata meta : allMeta) {
			List<TableMetadata> metas = tableMetadata.get(
					meta.getTable());
			if (metas == null) {
				metas = new ArrayList<>();
				tableMetadata.put(meta.getTable(), metas);
			}
			metas.add(meta);
		}
		getCachedMetadata(db).tableMetadata = tableMetadata;
		return tableMetadata;
	}
	
	
	/**
	 * Returns the names of the database fields for the specified (logical)
	 * table. It excludes field "id". It tries to get the field names from the
	 * cache. If the field names are not in the cache, it will read them from
	 * the metadata table and save them in the cache.
	 * 
	 * <p>This method should only be called after the database has been
	 * initialized and any table upgrades have been performed, so the table
	 * fields will not change anymore while the process is running.</p>
	 * 
	 * @param db the database
	 * @param table the (logical) table name
	 * @return the field names
	 * @throws DatabaseException if a database error occurs
	 */
	public List<String> getTableFields(Database db, String table)
			throws DatabaseException {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			List<String> fields = cache.tableFields.get(table);
			if (fields != null)
				return fields;
			List<TableMetadata> metas = getTableMetadata(db, table);
			TableMetadata meta = TableMetadata.findKey(metas,
					TableMetadata.KEY_FIELDS);
			ObjectMapper mapper = new ObjectMapper();
			try {
				fields = mapper.readValue(meta.getValue(),
						new TypeReference<List<String>>() {});
			} catch (IOException ex) {
				throw new RuntimeException("Can't parse JSON array: " +
						ex.getMessage(), ex);
			}
			cache.tableFields.put(table, fields);
			return fields;
		}
	}
	
	/**
	 * Sets the names of the database fields for a (logical) table in the cache
	 * and in the metadata table where needed. It excludes field "id". This
	 * method is called from {@link Database#initTable(DatabaseTableDef)
	 * Database.initTable()}, after any upgrades were performed. This means
	 * that the table fields will not change while the process is running.
	 * 
	 * <p>You should specify the metadata object with {@link
	 * TableMetadata#KEY_FIELDS KEY_FIELDS} that is currently in the metadata
	 * table. If there is no object in the table, you can set it to null.</p>
	 * 
	 * <p>This method checks whether the field names were already set in the
	 * cache. If so, it just returns the specified metadata object (which
	 * should not be null at this point), so this method returns quickly after
	 * the first call.</p>
	 * 
	 * <p>If the field names are not in the cache yet, it will read the
	 * specified metadata object and update the metadata table if needed before
	 * saving the field names in the cache. Then it returns the new metadata
	 * object.</p>
	 * 
	 * @param db the database
	 * @param table the (logical) table name
	 * @param fields the field names
	 * @param metadata the current metadata object or null
	 * @return the new metadata object
	 * @throws DatabaseException if a database error occurs
	 */
	public TableMetadata setTableFields(Database db, String table,
			List<String> fields, TableMetadata metadata)
			throws DatabaseException {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			List<String> cachedFields = cache.tableFields.get(table);
			if (cachedFields != null)
				return metadata;
			ObjectMapper mapper = new ObjectMapper();
			String fieldsStr;
			try {
				fieldsStr = mapper.writeValueAsString(fields);
			} catch (JsonProcessingException ex) {
				throw new RuntimeException(
						"Can't convert string list to JSON: " +
						ex.getMessage(), ex);
			}
			if (metadata == null) {
				metadata = new TableMetadata();
				metadata.setTable(table);
				metadata.setKey(TableMetadata.KEY_FIELDS);
				metadata.setValue(fieldsStr);
				db.insert(TableMetadataTableDef.NAME, metadata);
			} else if (!metadata.getValue().equals(fieldsStr)) {
				metadata.setValue(fieldsStr);
				db.update(TableMetadataTableDef.NAME, metadata);
			}
			cache.tableFields.put(table, fields);
			return metadata;
		}
	}
	
	/**
	 * Returns the data class for the specified (logical) table. It tries to get
	 * the data class from the cache. If the data class is not in the cache, it
	 * will read it from the metadata table and save it in the cache.
	 * 
	 * <p>This method should only be called after the database has been
	 * initialized and any table upgrades have been performed, so the data
	 * class will not change anymore while the process is running.</p>
	 * 
	 * @param db the database
	 * @param table the (logical) table name
	 * @return the data class
	 * @throws DatabaseException if a database error occurs
	 */
	public Class<? extends DatabaseObject> getTableDataClass(Database db,
			String table) throws DatabaseException {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			Class<? extends DatabaseObject> dataClass =
					cache.tableDataClasses.get(table);
			if (dataClass != null)
				return dataClass;
			List<TableMetadata> metas = getTableMetadata(db, table);
			TableMetadata meta = TableMetadata.findKey(metas,
					TableMetadata.KEY_DATA_CLASS);
			String className = meta.getValue();
			try {
				dataClass = Class.forName(className).asSubclass(
						DatabaseObject.class);
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException("Unknown data class: " + className +
						": " + ex.getMessage(), ex);
			}
			cache.tableDataClasses.put(table, dataClass);
			return dataClass;
		}
	}
	
	/**
	 * Sets the data class for a (logical) table in the cache and in the
	 * metadata table where needed. This method is called from {@link
	 * Database#initTable(DatabaseTableDef) Database.initTable()}, after any
	 * upgrades were performed. This means that the data class will not change
	 * while the process is running.
	 * 
	 * <p>You should specify the metadata object with {@link
	 * TableMetadata#KEY_DATA_CLASS KEY_DATA_CLASS} that is currently in the
	 * metadata table. If there is no object in the table, you can set it to
	 * null.</p>
	 * 
	 * <p>This method checks whether the data class was already set in the
	 * cache. If so, it just returns the specified metadata object (which
	 * should not be null at this point), so this method returns quickly after
	 * the first call.</p>
	 * 
	 * <p>If the data class is not in the cache yet, it will read the
	 * specified metadata object and update the metadata table if needed before
	 * saving the data class in the cache. Then it returns the new metadata
	 * object.</p>
	 * 
	 * @param db the database
	 * @param table the (logical) table name
	 * @param dataClass the data class
	 * @param metadata the current metadata object or null
	 * @return the new metadata object
	 * @throws DatabaseException if a database error occurs
	 */
	public TableMetadata setTableDataClass(Database db, String table,
			Class<? extends DatabaseObject> dataClass, TableMetadata metadata)
			throws DatabaseException {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			Class<? extends DatabaseObject> cachedDataClass =
					cache.tableDataClasses.get(table);
			if (cachedDataClass != null)
				return metadata;
			String dataClassName = dataClass.getName();
			if (metadata == null) {
				metadata = new TableMetadata();
				metadata.setTable(table);
				metadata.setKey(TableMetadata.KEY_DATA_CLASS);
				metadata.setValue(dataClassName);
				db.insert(TableMetadataTableDef.NAME, metadata);
			} else if (!metadata.getValue().equals(dataClassName)) {
				metadata.setValue(dataClassName);
				db.update(TableMetadataTableDef.NAME, metadata);
			}
			cache.tableDataClasses.put(table, dataClass);
			return metadata;
		}
	}
	
	/**
	 * Returns the compound indexes for the specified (logical) table. It tries
	 * to get the indexes from the cache. If the indexes are not in the cache,
	 * it will read them from the metadata table and save them in the cache.
	 * 
	 * <p>This method should only be called after the database has been
	 * initialized and any table upgrades have been performed, so the indexes
	 * will not change anymore while the process is running.</p>
	 * 
	 * @param db the database
	 * @param table the (logical) table name
	 * @return the compound indexes
	 * @throws DatabaseException if a database error occurs
	 */
	public List<DatabaseIndex> getTableCompoundIndexes(Database db,
			String table) throws DatabaseException {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			List<DatabaseIndex> indexes = cache.tableCompoundIndexes.get(
					table);
			if (indexes != null)
				return indexes;
			List<TableMetadata> metas = getTableMetadata(db, table);
			TableMetadata meta = TableMetadata.findKey(metas,
					TableMetadata.KEY_INDEXES);
			ObjectMapper mapper = new ObjectMapper();
			try {
				indexes = mapper.readValue(meta.getValue(),
						new TypeReference<List<DatabaseIndex>>() {});
			} catch (IOException ex) {
				throw new RuntimeException("Can't parse JSON array: " +
						ex.getMessage(), ex);
			}
			cache.tableCompoundIndexes.put(table, indexes);
			return indexes;
		}
	}
	
	/**
	 * Sets the compound indexes for a (logical) table in the cache and in the
	 * metadata table where needed. This method is called from {@link
	 * Database#initTable(DatabaseTableDef) Database.initTable()}, after any
	 * upgrades were performed. This means that the indexes will not change
	 * while the process is running.
	 * 
	 * <p>You should specify the metadata object with {@link
	 * TableMetadata#KEY_INDEXES KEY_INDEXES} that is currently in the metadata
	 * table. If there is no object in the table, you can set it to null.</p>
	 * 
	 * <p>This method checks whether the indexes were already set in the cache.
	 * If so, it just returns the specified metadata object (which should not be
	 * null at this point), so this method returns quickly after the first
	 * call.</p>
	 * 
	 * <p>If the indexes are not in the cache yet, it will read the
	 * specified metadata object and update the metadata table if needed before
	 * saving the indexes in the cache. Then it returns the new metadata
	 * object.</p>
	 * 
	 * @param db the database
	 * @param table the (logical) table name
	 * @param compoundIndexes the compound indexes
	 * @param metadata the current metadata object or null
	 * @return the new metadata object
	 * @throws DatabaseException if a database error occurs
	 */
	public TableMetadata setTableCompoundIndexes(Database db, String table,
			List<DatabaseIndex> compoundIndexes, TableMetadata metadata)
			throws DatabaseException {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			List<DatabaseIndex> cachedIndexes = cache.tableCompoundIndexes.get(
					table);
			if (cachedIndexes != null)
				return metadata;
			ObjectMapper mapper = new ObjectMapper();
			String json;
			try {
				json = mapper.writeValueAsString(compoundIndexes);
			} catch (JsonProcessingException ex) {
				throw new RuntimeException(
						"Can't generate JSON string for table compound indexes: " +
						ex.getMessage(), ex);
			}
			if (metadata == null) {
				metadata = new TableMetadata();
				metadata.setTable(table);
				metadata.setKey(TableMetadata.KEY_INDEXES);
				metadata.setValue(json);
				db.insert(TableMetadataTableDef.NAME, metadata);
			} else if (!metadata.getValue().equals(json)) {
				metadata.setValue(json);
				db.update(TableMetadataTableDef.NAME, metadata);
			}
			cache.tableCompoundIndexes.put(table, compoundIndexes);
			return metadata;
		}
	}
	
	/**
	 * Returns the splitByUser property for the specified (logical) table. It
	 * tries to get the property from the cache. If the property is not in the
	 * cache, it will read it from the metadata table and save it in the cache.
	 * 
	 * <p>This method should only be called after the database has been
	 * initialized and any table upgrades have been performed, so the property
	 * will not change anymore while the process is running.</p>
	 * 
	 * @param db the database
	 * @param table the (logical) table name
	 * @return true if the table should be split by user, false otherwise
	 * @throws DatabaseException if a database error occurs
	 */
	public boolean isTableSplitByUser(Database db, String table)
			throws DatabaseException {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			Boolean splitByUser = cache.tableSplitByUser.get(table);
			if (splitByUser != null)
				return splitByUser;
			List<TableMetadata> metas = getTableMetadata(db, table);
			TableMetadata meta = TableMetadata.findKey(metas,
					TableMetadata.KEY_SPLIT_BY_USER);
			splitByUser = Boolean.parseBoolean(meta.getValue());
			cache.tableSplitByUser.put(table, splitByUser);
			return splitByUser;
		}
	}
	
	/**
	 * Sets the splitByUser property for a (logical) table in the cache and in
	 * the metadata table where needed. This method is called from {@link
	 * Database#initTable(DatabaseTableDef) Database.initTable()}, after any
	 * upgrades were performed. This means that the property value will not
	 * change while the process is running.
	 * 
	 * <p>You should specify the metadata object with {@link
	 * TableMetadata#KEY_SPLIT_BY_USER KEY_SPLIT_BY_USER} that is currently in
	 * the metadata table. If there is no object in the table, you can set it to
	 * null.</p>
	 * 
	 * <p>This method checks whether the property was already set in the cache.
	 * If so, it just returns the specified metadata object (which should not be
	 * null at this point), so this method returns quickly after the first
	 * call.</p>
	 * 
	 * <p>If the splitByUser property is not in the cache yet, it will read the
	 * specified metadata object and update the metadata table if needed before
	 * saving the property in the cache. Then it returns the new metadata
	 * object.</p>
	 * 
	 * @param db the database
	 * @param table the (logical) table name
	 * @param splitByUser true if the table should be split by user, false
	 * otherwise
	 * @param metadata the current metadata object or null
	 * @return the new metadata object
	 * @throws DatabaseException if a database error occurs
	 */
	public TableMetadata setTableSplitByUser(Database db, String table,
			boolean splitByUser, TableMetadata metadata)
			throws DatabaseException {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			Boolean cachedSplitByUser = cache.tableSplitByUser.get(table);
			if (cachedSplitByUser != null)
				return metadata;
			String strValue = Boolean.toString(splitByUser);
			if (metadata == null) {
				metadata = new TableMetadata();
				metadata.setTable(table);
				metadata.setKey(TableMetadata.KEY_SPLIT_BY_USER);
				metadata.setValue(strValue);
				db.insert(TableMetadataTableDef.NAME, metadata);
			} else if (!metadata.getValue().equals(strValue)) {
				metadata.setValue(strValue);
				db.update(TableMetadataTableDef.NAME, metadata);
			}
			cache.tableSplitByUser.put(table, splitByUser);
			return metadata;
		}
	}

	/**
	 * Removes all references to the specified logical database table from the
	 * cache. This is called from {@link Database#dropTable(String)
	 * Database.dropTable()}. It does not remove related physical tables from
	 * the cache.
	 * 
	 * @param db the database
	 * @param table the table name
	 */
	public void removeLogicalTable(Database db, String table) {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			List<UserTable> actionTableKeys = new ArrayList<>(
					cache.actionTables.keySet());
			for (UserTable key : actionTableKeys) {
				if (key.getTable().equals(table))
					cache.actionTables.remove(key);
			}
			List<UserTable> userTableKeys = new ArrayList<>(
					cache.userTableKeys.keySet());
			for (UserTable key : userTableKeys) {
				if (key.getTable().equals(table))
					cache.userTableKeys.remove(key);
			}
			if (cache.logicalTables != null)
				cache.logicalTables.remove(table);
			cache.tableCompoundIndexes.remove(table);
			cache.tableDataClasses.remove(table);
			cache.tableFields.remove(table);
			cache.tableMetadata.remove(table);
			cache.tableSplitByUser.remove(table);
		}
	}
	
	/**
	 * Removes the specified physical table from the cache.
	 * 
	 * @param db the database
	 * @param table the physical table
	 */
	public void removePhysicalTable(Database db, String table) {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			if (cache.physicalTables != null)
				cache.physicalTables.remove(table);
		}
	}
	
	/**
	 * Removes references to the specified user-table key from the cache.
	 * 
	 * @param db the database
	 * @param user the user
	 * @param table the table
	 */
	public void removeUserTable(Database db, String user, String table) {
		synchronized (STATIC_LOCK) {
			DatabaseCachedMetadata cache = getCachedMetadata(db);
			UserTable key = new UserTable(user, table);
			cache.actionTables.remove(key);
			cache.userTableKeys.remove(key);
		}
	}
	
	/**
	 * Returns the cached metadata for the specified database.
	 * 
	 * @param db the database
	 * @return the cached metadata
	 */
	private DatabaseCachedMetadata getCachedMetadata(Database db) {
		DatabaseCachedMetadata cache = databases.get(db.getName());
		if (cache == null) {
			cache = new DatabaseCachedMetadata();
			databases.put(db.getName(), cache);
		}
		return cache;
	}
	
	/**
	 * Generates a random 32-bit key (8 hex characters) that doesn't exist yet
	 * in table "_user_table_keys" in the specified database.
	 * 
	 * @param db the database
	 * @return the key
	 * @throws DatabaseException if a database error occurs
	 */
	private String generateKey(Database db) throws DatabaseException {
		int randNum = random.nextInt();
		while (true) {
			String key = String.format("%08x", randNum);
			DatabaseCriteria criteria = new DatabaseCriteria.Equal("key", key);
			if (db.count(new UserTableKeyTable(), criteria) == 0)
				return key;
			randNum++;
		}
	}
	
	/**
	 * Cache of metadata for one database.
	 */
	private static class DatabaseCachedMetadata {
		public Map<UserTable,DatabaseActionTable> actionTables =
				new LinkedHashMap<>();
		public Map<UserTable,UserTableKey> userTableKeys =
				new LinkedHashMap<>();
		public List<String> logicalTables = null;
		public List<String> physicalTables = null;
		public Map<String,List<TableMetadata>> tableMetadata = null;
		public Map<String,List<String>> tableFields = new LinkedHashMap<>();
		public Map<String,Class<? extends DatabaseObject>> tableDataClasses =
				new LinkedHashMap<>();
		public Map<String,List<DatabaseIndex>> tableCompoundIndexes =
				new LinkedHashMap<>();
		public Map<String,Boolean> tableSplitByUser = new LinkedHashMap<>();
	}
}
