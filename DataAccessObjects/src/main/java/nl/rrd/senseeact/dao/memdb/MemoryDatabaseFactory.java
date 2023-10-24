package nl.rrd.senseeact.dao.memdb;

import java.io.IOException;

import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.DatabaseFactory;

/**
 * This database factory provides access to a single {@link
 * MemoryDatabaseConnection MemoryDatabaseConnection}.
 * 
 * @author Dennis Hofs (RRD)
 */
public class MemoryDatabaseFactory extends DatabaseFactory {
	private MemoryDatabaseConnection connection =
			new MemoryDatabaseConnection();

	@Override
	protected DatabaseConnection doConnect() throws IOException {
		return connection;
	}
}
