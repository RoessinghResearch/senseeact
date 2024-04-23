package nl.rrd.senseeact.dao.mariadb;

import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.DatabaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MariaDBDatabaseTest {
	private DatabaseConnection dbConn = null;
	private DatabaseTest dbTest = null;

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
		dbFactory.setHost(host);
		dbFactory.setPort(port);
		dbFactory.setUser(user);
		dbFactory.setPassword(password);
		dbConn = dbFactory.connect();
		dbConn.dropDatabase(dbName);
		dbTest = new DatabaseTest(dbConn, dbName);
	}

	@Test
	public void testInsertSelect() throws Exception {
		if (dbTest == null)
			return;
		dbTest.testInsertSelect();
	}

	@Test
	public void testInsertSelectSplitByUser() throws Exception {
		if (dbTest == null)
			return;
		dbTest.testInsertSelectSplitByUser();
	}

	@Test
	public void testInsertSelectResource() throws Exception {
		if (dbTest == null)
			return;
		dbTest.testInsertSelectResource();
	}

	@Test
	public void testUpdateDelete() throws Exception {
		if (dbTest == null)
			return;
		dbTest.testUpdateDelete();
	}

	@Test
	public void testUpdateDeleteSplitByUser() throws Exception {
		if (dbTest == null)
			return;
		dbTest.testUpdateDeleteSplitByUser();
	}

	@Test
	public void testUpdateDeleteResource() throws Exception {
		if (dbTest == null)
			return;
		dbTest.testUpdateDeleteResource();
	}

	@Test
	public void testSelectQuery() throws Exception {
		if (dbTest == null)
			return;
		dbTest.testSelectQuery();
	}

	@Test
	public void testSelectQuerySplitByUser() throws Exception {
		if (dbTest == null)
			return;
		dbTest.testSelectQuerySplitByUser();
	}

	@After
	public void cleanup() throws Exception {
		if (dbConn != null)
			dbConn.close();
	}
}
