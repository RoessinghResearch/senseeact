package nl.rrd.senseeact.dao.mysql;

import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.sql.SQLCursor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Implementation of {@link SQLCursor SQLCursor} for MySQL.
 *
 * @author Dennis Hofs (RRD)
 */
public class MySQLCursor implements SQLCursor {
	private Statement statement;
	private ResultSet resultSet;

	/**
	 * Constructs a new MySQL cursor.
	 *
	 * @param statement the statement from which the result set was obtained
	 * @param resultSet the result set
	 */
	public MySQLCursor(Statement statement, ResultSet resultSet) {
		this.statement = statement;
		this.resultSet = resultSet;
	}

	@Override
	public boolean moveToNext() throws DatabaseException {
		try {
			return resultSet.next();
		} catch (SQLException ex) {
			throw new DatabaseException("Can't read SQL result: " +
					ex.getMessage(), ex);
		}
	}

	@Override
	public int getColumnIndex(String column) throws DatabaseException {
		try {
			return resultSet.findColumn(column);
		} catch (SQLException ex) {
			throw new DatabaseException("Can't read SQL result: " +
					ex.getMessage(), ex);
		}
	}

	@Override
	public String getString(int column) throws DatabaseException {
		try {
			return resultSet.getString(column);
		} catch (SQLException ex) {
			throw new DatabaseException("Can't read SQL result: " +
					ex.getMessage(), ex);
		}
	}

	@Override
	public Integer getInt(int column) throws DatabaseException {
		try {
			Object obj = resultSet.getObject(column);
			if (obj == null)
				return null;
			else
				return resultSet.getInt(column);
		} catch (SQLException ex) {
			throw new DatabaseException("Can't read SQL result: " +
					ex.getMessage(), ex);
		}
	}

	@Override
	public Object getValue(int column, String type) throws DatabaseException {
		try {
			// getObject can result in an error with datetime columns if the
			// value is a local time that does not exist in the current timezone
			// because of DST
			Object obj = resultSet.getString(column);
			if (obj == null)
				return null;
			type = type.toLowerCase();
			if (type.startsWith("tinyint") || type.startsWith("smallint") ||
					type.startsWith("mediumint") || type.startsWith("int")) {
				return resultSet.getInt(column);
			} else if (type.startsWith("bigint")) {
				return resultSet.getLong(column);
			} else if (type.startsWith("float") || type.startsWith("real") ||
					type.startsWith("double")) {
				return resultSet.getDouble(column);
			} else if (type.startsWith("datetime")) {
				String val = resultSet.getString(column);
				if (val == null)
					return null;
				return val.replaceAll("\\..*$", "");
			} else {
				return resultSet.getString(column);
			}
		} catch (SQLException ex) {
			throw new DatabaseException("Can't read SQL result: " +
					ex.getMessage(), ex);
		}
	}

	@Override
	public void close() {
		try {
			resultSet.close();
		} catch (SQLException ex) {
			System.err.println("Can't close ResultSet: " + ex.getMessage());
			ex.printStackTrace();
		}
		try {
			statement.close();
		} catch (SQLException ex) {
			System.err.println("Can't close Statement: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
