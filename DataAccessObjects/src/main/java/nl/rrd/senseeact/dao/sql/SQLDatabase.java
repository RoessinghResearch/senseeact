package nl.rrd.senseeact.dao.sql;

import nl.rrd.senseeact.dao.*;
import nl.rrd.utils.exception.DatabaseException;

import java.util.*;

/**
 * Base class for SQL implementations of {@link Database Database}.
 *
 * @author Dennis Hofs (RRD)
 */
public abstract class SQLDatabase extends Database {
	private SQLQueryRunner queryRunner;

	/**
	 * Constructs a new SQL database.
	 *
	 * @param name the database name
	 * @param queryRunner the SQL query runner
	 */
	public SQLDatabase(String name, SQLQueryRunner queryRunner) {
		super(name);
		this.queryRunner = queryRunner;
	}

	/**
	 * Returns the SQL query runner.
	 *
	 * @return the SQL query runner
	 */
	public SQLQueryRunner getQueryRunner() {
		return queryRunner;
	}

	@Override
	protected void createTable(String table, List<DatabaseColumnDef> columns)
			throws DatabaseException {
		StringBuilder sql = new StringBuilder("CREATE TABLE " +
				escapeName(table) + " (");
		sql.append(escapeName("_id") + " " +
				getColumnSqlType(DatabaseType.STRING) + " PRIMARY KEY");
		List<String> idxCols = new ArrayList<>();
		for (DatabaseColumnDef column : columns) {
			String colType = getColumnSqlType(column.getType());
			if (colType != null) {
				if (isIndexColumn(column))
					idxCols.add(column.getName());
				sql.append(", " + escapeName(column.getName()) + " " +
						colType);
			}
		}
		sql.append(")");
		queryRunner.execSQL(sql.toString());
		for (String idxCol : idxCols) {
			createIndex(table, idxCol);
		}
	}

	@Override
	public void createIndex(String table, DatabaseIndex index)
			throws DatabaseException {
		StringBuilder columnList = new StringBuilder();
		for (String field : index.getFields()) {
			if (columnList.length() > 0)
				columnList.append(", ");
			columnList.append(escapeName(field));
		}
		String sql = "CREATE INDEX " + escapeName(table + "." +
				index.getName()) +
				" ON " + escapeName(table) + " (" + columnList + ")";
		queryRunner.execSQL(sql);
	}

	/**
	 * Returns whether table and column names in queries should be escaped
	 * with backticks.
	 *
	 * @return true if names should be escaped with backticks, false otherwise
	 */
	protected abstract boolean isBacktickNames();

	/**
	 * If {@link #isBacktickNames() isBacktickNames()} returns true, this method
	 * escapes the specified name with backticks. Otherwise it just returns the
	 * name.
	 *
	 * @param name the name
	 * @return the escaped name
	 */
	protected String escapeName(String name) {
		if (!isBacktickNames())
			return name;
		else
			return "`" + name + "`";
	}

	/**
	 * Returns the SQL column type for the specified database type. If no
	 * column should be created for this type, then this method returns null.
	 *
	 * @param type the database type
	 * @return the SQL column type or null
	 */
	protected abstract String getColumnSqlType(DatabaseType type);
	
	/**
	 * Returns the indices on the specified table. When indices are created with
	 * this class their names are formatted as table.indexName. This method only
	 * returns those indices and returned index names are without the table
	 * prefix, so the result corresponds to the argument of {@link
	 * #createIndex(String, DatabaseIndex) createIndex()}.
	 *
	 * @param table the table name
	 * @return the indices
	 * @throws DatabaseException if a database error occurs
	 */
	protected abstract List<DatabaseIndex> getIndices(String table)
			throws DatabaseException;

	/**
	 * Returns whether the specified column should have an index. This should
	 * only be called if {@link #getColumnSqlType(DatabaseType)
	 * getColumnSqlType()} does not return null.
	 * 
	 * @param column the column
	 * @return true if the column should have an index, false otherwise
	 */
	private boolean isIndexColumn(DatabaseColumnDef column) {
		return column.isIndex();
	}
	
	/**
	 * Creates an index on the specified table and column.
	 * 
	 * @param table the table name
	 * @param column the column name
	 * @throws DatabaseException if a database error occurs
	 */
	private void createIndex(String table, String column)
			throws DatabaseException {
		String sql = "CREATE INDEX " + escapeName(table + "." + column) +
				" ON " + escapeName(table) + " (" + escapeName(column) + ")";
		queryRunner.execSQL(sql);
	}

	@Override
	public void addColumn(String table, DatabaseColumnDef column)
			throws DatabaseException {
		String sqlType = getColumnSqlType(column.getType());
		if (sqlType != null) {
			String sql = "ALTER TABLE " + escapeName(table) + " ADD COLUMN " +
					escapeName(column.getName()) + " " + sqlType;
			queryRunner.execSQL(sql);
			if (isIndexColumn(column))
				createIndex(table, column.getName());
		}
	}

	@Override
	public void dropColumn(String table, String column)
			throws DatabaseException {
		List<String> tables = selectDbTables();
		String subPrefix = table + Database.TABLE_TOKEN_SEP +
				column.toLowerCase();
		for (String subTable : tables) {
			if (subTable.equals(subPrefix) || subTable.startsWith(subPrefix +
					Database.TABLE_TOKEN_SEP)) {
				queryRunner.execSQL("DROP TABLE " + escapeName(subTable));
			}
		}
		Map<String,String> columns = getTableColumns(table, null);
		List<DatabaseIndex> oldIdxs = getIndices(table);
		List<DatabaseIndex> newIdxs = new ArrayList<>();
		for (DatabaseIndex oldIdx : oldIdxs) {
			List<String> fields = Arrays.asList(oldIdx.getFields());
			if (!fields.contains(column))
				newIdxs.add(oldIdx);
		}
		columns.remove("_id");
		List<String> oldCols = new ArrayList<>();
		List<String> newCols = new ArrayList<>();
		for (String colName : columns.keySet()) {
			if (!colName.equals(column)) {
				oldCols.add(colName);
				newCols.add(colName);
			}
		}
		alterTableColumns(table, columns, oldCols, oldIdxs, newCols, newIdxs);
	}

	@Override
	public void renameColumn(String table, String oldName, String newName)
			throws DatabaseException {
		List<String> tables = selectDbTables();
		String subPrefix = table + Database.TABLE_TOKEN_SEP +
				oldName.toLowerCase();
		for (String subTable : tables) {
			String renameTo = null;
			if (subTable.equals(subPrefix) || subTable.startsWith(subPrefix +
					Database.TABLE_TOKEN_SEP)) {
				renameTo = table + Database.TABLE_TOKEN_SEP +
						newName.toLowerCase() + subTable.substring(
						subPrefix.length());
			}
			if (renameTo != null)
				queryRunner.renameTable(subTable, renameTo);
		}
		Map<String,String> columns = getTableColumns(table, null);
		List<DatabaseIndex> oldIdxs = getIndices(table);
		List<DatabaseIndex> newIdxs = new ArrayList<>();
		for (DatabaseIndex oldIdx : oldIdxs) {
			List<String> fields = Arrays.asList(oldIdx.getFields());
			if (fields.contains(oldName)) {
				String newIdxName = oldIdx.getName().equals(oldName) ?
						newName : oldIdx.getName();
				String[] newFields = new String[fields.size()];
				for (int i = 0; i < fields.size(); i++) {
					String newFieldName = fields.get(i);
					if (newFieldName.equals(oldName))
						newFieldName = newName;
					newFields[i] = newFieldName;
				}
				newIdxs.add(new DatabaseIndex(newIdxName, newFields));
			} else {
				newIdxs.add(oldIdx);
			}
		}
		columns.remove("_id");
		List<String> oldCols = new ArrayList<>();
		List<String> newCols = new ArrayList<>();
		for (String colName : columns.keySet()) {
			oldCols.add(colName);
			String addName = colName.equals(oldName) ? newName : colName;
			newCols.add(addName);
		}
		alterTableColumns(table, columns, oldCols, oldIdxs, newCols, newIdxs);
	}

	/**
	 * Alters the set of columns of a table. This can be used to drop a column
	 * or rename a column. The parameters oldCols and newCols should have the
	 * same length and there should be a one-to-one mapping between them.
	 *
	 * @param table the table name
	 * @param colTypes a map from current column names to column types (excluding
	 * _id)
	 * @param oldCols the current column names whose data should be kept in
	 * the altered table (excluding _id)
	 * @param oldIdxs the current indices
	 * @param newCols the new column names (excluding _id)
	 * @param newIdxs the new indices
	 * @throws DatabaseException if a database error occurs
	 */
	private void alterTableColumns(String table, Map<String,String> colTypes,
			List<String> oldCols, List<DatabaseIndex> oldIdxs,
			List<String> newCols, List<DatabaseIndex> newIdxs)
			throws DatabaseException {
		for (DatabaseIndex oldIdx : oldIdxs) {
			dropIndex(table, oldIdx.getName());
		}
		queryRunner.renameTable(table, Database.TABLE_TOKEN_SEP + table);
		String createSql = "CREATE TABLE " + escapeName(table) + " (" +
				escapeName("_id") + " " +
				getColumnSqlType(DatabaseType.STRING) + " PRIMARY KEY";
		String newQueryCols = escapeName("_id");
		String oldQueryCols = escapeName("_id");
		for (int i = 0; i < newCols.size(); i++) {
			String oldCol = oldCols.get(i);
			String newCol = newCols.get(i);
			createSql += ", " + escapeName(newCol) + " " + colTypes.get(oldCol);
			oldQueryCols += ", " + escapeName(oldCol);
			newQueryCols += ", " + escapeName(newCol);
		}
		createSql += ")";
		queryRunner.execSQL(createSql);
		for (DatabaseIndex newIdx : newIdxs) {
			createIndex(table, newIdx);
		}
		String copySql = "INSERT INTO " + escapeName(table) + " (" +
				newQueryCols + ") " +
				"SELECT " + oldQueryCols + " FROM " +
				escapeName(Database.TABLE_TOKEN_SEP + table);
		queryRunner.execSQL(copySql);
		queryRunner.execSQL("DROP TABLE " + escapeName(
				Database.TABLE_TOKEN_SEP + table));
	}

	@Override
	protected void dropDbTable(String table) throws DatabaseException {
		queryRunner.execSQL("DROP TABLE " + escapeName(table));
	}

	@Override
	public void beginTransaction() throws DatabaseException {
		queryRunner.beginTransaction();
	}

	@Override
	public void commitTransaction() throws DatabaseException {
		queryRunner.commitTransaction();
	}

	@Override
	protected void doInsertMaps(String table, List<Map<String,Object>> values)
			throws DatabaseException {
		List<String> ids = insertMapsGetIds(table, values);
		Iterator<String> idIt = ids.iterator();
		Iterator<Map<String,Object>> valueIt = values.iterator();
		while (idIt.hasNext()) {
			valueIt.next().put("id", idIt.next());
		}
	}

	/**
	 * Inserts a list of object maps. This method is also called for nested
	 * objects. For the main object, this method is called from {@link
	 * #insertMaps(String, java.util.List) insertMaps()} and the maps may
	 * contain key "id". For nested objects there should not be a key "id". If
	 * no ID is specified, this method generates one. The IDs will be returned.
	 *
	 * @param table the table name (lower case)
	 * @param maps the object maps
	 * @return the IDs
	 * @throws DatabaseException if a database error occurs
	 */
	private List<String> insertMapsGetIds(String table,
			List<? extends Map<?,?>> maps) throws DatabaseException {
		List<String> ids = new ArrayList<>();
		if (maps.isEmpty())
			return ids;
		List<Map<String,Object>> simpleContentList = new ArrayList<>();
		for (Map<?,?> map : maps) {
			Map<String,Object> content = processInsertMap(map);
			simpleContentList.add(content);
			ids.add((String)content.get("_id"));
		}
		queryRunner.insert(table, simpleContentList);
		return ids;
	}
	
	/**
	 * Processes one object map to be inserted. This method is called from
	 * {@link #insertMaps(String, java.util.List) insertMaps()} and the map may
	 * contain key "id". If no ID is specified, this method generates one. The
	 * ID will be returned under key "_id".
	 *
	 * @param map the object map (with optional key "id")
	 * @return the content (with key "_id")
	 * @throws DatabaseException if a database error occurs
	 */
	private Map<String,Object> processInsertMap(Map<?,?> map)
			throws DatabaseException {
		String id = null;
		Map<String,Object> content = new LinkedHashMap<>();
		if (map.containsKey("id"))
			id = (String)map.get("id");
		if (id == null)
			id = generateId();
		content.put("_id", id);
		for (Object keyObj : map.keySet()) {
			if (keyObj.equals("id"))
				continue;
			String key = (String)keyObj;
			Object val = map.get(key);
			content.put(key, val);
		}
		return content;
	}

	/**
	 * Generates a random UUID for a database record.
	 *
	 * @return the ID
	 */
	private String generateId() {
		return UUID.randomUUID().toString().toLowerCase().replaceAll("-", "");
	}

	@Override
	protected List<Map<String,?>> doSelectMaps(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria, int limit, DatabaseSort[] sort)
			throws DatabaseException {
		return selectRecords(table, dataClass, criteria, limit, sort);
	}

	/**
	 * Selects records from the specified table. Each record is returned as
	 * a map. The specified record reader defines how the map is created.
	 *
	 * @param table the table name (lower case)
	 * @param criteria the criteria for the records to return. This can be
	 * null.
	 * @param limit the maximum number of records to return. Set this to 0 or
	 * less to get all records.
	 * @param sort the order in which the records are returned. This can be
	 * null or an empty array if no sorting is needed.
	 * @return the records (keys may be in lower case)
	 * @throws DatabaseException if a database error occurs
	 */
	private List<Map<String,?>> selectRecords(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria, int limit, DatabaseSort[] sort)
			throws DatabaseException {
		LinkedHashMap<String,String> columns = getTableColumns(table,
				dataClass);
		SQLWhereBuilder whereBuilder = new SQLWhereBuilder(this, table,
				dataClass, criteria);
		StringBuilder orderBy = null;
		if (sort != null && sort.length > 0) {
			orderBy = new StringBuilder();
			for (DatabaseSort sortItem : sort) {
				if (orderBy.length() > 0)
					orderBy.append(", ");
				String sortCol = sortItem.getColumn();
				if (sortCol.equals("id"))
					sortCol = "_id";
				orderBy.append(getCompareColumn(sortCol, columns.get(sortCol)));
				orderBy.append(" ");
				orderBy.append(sortItem.isAscending() ? "ASC" : "DESC");
			}
		}
		List<Map<String,?>> result = new ArrayList<>();
		SQLCursor cursor = queryRunner.query(table,
				columns.keySet().toArray(new String[0]),
				whereBuilder.getWhere(), whereBuilder.getArgs(),
				null, null, orderBy == null ? null : orderBy.toString(),
				limit <= 0 ? null : Integer.toString(limit));
		try {
			boolean hasMore = cursor.moveToNext();
			while (hasMore && (limit <= 0 || result.size() < limit)) {
				result.add(readMap(cursor, table, columns));
				hasMore = cursor.moveToNext();
			}
			return result;
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Returns the specification of a column in the context of a comparison.
	 * This can be in an order by clause or in a comparison expression. The
	 * default implementation returns {@link #escapeName(String)
	 * escapeName(column)}. Databases that ignore case and diacritics by
	 * default when comparing string values, should return a specification
	 * that doesn't ignore case and diacritics if the column has a text type.
	 * 
	 * @param column the column name
	 * @param sqlType the SQL type of the column
	 * @return the column specification
	 */
	protected String getCompareColumn(String column, String sqlType) {
		return escapeName(column);
	}

	/**
	 * Reads the map for a database object from the current position of the
	 * cursor. The keys in the map may be in lower case and not exactly match
	 * the field names in the {@link DatabaseObject DatabaseObject} class (the
	 * class is unknown at this stage).
	 *
	 * @param cursor the cursor
	 * @param table the table name (lower case)
	 * @param columns map from column names (including "_id") to SQL types
	 * @return the map (including "id")
	 * @throws DatabaseException if a database error occurs
	 */
	private Map<String,?> readMap(SQLCursor cursor, String table,
			LinkedHashMap<String,String> columns) throws DatabaseException {
		Map<String,Object> result = new LinkedHashMap<>();
		List<String> colNames = new ArrayList<>(columns.keySet());
		for (int i = 0; i < colNames.size(); i++) {
			String column = colNames.get(i);
			Object val;
			try {
				val = cursor.getValue(i + 1, columns.get(column));
			} catch (DatabaseException ex) {
				throw new DatabaseException(String.format(
						"Failed to read value from database %s table %s, column %s, type %s",
						getName(), table, column, columns.get(column)) +
						": " + ex.getMessage(), ex);
			}
			if (column.equals("_id"))
				result.put("id", val);
			else
				result.put(column, val);
		}
		return result;
	}

	@Override
	protected int doCount(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria) throws DatabaseException {
		SQLWhereBuilder whereBuilder = new SQLWhereBuilder(this, table,
				dataClass, criteria);
		return queryRunner.count(table, new String[]{"_id"},
				whereBuilder.getWhere(), whereBuilder.getArgs());
	}

	@Override
	protected void doUpdate(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria, Map<String, ?> values)
			throws DatabaseException {
		if (!values.isEmpty()) {
			SQLWhereBuilder whereBuilder = new SQLWhereBuilder(this, table,
					dataClass, criteria);
			queryRunner.update(table, values, whereBuilder.getWhere(),
					whereBuilder.getArgs());
		}
	}

	@Override
	protected void doDelete(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria) throws DatabaseException {
		SQLWhereBuilder whereBuilder = new SQLWhereBuilder(this, table,
				dataClass, criteria);
		queryRunner.delete(table, whereBuilder.getWhere(),
				whereBuilder.getArgs());
	}

	@Override
	protected List<? extends Map<String,?>> selectLogRecords(String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria) throws DatabaseException {
		SQLWhereBuilder whereBuilder = new SQLWhereBuilder(this, table,
				dataClass, criteria);
		Map<String,String> tableCols = getTableColumns(table, dataClass);
		List<String> selCols = new ArrayList<>();
		selCols.add("_id");
		if (tableCols.containsKey("user"))
			selCols.add("user");
		if (tableCols.containsKey("utcTime"))
			selCols.add("utcTime");
		if (tableCols.containsKey("localTime"))
			selCols.add("localTime");
		String[] selColArray = selCols.toArray(new String[0]);
		List<Map<String,?>> records = new ArrayList<>();
		SQLCursor cursor = queryRunner.query(table, selColArray,
				whereBuilder.getWhere(), whereBuilder.getArgs(), null, null,
				null, null);
		try {
			boolean hasMore = cursor.moveToNext();
			while (hasMore) {
				Map<String,Object> record = new LinkedHashMap<>();
				record.put("id", cursor.getString(1));
				if (tableCols.containsKey("user")) {
					record.put("user", cursor.getValue(
							selCols.indexOf("user") + 1,
							tableCols.get("user")));
				}
				if (tableCols.containsKey("utcTime")) {
					record.put("utcTime", cursor.getValue(
							selCols.indexOf("utcTime") + 1,
							tableCols.get("utcTime")));
				}
				if (tableCols.containsKey("localTime")) {
					record.put("localTime", cursor.getValue(
							selCols.indexOf("localTime") + 1,
							tableCols.get("localTime")));
				}
				records.add(record);
				hasMore = cursor.moveToNext();
			}
		} finally {
			cursor.close();
		}
		return records;
	}

	/**
	 * Returns all columns in the specified table as a map from column name
	 * to SQL type.
	 *
	 * <p>If a data class is specified, the columns can be taken from that
	 * class. Otherwise they should be requested from the database, which is
	 * less efficient.</p>
	 *
	 * @param table the table name (lower case)
	 * @param dataClass the data class or null
	 * @return the table columns
	 * @throws DatabaseException if a database error occurs
	 */
	protected LinkedHashMap<String,String> getTableColumns(String table,
			Class<? extends DatabaseObject> dataClass)
			throws DatabaseException {
		if (dataClass == null)
			return queryRunner.getTableColumns(table);
		List<DatabaseFieldSpec> fields = DatabaseFieldScanner.getDatabaseFields(
				dataClass);
		LinkedHashMap<String,String> result = new LinkedHashMap<>();
		result.put("_id", getColumnSqlType(DatabaseType.STRING));
		for (DatabaseFieldSpec field : fields) {
			result.put(field.getPropSpec().getName(),
					getColumnSqlType(field.getDbField().value()));
		}
		return result;
	}
}
