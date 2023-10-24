package nl.rrd.senseeact.dao.mysql;

import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseIndex;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.senseeact.dao.sql.SQLCursor;
import nl.rrd.senseeact.dao.sql.SQLDatabase;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link SQLDatabase SQLDatabase} for MySQL.
 *
 * @author Dennis Hofs (RRD)
 */
public class MySQLDatabase extends SQLDatabase {

	/**
	 * Constructs a new instance.
	 *
	 * @param name the database name
	 * @param conn the MySQL connection
	 */
	public MySQLDatabase(String name, Connection conn) {
		super(name, new MySQLQueryRunner(conn));
	}

	@Override
	protected boolean isBacktickNames() {
		return true;
	}

	@Override
	protected String getColumnSqlType(DatabaseType type) {
		switch (type) {
			case BYTE:
				return "TINYINT";
			case SHORT:
				return "SMALLINT";
			case INT:
				return "INTEGER";
			case LONG:
				return "BIGINT";
			case FLOAT:
				return "FLOAT";
			case DOUBLE:
				return "DOUBLE";
			case STRING:
				return "VARCHAR(255)";
			case TEXT:
				return "LONGTEXT";
			case DATE:
				return "DATE";
			case TIME:
				return "TIME";
			case DATETIME:
				return "DATETIME";
			case ISOTIME:
				return "VARCHAR(255)";
			default:
				return null;
		}
	}

	@Override
	protected List<String> selectDbTables() throws DatabaseException {
		SQLCursor cursor = getQueryRunner().rawQuery("SHOW TABLES", null);
		try {
			List<String> tables = new ArrayList<>();
			boolean hasMore = cursor.moveToNext();
			while (hasMore) {
				tables.add(cursor.getString(1).toLowerCase());
				hasMore = cursor.moveToNext();
			}
			return tables;
		} finally {
			cursor.close();
		}
	}

	@Override
	protected List<DatabaseIndex> getIndices(String table)
			throws DatabaseException {
		SQLCursor cursor = getQueryRunner().rawQuery("SHOW INDEX FROM " +
			escapeName(table), null);
		try {
			List<DatabaseIndex> result = new ArrayList<>();
			String currIdxName = null;
			List<String> currFields = null;
			String prefix = table + ".";
			boolean hasMore = cursor.moveToNext();
			while (hasMore) {
				String idxName = cursor.getString(3);
				if (idxName.startsWith(prefix)) {
					idxName = idxName.substring(prefix.length());
					String field = cursor.getString(5);
					if (currIdxName == null) {
						currIdxName = idxName;
						currFields = new ArrayList<>();
					} else if (!currIdxName.equals(idxName)) {
						String[] currFieldArray = currFields.toArray(
								new String[0]);
						result.add(new DatabaseIndex(currIdxName,
								currFieldArray));
						currIdxName = idxName;
						currFields = new ArrayList<>();
					}
					currFields.add(field);
				}
				hasMore = cursor.moveToNext();
			}
			if (currIdxName != null) {
				String[] currFieldArray = currFields.toArray(new String[0]);
				result.add(new DatabaseIndex(currIdxName, currFieldArray));
			}
			return result;
		} finally {
			cursor.close();
		}
	}

	@Override
	public void dropIndex(String table, String name) throws DatabaseException {
		String sql = "DROP INDEX " + escapeName(table + "." + name) + " ON " +
				escapeName(table);
		getQueryRunner().execSQL(sql);
	}
}
