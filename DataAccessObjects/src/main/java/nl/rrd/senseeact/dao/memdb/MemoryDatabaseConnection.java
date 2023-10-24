package nl.rrd.senseeact.dao.memdb;

import java.util.HashMap;
import java.util.Map;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.utils.exception.DatabaseException;

/**
 * Repository of {@link MemoryDatabase MemoryDatabase}s. All data is lost when
 * the application exits.
 * 
 * @author Dennis Hofs (RRD)
 */
public class MemoryDatabaseConnection extends DatabaseConnection {
	private Map<String,MemoryDatabase> databases =
			new HashMap<String,MemoryDatabase>();
	private final Object lock = new Object();

	@Override
	protected boolean databaseExists(String name) {
		synchronized (lock) {
			return databases.containsKey(name);
		}
	}

	@Override
	protected Database createDatabase(String name) throws DatabaseException {
		synchronized (lock) {
			MemoryDatabase db = new MemoryDatabase(name);
			databases.put(name, db);
			return db;
		}
	}

	@Override
	protected Database doGetDatabase(String name) throws DatabaseException {
		synchronized (lock) {
			return databases.get(name);
		}
	}

	@Override
	protected void doDropDatabase(String name) throws DatabaseException {
		databases.remove(name);
	}

	@Override
	public void close() {
	}
}
