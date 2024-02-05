package nl.rrd.senseeact.dao.mariadb;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.sync.DatabaseSynchronizerTest;

public class MariaDBDatabaseSynchronizerTest {
	private DatabaseConnection serverDbConn = null;
	private DatabaseConnection client1DbConn = null;
	private DatabaseConnection client2DbConn = null;
	private DatabaseSynchronizerTest dbTest = null;

	@Before
	public void init() throws Exception {
		boolean runMysqlTest = Boolean.parseBoolean(System.getProperty(
				"testMysqlRun", "false"));
		if (!runMysqlTest)
			return;
		String host = System.getProperty("testMysqlHost", "localhost");
		int port = Integer.parseInt(System.getProperty(
				"testMysqlPort", "3306"));
		String user = System.getProperty("testMysqlUser");
		String password = System.getProperty("testMysqlPassword");
		String dbName = System.getProperty("testMysqlDatabase");
		MariaDBDatabaseFactory dbFactory = new MariaDBDatabaseFactory();
		dbFactory.setSyncEnabled(true);
		dbFactory.setHost(host);
		dbFactory.setPort(port);
		dbFactory.setUser(user);
		dbFactory.setPassword(password);
		serverDbConn = dbFactory.connect();
		String serverDbName = dbName + "_sync_server";
		client1DbConn = dbFactory.connect();
		String client1DbName = dbName + "_sync_client1";
		client2DbConn = dbFactory.connect();
		String client2DbName = dbName + "_sync_client2";
		dbTest = new DatabaseSynchronizerTest(serverDbConn, serverDbName,
				client1DbConn, client1DbName, client2DbConn, client2DbName);
	}

	@Test
	public void testSynchronizer() throws Exception {
		if (dbTest == null)
			return;
		dbTest.testSynchronizer();
	}

	@After
	public void cleanup() throws Exception {
		if (serverDbConn != null)
			serverDbConn.close();
		if (client1DbConn != null)
			client1DbConn.close();
		if (client2DbConn != null)
			client2DbConn.close();
	}
}
