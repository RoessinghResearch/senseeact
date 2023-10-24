package nl.rrd.senseeact.dao;

import nl.rrd.utils.exception.DatabaseException;

import java.util.List;

public class DatabaseConnectionDecorator extends DatabaseConnection {
	private DatabaseConnection baseConn;
	
	public DatabaseConnectionDecorator(DatabaseConnection baseConn) {
		this.baseConn = baseConn;
	}

	@Override
	public boolean isSyncEnabled() {
		return baseConn.isSyncEnabled();
	}

	@Override
	public void setSyncEnabled(boolean syncEnabled) {
		baseConn.setSyncEnabled(syncEnabled);
	}

	@Override
	public boolean isSaveSyncedRemoteActions() {
		return baseConn.isSaveSyncedRemoteActions();
	}

	@Override
	public void setSaveSyncedRemoteActions(boolean saveSyncedRemoteActions) {
		baseConn.setSaveSyncedRemoteActions(saveSyncedRemoteActions);
	}

	@Override
	public Database initDatabase(String name,
			List<? extends DatabaseTableDef<?>> tableDefs,
			boolean dropOldTables) throws DatabaseException {
		return baseConn.initDatabase(name, tableDefs, dropOldTables);
	}

	@Override
	protected boolean databaseExists(String name) throws DatabaseException {
		return baseConn.databaseExists(name);
	}

	@Override
	protected Database createDatabase(String name) throws DatabaseException {
		return baseConn.createDatabase(name);
	}

	@Override
	public Database getDatabase(String name) throws DatabaseException {
		return baseConn.getDatabase(name);
	}

	@Override
	protected Database doGetDatabase(String name) throws DatabaseException {
		return baseConn.doGetDatabase(name);
	}

	@Override
	public void dropDatabase(String name) throws DatabaseException {
		baseConn.dropDatabase(name);
	}

	@Override
	protected void doDropDatabase(String name) throws DatabaseException {
		baseConn.doDropDatabase(name);
	}

	@Override
	public void close() {
		baseConn.close();
	}
}
