package nl.rrd.senseeact.dao.mariadb;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.DatabaseFactory;
import nl.rrd.utils.AppComponent;

/**
 * Database factory for MariaDB. It has the following attributes:
 *
 * <p><ul>
 * <li>host: host name of the MariaDB server (default: localhost)</li>
 * <li>port: port number of the MariaDB server (default: 3306)</li>
 * <li>user: the user name</li>
 * <li>password: the password</li>
 * </ul></p>
 *
 * <p>You must set at least the user and password.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
@AppComponent
public class MariaDBDatabaseFactory extends DatabaseFactory {
	private String host = "localhost";
	private int port = 3306;
	private String user = null;
	private String password = null;

	public MariaDBDatabaseFactory() {
		try {
			Class.forName("org.mariadb.jdbc.Driver").getConstructor()
					.newInstance();
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(
					"MariaDB driver not found on the classpath: " +
					ex.getMessage(), ex);
		} catch (NoSuchMethodException ex) {
			throw new RuntimeException("Can't instantiate MariaDB driver: " +
					ex.getMessage(), ex);
		} catch (InvocationTargetException ex) {
			throw new RuntimeException("Can't instantiate MariaDB driver: " +
					ex.getCause().getMessage(), ex.getCause());
		} catch (IllegalAccessException ex) {
			throw new RuntimeException("Can't instantiate MariaDB driver: " +
					ex.getMessage(), ex);
		} catch (InstantiationException ex) {
			throw new RuntimeException("Can't instantiate MariaDB driver: " +
					ex.getMessage(), ex);
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	protected DatabaseConnection doConnect() throws IOException {
		return new MariaDBDatabaseConnection(host, port, user, password);
	}
}
