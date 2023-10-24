package nl.rrd.senseeact.dao;

import nl.rrd.utils.AppComponent;
import nl.rrd.utils.AppComponents;
import nl.rrd.senseeact.dao.memdb.MemoryDatabaseFactory;

import java.io.IOException;

/**
 * This factory can create {@link DatabaseConnection DatabaseConnection}s. The
 * factory can be configured as an {@link AppComponent AppComponent}. Its
 * default implementation is {@link MemoryDatabaseFactory
 * MemoryDatabaseFactory}.
 *
 * <p>The factory can configure database connections so that the databases log
 * actions for synchronisation with a remote database. For more information see
 * {@link Database Database}.</p>
 *
 * @author Dennis Hofs (RRD)
 */
@AppComponent
public abstract class DatabaseFactory {
	private boolean syncEnabled = false;
	private boolean saveSyncedRemoteActions = true;

	/**
	 * Returns whether action logging is enabled for synchronisation with
	 * a remote database.
	 *
	 * @return true if synchronisation logging is enabled, false otherwise
	 */
	public boolean isSyncEnabled() {
		return syncEnabled;
	}

	/**
	 * Returns whether action logging should be enabled for synchronisation
	 * with a remote database.
	 *
	 * @param syncEnabled true if synchronisation logging is enabled, false
	 * otherwise
	 */
	public void setSyncEnabled(boolean syncEnabled) {
		this.syncEnabled = syncEnabled;
	}

	/**
	 * If synchronization logging is enabled (see {@link #isSyncEnabled()
	 * isSyncEnabled()}), this method returns whether database actions received
	 * from the remote database should be logged as well. If this is false,
	 * then only local database actions are logged. The default is true.
	 * 
	 * @return true if remote database actions are logged, false if only local
	 * database actions are logged
	 */
	public boolean isSaveSyncedRemoteActions() {
		return saveSyncedRemoteActions;
	}

	/**
	 * If synchronization logging is enabled (see {@link
	 * #setSyncEnabled(boolean) setSyncEnabled()}), this method determines
	 * whether database actions received from the remote database should be
	 * logged as well. If this is false, then only local database actions are
	 * logged. The default is true.
	 * 
	 * @param saveSyncedRemoteActions true if remote database actions are
	 * logged, false if only local database actions are logged
	 */
	public void setSaveSyncedRemoteActions(boolean saveSyncedRemoteActions) {
		this.saveSyncedRemoteActions = saveSyncedRemoteActions;
	}

	/**
	 * Returns a new instance of {@link MemoryDatabaseFactory
	 * MemoryDatabaseFactory}. This method is called as a default when you
	 * get an instance from {@link AppComponents AppComponents} and you haven't
	 * configured a specific subclass.
	 * 
	 * @return a new memory database factory
	 */
	public static DatabaseFactory getInstance() {
		return new MemoryDatabaseFactory();
	}

	/**
	 * Connects to the database server and returns a {@link DatabaseConnection
	 * DatabaseConnection}. When you no longer need the connection, you should
	 * call {@link DatabaseConnection#close() close()}.
	 *
	 * <p>The returned connection will be configured for action logging as you
	 * have configured this factory.</p>
	 *
	 * @return the database connection
	 * @throws IOException if the connection could not be established
	 */
	public DatabaseConnection connect() throws IOException {
		DatabaseConnection conn = doConnect();
		conn.setSyncEnabled(syncEnabled);
		conn.setSaveSyncedRemoteActions(saveSyncedRemoteActions);
		return conn;
	}
	
	/**
	 * Connects to the database server and returns a {@link DatabaseConnection
	 * DatabaseConnection}. When you no longer need the connection, you should
	 * call {@link DatabaseConnection#close() close()}.
	 * 
	 * @return the database connection
	 * @throws IOException if the connection could not be established
	 */
	protected abstract DatabaseConnection doConnect() throws IOException;
}
