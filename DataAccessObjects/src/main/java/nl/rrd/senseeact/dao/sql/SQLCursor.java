package nl.rrd.senseeact.dao.sql;

import nl.rrd.utils.exception.DatabaseException;

/**
 * An SQL cursor is used to read the results from an SQL select query.
 *
 * @author Dennis Hofs (RRD)
 */
public interface SQLCursor {

	/**
	 * Moves to the next selected record. If there are no more records, this
	 * method returns false.
	 *
	 * @return true if the cursor is at the next record, false if there are no
	 * more records
	 * @throws DatabaseException if a database error occurs
	 */
	boolean moveToNext() throws DatabaseException;

	/**
	 * Returns the index of the specified column. The first column has index 1.
	 *
	 * @param column the column name
	 * @return the column index
	 * @throws DatabaseException if a database error occurs
	 */
	int getColumnIndex(String column) throws DatabaseException;

	/**
	 * Returns the string value of the specified column. This may be null.
	 *
	 * @param column the column index (the first column has index 1)
	 * @return the string value or null
	 * @throws DatabaseException if a database error occurs
	 */
	String getString(int column) throws DatabaseException;

	/**
	 * Returns the integer value of the specified column. This may be null.
	 *
	 * @param column the column index (the first column has index 1)
	 * @return the integer value or null
	 * @throws DatabaseException if a database error occurs
	 */
	Integer getInt(int column) throws DatabaseException;

	/**
	 * Returns the value of the specified column. The result should be a
	 * primitive type (number, string, null) matching the specified SQL type.
	 *
	 * @param column the column index (the first column has index 1)
	 * @param type the SQL type of the column
	 * @return the value
	 * @throws DatabaseException if a database error occurs
	 */
	Object getValue(int column, String type) throws DatabaseException;

	/**
	 * Closes this cursor.
	 */
	void close();
}
