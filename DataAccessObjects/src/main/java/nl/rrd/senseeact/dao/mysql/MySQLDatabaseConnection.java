package nl.rrd.senseeact.dao.mysql;

import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.sql.SQLCursor;
import nl.rrd.senseeact.dao.sql.SQLQueryRunner;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.DatabaseException;

/**
 * Implementation of {@link DatabaseConnection DatabaseConnection} for MySQL.
 *
 * @author Dennis Hofs (RRD)
 */
public class MySQLDatabaseConnection extends DatabaseConnection {
	private static final int CHECK_VALID_TIMEOUT = 300; // seconds

	private static final Object LOCK = new Object();
	private static Map<String,List<OpenConnection>> globalOpenConns =
			new HashMap<>();

	private String host;
	private int port;
	private String user;
	private String password;

	private boolean closed = false;
	private List<OpenConnection> localOpenConns = new ArrayList<>();

	/**
	 * Constructs a new connection.
	 *
	 * @param host the host name of the MySQL server
	 * @param port the port number of the MySQL server (default: 3306)
	 * @param user the user name
	 * @param password the password
	 */
	public MySQLDatabaseConnection(String host, int port, String user,
			String password) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
	}

	@Override
	protected boolean databaseExists(String name) throws DatabaseException {
		Connection conn = openConnection(null);
		MySQLQueryRunner queryRunner = new MySQLQueryRunner(conn);
		SQLCursor cursor = queryRunner.rawQuery("SHOW DATABASES", null);
		try {
			boolean hasMore = cursor.moveToNext();
			while (hasMore) {
				String database = cursor.getString(1);
				if (database.equals(name))
					return true;
				hasMore = cursor.moveToNext();
			}
			return false;
		} finally {
			cursor.close();
		}
	}

	@Override
	protected Database createDatabase(String name) throws DatabaseException {
		Connection conn = openConnection(null);
		MySQLQueryRunner queryRunner = new MySQLQueryRunner(conn);
		queryRunner.execSQL("CREATE DATABASE `" + name +
				"` CHARACTER SET = utf8 COLLATE = utf8_general_ci");
		return getDatabase(name);
	}

	@Override
	protected Database doGetDatabase(String name) throws DatabaseException {
		Connection conn = openConnection(name);
		return new MySQLDatabase(name, conn);
	}

	/**
	 * Connects to the MySQL server. If a database is specified, it will be
	 * selected.
	 *
	 * @param database the database or null
	 * @return the connection
	 * @throws DatabaseException if the connection can't be established
	 */
	private Connection openConnection(String database)
			throws DatabaseException {
		OpenConnection openConn;
		synchronized (LOCK) {
			if (closed)
				throw new DatabaseException("Database connection closed");
			openConn = findMatchingOpenConnection(database);
		}
		try {
			if (openConn != null && openConn.connection.isValid(
					CHECK_VALID_TIMEOUT)) {
				return openConn.connection;
			}
		} catch (SQLException ex) {}
		synchronized (LOCK) {
			if (closed)
				throw new DatabaseException("Database connection closed");
			if (openConn != null) {
				// close open connection that is no longer valid
				closeOpenConnection(openConn);
			}
		}
		String url = "jdbc:mysql://" + host + ":" + port + "/";
		if (database != null)
			url += database;
		try {
			url += "?user=" + URLEncoder.encode(user, "UTF-8") +
					"&password=" + URLEncoder.encode(password, "UTF-8") +
					"&useSSL=false";
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
		try {
			openConn = new OpenConnection(database,
					DriverManager.getConnection(url));
			openConn = tryAddNewConnection(openConn);
		} catch (SQLException ex) {
			throw new DatabaseException("MySQL connection failed: " +
					ex.getMessage(), ex);
		}
		MySQLQueryRunner queryRunner = new MySQLQueryRunner(
				openConn.connection);
		queryRunner.execSQL("SET NAMES 'utf8mb4'");
		return openConn.connection;
	}

	@Override
	protected void doDropDatabase(String name) throws DatabaseException {
		Connection conn = openConnection(null);
		SQLQueryRunner queryRunner = new MySQLQueryRunner(conn);
		queryRunner.execSQL("DROP DATABASE IF EXISTS `" + name + "`");
	}

	@Override
	public void close() {
		synchronized (LOCK) {
			if (closed)
				return;
			closed = true;
			List<OpenConnection> openConns = new ArrayList<>(localOpenConns);
			for (OpenConnection openConn : openConns) {
				closeOpenConnection(openConn);
			}
		}
	}

	private OpenConnection findMatchingOpenConnection(String database) {
		for (OpenConnection openConn : localOpenConns) {
			if (equalsNullableString(database, openConn.database))
				return openConn;
		}
		return null;
	}

	private OpenConnection tryAddNewConnection(OpenConnection openConn)
			throws DatabaseException {
		synchronized (LOCK) {
			if (closed) {
				closeConnection(openConn.connection);
				throw new DatabaseException("Database connection closed");
			}
			OpenConnection other = findMatchingOpenConnection(
					openConn.database);
			if (other != null) {
				closeConnection(openConn.connection);
				return other;
			}
			addOpenConnection(openConn);
			return openConn;
		}
	}

	private void closeConnection(Connection conn) {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		try {
			conn.close();
		} catch (SQLException ex) {
			logger.error("Can't close MySQL connection: " + ex.getMessage(),
					ex);
		}
	}

	private void addOpenConnection(OpenConnection openConn) {
		localOpenConns.add(openConn);
		List<OpenConnection> connList = globalOpenConns.get(openConn.database);
		if (connList == null) {
			connList = new ArrayList<>();
			globalOpenConns.put(openConn.database, connList);
		}
		connList.add(openConn);
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("Opened MySQL connection for database " +
				openConn.database + "; open connections: " +
				logOpenDatabases());
	}

	private void closeOpenConnection(OpenConnection openConn) {
		closeConnection(openConn.connection);
		localOpenConns.remove(openConn);
		List<OpenConnection> connList = globalOpenConns.get(openConn.database);
		if (connList != null) {
			connList.remove(openConn);
			if (connList.isEmpty())
				globalOpenConns.remove(openConn.database);
		}
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info("Closed MySQL connection for database " +
				openConn.database + "; open connections: " +
				logOpenDatabases());
	}

	private String logOpenDatabases() {
		StringBuilder builder = new StringBuilder();
		List<String> databases = new ArrayList<>(globalOpenConns.keySet());
		databases.sort(this::compareNullableString);
		for (String database : databases) {
			if (builder.length() > 0)
				builder.append(", ");
			builder.append(database);
			builder.append(" (");
			builder.append(globalOpenConns.get(database).size());
			builder.append(")");
		}
		return "[" + builder + "]";
	}

	private int compareNullableString(String o1, String o2) {
		if (o1 == null && o2 == null)
			return 0;
		else if (o1 == null)
			return -1;
		else if (o2 == null)
			return 1;
		else
			return o1.compareTo(o2);
	}

	private boolean equalsNullableString(String o1, String o2) {
		if ((o1 == null) != (o2 == null))
			return false;
		if (o1 != null && !o1.equals(o2))
			return false;
		return true;
	}
	
	private static class OpenConnection {
		public String database;
		public Connection connection;

		public OpenConnection(String database, Connection connection) {
			this.database = database;
			this.connection = connection;
		}
	}
}
