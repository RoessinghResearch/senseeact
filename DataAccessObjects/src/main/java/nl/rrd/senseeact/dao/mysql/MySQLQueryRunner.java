package nl.rrd.senseeact.dao.mysql;

import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.sql.SQLCursor;
import nl.rrd.senseeact.dao.sql.SQLQueryRunner;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Implementation of {@link SQLQueryRunner SQLQueryRunner} for MySQL.
 *
 * @author Dennis Hofs (RRD)
 */
public class MySQLQueryRunner implements SQLQueryRunner {
	// log SQL queries with durations
	private static boolean PROFILING_ENABLED = false;
	// log only SQL queries that take longer than PROFILING_THRESHOLD
	private static final int PROFILING_THRESHOLD = 100;
	private static final int MAX_LOG_LENGTH = 1000;

	private Connection conn;

	/**
	 * Constructs a new query runner.
	 *
	 * @param conn the MySQL connection
	 */
	public MySQLQueryRunner(Connection conn) {
		this.conn = conn;
	}

	@Override
	public void execSQL(String sql) throws DatabaseException {
		execSQL(sql, null);
	}

	/**
	 * The same as {@link #execSQL(String) execSQL()}, but you may specify a
	 * parameterized query.
	 *
	 * @param sql the SQL query (may contain ? placeholders)
	 * @param args values to write as escaped strings for the ? placeholders
	 * in the query (may be null)
	 * @throws DatabaseException if a database error occurs
	 */
	private void execSQL(String sql, String[] args) throws DatabaseException {
		long start = System.currentTimeMillis();
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					stmt.setString(i + 1, args[i]);
				}
			}
			stmt.execute();
			long end = System.currentTimeMillis();
			if (PROFILING_ENABLED && end - start > PROFILING_THRESHOLD) {
				Logger logger = AppComponents.getLogger(
						getClass().getSimpleName());
				String logSql = sql;
				if (logSql.length() > MAX_LOG_LENGTH)
					logSql = logSql.substring(0, MAX_LOG_LENGTH);
				String log = "MySQL query: " + (end - start) + " ms: " + logSql;
				if (args != null) {
					String logArgs = Arrays.asList(args).toString();
					if (logArgs.length() > MAX_LOG_LENGTH)
						logArgs = logArgs.substring(0, MAX_LOG_LENGTH);
					log += ": " + logArgs;
				}
				logger.info(log);
			}
		} catch (SQLException ex) {
			throw new DatabaseException("Can't execute SQL query: " +
					ex.getMessage(), ex);
		}
	}

	@Override
	public SQLCursor rawQuery(String sql, String[] args)
			throws DatabaseException {
		long start = System.currentTimeMillis();
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(sql);
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					stmt.setString(i + 1, args[i]);
				}
			}
			ResultSet resultSet = stmt.executeQuery();
			long end = System.currentTimeMillis();
			if (PROFILING_ENABLED && end - start > PROFILING_THRESHOLD) {
				Logger logger = AppComponents.getLogger(
						getClass().getSimpleName());
				String log = "MySQL query: " + (end - start) + " ms: " + sql;
				if (args != null)
					log += ": " + Arrays.asList(args);
				logger.info(log);
			}
			return new MySQLCursor(stmt, resultSet);
		} catch (SQLException ex) {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException ex2) {
				System.err.println("Can't close Statement: " +
						ex2.getMessage());
				ex.printStackTrace();
			}
			throw new DatabaseException("Can't execute SQL query: " +
					ex.getMessage(), ex);
		}
	}

	@Override
	public SQLCursor query(String table, String[] columns, String whereClause,
			String[] whereArgs, String groupBy, String having, String orderBy,
			String limit) throws DatabaseException {
		StringBuilder sql = new StringBuilder("SELECT ");
		for (int i = 0; i < columns.length; i++) {
			if (i > 0)
				sql.append(", ");
			sql.append("`");
			sql.append(columns[i]);
			sql.append("`");
		}
		sql.append(" FROM `");
		sql.append(table);
		sql.append("`");
		if (whereClause != null) {
			sql.append(" WHERE ");
			sql.append(whereClause);
		}
		if (groupBy != null) {
			sql.append(" GROUP BY ");
			sql.append(groupBy);
		}
		if (having != null) {
			sql.append(" HAVING ");
			sql.append(having);
		}
		if (orderBy != null) {
			sql.append(" ORDER BY ");
			sql.append(orderBy);
		}
		if (limit != null) {
			sql.append(" LIMIT " + limit);
		}
		return rawQuery(sql.toString(), whereArgs);
	}

	@Override
	public int count(String table, String[] columns, String whereClause,
			String[] whereArgs) throws DatabaseException {
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM `");
		sql.append(table);
		sql.append("`");
		if (whereClause != null) {
			sql.append(" WHERE ");
			sql.append(whereClause);
		}
		SQLCursor cursor = rawQuery(sql.toString(), whereArgs);
		try {
			if (!cursor.moveToNext())
				throw new DatabaseException("Count row not found");
			return cursor.getInt(1);
		} finally {
			cursor.close();
		}
	}

	@Override
	public void update(String table, Map<String, ?> values, String whereClause,
			String[] whereArgs) throws DatabaseException {
		StringBuilder sql = new StringBuilder("UPDATE `");
		sql.append(table);
		sql.append("` SET ");
		List<String> columns = new ArrayList<>(values.keySet());
		List<String> queryArgs = new ArrayList<>();
		for (int i = 0; i < columns.size(); i++) {
			String column = columns.get(i);
			if (i > 0)
				sql.append(", ");
			sql.append("`");
			sql.append(column);
			sql.append("` = ?");
			Object value = values.get(column);
			queryArgs.add(value == null ? null : value.toString());
		}
		if (whereClause != null) {
			sql.append(" WHERE ");
			sql.append(whereClause);
		}
		if (whereArgs != null)
			queryArgs.addAll(Arrays.asList(whereArgs));
		execSQL(sql.toString(),
				queryArgs.toArray(new String[0]));
	}

	@Override
	public void delete(String table, String whereClause, String[] whereArgs)
			throws DatabaseException {
		StringBuilder sql = new StringBuilder("DELETE FROM `");
		sql.append(table);
		sql.append("`");
		if (whereClause != null) {
			sql.append(" WHERE ");
			sql.append(whereClause);
		}
		execSQL(sql.toString(), whereArgs);
	}

	@Override
	public void insert(String table, List<? extends Map<String, ?>> values)
			throws DatabaseException {
		StringBuilder sql = new StringBuilder("INSERT INTO `");
		sql.append(table);
		sql.append("` (");
		List<String> columns = new ArrayList<>(values.get(0).keySet());
		for (int i = 0; i < columns.size(); i++) {
			String column = columns.get(i);
			if (i > 0)
				sql.append(", ");
			sql.append("`");
			sql.append(column);
			sql.append("`");
		}
		sql.append(") VALUES (");
		String[] args = new String[values.size() * columns.size()];
		int argIndex = 0;
		for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
			Map<String,?> map = values.get(valueIndex);
			if (valueIndex > 0)
				sql.append("), (");
			for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
				String column = columns.get(colIndex);
				Object value = map.get(column);
				if (colIndex > 0)
					sql.append(", ");
				sql.append("?");
				args[argIndex++] = value == null ? null : value.toString();
			}
		}
		sql.append(")");
		execSQL(sql.toString(), args);
	}

	@Override
	public void renameTable(String oldName, String newName)
			throws DatabaseException {
		execSQL("RENAME TABLE `" + oldName + "` TO `" + newName + "`");
	}

	@Override
	public LinkedHashMap<String,String> getTableColumns(String table)
			throws DatabaseException {
		LinkedHashMap<String,String> columns = new LinkedHashMap<>();
		SQLCursor cursor = rawQuery("SHOW FULL COLUMNS FROM `" + table + "`",
				null);
		try {
			boolean hasMore = cursor.moveToNext();
			while (hasMore) {
				String type = cursor.getString(2);
				String collate = cursor.getString(3);
				if (collate != null && collate.equalsIgnoreCase(
						"utf8mb4_general_ci")) {
					type += " CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci";
				}
				columns.put(cursor.getString(1), type);
				hasMore = cursor.moveToNext();
			}
		} finally {
			cursor.close();
		}
		return columns;
	}

	@Override
	public void beginTransaction() throws DatabaseException {
		execSQL("START TRANSACTION");
	}

	@Override
	public void commitTransaction() throws DatabaseException {
		execSQL("COMMIT");
	}
}
