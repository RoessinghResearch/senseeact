package nl.rrd.senseeact.dao.sql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.rrd.utils.exception.DatabaseException;

/**
 * Interface for classes that can run basic SQL queries (for example SQLite or
 * MariaDB). You can run raw SQL queries with {@link #execSQL(String) execSQL()}
 * and {@link #rawQuery(String, String[]) rawQuery()}, but you should use the
 * more specific methods when possible. The choice of methods were inspired by
 * the Android SQLite API, and methods where the actual SQL query is different
 * in SQLite than in MariaDB.
 *
 * @author Dennis Hofs (RRD)
 */
public interface SQLQueryRunner {

	/**
	 * Runs a raw SQL query that doesn't return results. Normally you should use
	 * one of the more specific methods, such as {@link #insert(String, List)
	 * insert()}, {@link #update(String, Map, String, String[]) update()} and
	 * {@link #delete(String, String, String[]) delete()}. For a raw query that
	 * returns results, see {@link #rawQuery(String, String[]) rawQuery()}.
	 *
	 * @param sql the SQL query
	 * @throws DatabaseException if a database error occurs
	 */
	void execSQL(String sql) throws DatabaseException;

	/**
	 * Runs a raw SQL select query. Normally you should use a more specific
	 * method such as {@link
	 * #query(String, String[], String, String[], String, String, String, String)
	 * query()}. For a raw query that doesn't return results, see {@link
	 * #execSQL(String) execSQL()}.
	 *
	 * @param sql the SQL query (may contain ? placeholders)
	 * @param args values to write as escaped strings for the ? placeholders
	 * in the query (may be null)
	 * @return a cursor to read the results
	 * @throws DatabaseException if a database error occurs
	 */
	SQLCursor rawQuery(String sql, String[] args) throws DatabaseException;

	/**
	 * Selects records from the specified table.
	 *
	 * @param table the table name (lower case)
	 * @param columns the columns to select
	 * @param whereClause the where clause (may contain ? placeholders) or null
	 * @param whereArgs values to write as escaped strings for the ?
	 * placeholders in the where clause (may be null)
	 * @param groupBy the group by clause or null
	 * @param having the having clause or null
	 * @param orderBy the order by clause or null
	 * @param limit the limit clause or null
	 * @return a cursor to read the records
	 * @throws DatabaseException if a database error occurs
	 */
	SQLCursor query(String table, String[] columns, String whereClause,
			String[] whereArgs, String groupBy, String having, String orderBy,
			String limit) throws DatabaseException;

	/**
	 * Returns the number of records that match the specified select query.
	 *
	 * @param table the table name (lower case)
	 * @param columns the columns to select
	 * @param whereClause the where clause (may contain ? placeholders) or null
	 * @param whereArgs values to write as escaped strings for the ?
	 * placeholders in the where clause (may be null)
	 * @return the number of records
	 * @throws DatabaseException if a database error occurs
	 */
	int count(String table, String[] columns, String whereClause,
			  String[] whereArgs) throws DatabaseException;

	/**
	 * Updates records in the specified table.
	 *
	 * @param table the table name (lower case)
	 * @param values the values to update as a map from column to value
	 * @param whereClause the where clause (may contain ? placeholders) or null
	 * @param whereArgs values to write as escaped strings for the ?
	 * placeholders in the where clause (may be null)
	 * @throws DatabaseException if a database error occurs
	 */
	void update(String table, Map<String,?> values, String whereClause,
			String[] whereArgs) throws DatabaseException;

	/**
	 * Deletes records from the specified table.
	 *
	 * @param table the table name (lower case)
	 * @param whereClause the where clause (may contain ? placeholders) or null
	 * @param whereArgs values to write as escaped strings for the ?
	 * placeholders in the where clause (may be null)
	 * @throws DatabaseException if a database error occurs
	 */
	void delete(String table, String whereClause, String[] whereArgs)
			throws DatabaseException;

	/**
	 * Inserts one or more records into the specified table. All records should
	 * have the same set of columns.
	 *
	 * @param table the table name (lower case)
	 * @param values list of maps from column name to value, including _id
	 * @throws DatabaseException if a database error occurs
	 */
	void insert(String table, List<? extends Map<String,?>> values)
			throws DatabaseException;

	/**
	 * Renames a table.
	 *
	 * @param oldName the old table name (lower case)
	 * @param newName the new table name (lower case)
	 * @throws DatabaseException if a database error occurs
	 */
	void renameTable(String oldName, String newName) throws DatabaseException;

	/**
	 * Returns all columns in the specified table as a map from column name
	 * to SQL type.
	 *
	 * @param table the table name (lower case)
	 * @return the table columns
	 * @throws DatabaseException if a database error occurs
	 */
	LinkedHashMap<String,String> getTableColumns(String table)
			throws DatabaseException;
	
	/**
	 * Begins a transaction. Currently there is no rollback functionality, but
	 * a transaction can be used to speed up a sequence of write queries. At
	 * the end call {@link #commitTransaction() commitTransaction()}.
	 * 
	 * @throws DatabaseException if a database error occurs
	 */
	void beginTransaction() throws DatabaseException;
	
	/**
	 * Commits a transaction. This should be called after {@link
	 * #beginTransaction() beginTransaction()}.
	 * 
	 * @throws DatabaseException if a database error occurs
	 */
	void commitTransaction() throws DatabaseException;
}
