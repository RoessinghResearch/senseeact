package nl.rrd.senseeact.dao.mariadb;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.sql.SQLCursor;
import nl.rrd.senseeact.dao.sql.SQLQueryRunner;

/**
 * This class wraps around a {@link MariaDBDatabase MariaDBDatabase} and
 * provides functions to run raw queries instead of using the {@link Database
 * Database} class. This can be used for any MariaDB database, including those
 * that don't contain the special metadata tables for the {@link Database
 * Database} class.
 * 
 * <p>When you get a {@link MariaDBDatabaseConnection
 * MariaDBDatabaseConnection}, call {@link
 * DatabaseConnection#getDatabase(String) getDatabase()} on it, cast the result
 * to a {@link MariaDBDatabase MariaDBDatabase} and pass it to the constructor
 * of this class.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class MariaDBRaw {
	private MariaDBDatabase database;
	private SQLQueryRunner queryRunner;
	
	/**
	 * Constructs a new instance.
	 * 
	 * @param database the MariaDB database
	 */
	public MariaDBRaw(MariaDBDatabase database) {
		this.database = database;
		this.queryRunner = database.getQueryRunner();
	}
	
	/**
	 * Returns the database.
	 * 
	 * @return the database
	 */
	public MariaDBDatabase getDatabase() {
		return database;
	}
	
	/**
	 * Returns the query runner.
	 * 
	 * @return the query runner
	 */
	public SQLQueryRunner getQueryRunner() {
		return queryRunner;
	}
	
	/**
	 * Runs a query that returns an integer in the first column. This method
	 * returns the value in the first row. If the result is empty, then this
	 * method returns null.
	 * 
	 * @param sql the SQL query (may contain ? placeholders)
	 * @param args values to write as escaped strings for the ? placeholders
	 * in the query (may be null)
	 * @return the integer or null
	 * @throws DatabaseException if a database error occurs
	 */
	public Integer queryInt(String sql, String[] args)
			throws DatabaseException {
		SQLCursor cursor = queryRunner.rawQuery(sql, args);
		try {
			return queryInt(cursor);
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Runs a query that returns an integer in the first column. This method
	 * returns the value in the first row. If the result is empty, then this
	 * method returns null.
	 * 
	 * @param table the table name
	 * @param columns the columns to select
	 * @param whereClause the where clause (may contain ? placeholders) or null
	 * @param whereArgs values to write as escaped strings for the ?
	 * placeholders in the where clause (may be null)
	 * @param groupBy the group by clause or null
	 * @param having the having clause or null
	 * @param orderBy the order by clause or null
	 * @param limit the limit clause or null
	 * @return the integer or null
	 * @throws DatabaseException if a database error occurs
	 */
	public Integer queryInt(String table, String[] columns, String whereClause,
			String[] whereArgs, String groupBy, String having, String orderBy,
			String limit) throws DatabaseException {
		SQLCursor cursor = queryRunner.query(table, columns, whereClause,
				whereArgs, groupBy, having, orderBy, limit);
		try {
			return queryInt(cursor);
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Reads the result of a query that returned an integer in the first column.
	 * This method returns the value in the next row. If the result is empty,
	 * then this method returns null.
	 * 
	 * @param cursor the cursor
	 * @return the integer or null
	 * @throws DatabaseException if a database error occurs
	 */
	public Integer queryInt(SQLCursor cursor) throws DatabaseException {
		if (cursor.moveToNext())
			return cursor.getInt(1);
		else
			return null;
	}
	
	/**
	 * Runs a query that returns a result with integers in the first column.
	 * 
	 * @param sql the SQL query (may contain ? placeholders)
	 * @param args values to write as escaped strings for the ? placeholders
	 * in the query (may be null)
	 * @return the integer list
	 * @throws DatabaseException if a database error occurs
	 */
	public List<Integer> queryIntList(String sql, String[] args)
			throws DatabaseException {
		SQLCursor cursor = queryRunner.rawQuery(sql, args);
		try {
			return queryIntList(cursor);
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Runs a query that returns a result with integers in the first column.
	 * 
	 * @param table the table name
	 * @param columns the columns to select
	 * @param whereClause the where clause (may contain ? placeholders) or null
	 * @param whereArgs values to write as escaped strings for the ?
	 * placeholders in the where clause (may be null)
	 * @param groupBy the group by clause or null
	 * @param having the having clause or null
	 * @param orderBy the order by clause or null
	 * @param limit the limit clause or null
	 * @return the integer list
	 * @throws DatabaseException if a database error occurs
	 */
	public List<Integer> queryIntList(String table, String[] columns,
			String whereClause, String[] whereArgs, String groupBy,
			String having, String orderBy, String limit)
			throws DatabaseException {
		SQLCursor cursor = queryRunner.query(table, columns, whereClause,
				whereArgs, groupBy, having, orderBy, limit);
		try {
			return queryIntList(cursor);
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Reads the result of a query that returned integers in the first column.
	 * 
	 * @param cursor the cursor
	 * @return the integer list
	 * @throws DatabaseException if a database error occurs
	 */
	public List<Integer> queryIntList(SQLCursor cursor)
			throws DatabaseException {
		List<Integer> result = new ArrayList<>();
		boolean hasMore = cursor.moveToNext();
		while (hasMore) {
			result.add(cursor.getInt(1));
			hasMore = cursor.moveToNext();
		}
		return result;
	}
	
	/**
	 * Runs a query that returns a string in the first column. This method
	 * returns the value in the first row. If the result is empty, then this
	 * method returns null.
	 * 
	 * @param sql the SQL query (may contain ? placeholders)
	 * @param args values to write as escaped strings for the ? placeholders
	 * in the query (may be null)
	 * @return the string or null
	 * @throws DatabaseException if a database error occurs
	 */
	public String queryString(String sql, String[] args)
			throws DatabaseException {
		SQLCursor cursor = queryRunner.rawQuery(sql, args);
		try {
			return queryString(cursor);
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Runs a query that returns a string in the first column. This method
	 * returns the value in the first row. If the result is empty, then this
	 * method returns null.
	 * 
	 * @param table the table name
	 * @param columns the columns to select
	 * @param whereClause the where clause (may contain ? placeholders) or null
	 * @param whereArgs values to write as escaped strings for the ?
	 * placeholders in the where clause (may be null)
	 * @param groupBy the group by clause or null
	 * @param having the having clause or null
	 * @param orderBy the order by clause or null
	 * @param limit the limit clause or null
	 * @return the string or null
	 * @throws DatabaseException if a database error occurs
	 */
	public String queryString(String table, String[] columns,
			String whereClause, String[] whereArgs, String groupBy,
			String having, String orderBy, String limit)
			throws DatabaseException {
		SQLCursor cursor = queryRunner.query(table, columns, whereClause,
				whereArgs, groupBy, having, orderBy, limit);
		try {
			return queryString(cursor);
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Reads the result of a query that returned a string in the first column.
	 * This method returns the value in the next row. If the result is empty,
	 * then this method returns null.
	 * 
	 * @param cursor the cursor
	 * @return the string or null
	 * @throws DatabaseException if a database error occurs
	 */
	public String queryString(SQLCursor cursor) throws DatabaseException {
		if (cursor.moveToNext())
			return cursor.getString(1);
		else
			return null;
	}
	
	/**
	 * Runs a query that returns a result with strings in the first column.
	 * 
	 * @param sql the SQL query (may contain ? placeholders)
	 * @param args values to write as escaped strings for the ? placeholders
	 * in the query (may be null)
	 * @return the string list
	 * @throws DatabaseException if a database error occurs
	 */
	public List<String> queryStringList(String sql, String[] args)
			throws DatabaseException {
		SQLCursor cursor = queryRunner.rawQuery(sql, args);
		try {
			return queryStringList(cursor);
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Runs a query that returns a result with strings in the first column.
	 * 
	 * @param table the table name
	 * @param columns the columns to select
	 * @param whereClause the where clause (may contain ? placeholders) or null
	 * @param whereArgs values to write as escaped strings for the ?
	 * placeholders in the where clause (may be null)
	 * @param groupBy the group by clause or null
	 * @param having the having clause or null
	 * @param orderBy the order by clause or null
	 * @param limit the limit clause or null
	 * @return the string list
	 * @throws DatabaseException if a database error occurs
	 */
	public List<String> queryStringList(String table, String[] columns,
			String whereClause, String[] whereArgs, String groupBy,
			String having, String orderBy, String limit)
			throws DatabaseException {
		SQLCursor cursor = queryRunner.query(table, columns, whereClause,
				whereArgs, groupBy, having, orderBy, limit);
		try {
			return queryStringList(cursor);
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Reads the result of a query that returned strings in the first column.
	 * 
	 * @param cursor the cursor
	 * @return the string list
	 * @throws DatabaseException if a database error occurs
	 */
	public List<String> queryStringList(SQLCursor cursor)
			throws DatabaseException {
		List<String> result = new ArrayList<>();
		boolean hasMore = cursor.moveToNext();
		while (hasMore) {
			result.add(cursor.getString(1));
			hasMore = cursor.moveToNext();
		}
		return result;
	}
}
