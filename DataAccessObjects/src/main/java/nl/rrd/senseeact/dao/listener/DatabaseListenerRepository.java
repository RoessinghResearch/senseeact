package nl.rrd.senseeact.dao.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseAction;

/**
 * This repository contains all database listeners within the current process.
 * Clients can add and remove listeners per database with {@link
 * #addDatabaseListener(String, DatabaseListener) addDatabaseListener()} and
 * {@link #removeDatabaseListener(String, DatabaseListener)
 * removeDatabaseListener()}.
 * 
 * <p>The class {@link Database Database} calls the notify methods. They should
 * not be called by clients.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class DatabaseListenerRepository {
	private static final Object LOCK = new Object();
	private static DatabaseListenerRepository instance = null;
	
	private Map<String,List<DatabaseListener>> listeners = new HashMap<>();
	private Map<String,List<DatabaseActionListener>> actionListeners =
			new HashMap<>();
	
	/**
	 * This private constructor is used in {@link #getInstance()
	 * getInstance()}.
	 */
	private DatabaseListenerRepository() {
	}
	
	/**
	 * Returns the database listener repository.
	 * 
	 * @return the database listener repository
	 */
	public static DatabaseListenerRepository getInstance() {
		synchronized (LOCK) {
			if (instance == null)
				instance = new DatabaseListenerRepository();
			return instance;
		}
	}
	
	/**
	 * Adds a database listener.
	 * 
	 * @param database the database
	 * @param listener the listener
	 */
	public void addDatabaseListener(String database,
			DatabaseListener listener) {
		synchronized (LOCK) {
			List<DatabaseListener> ls = listeners.computeIfAbsent(database,
					key -> new ArrayList<>());
			ls.add(listener);
		}
	}
	
	/**
	 * Removes a database listener.
	 * 
	 * @param database the database
	 * @param listener the listener
	 */
	public void removeDatabaseListener(String database,
			DatabaseListener listener) {
		synchronized (LOCK) {
			List<DatabaseListener> ls = listeners.get(database);
			if (ls == null)
				return;
			ls.remove(listener);
			if (ls.isEmpty())
				listeners.remove(database);
		}
	}
	
	/**
	 * Returns all current database listeners for the specified database.
	 * 
	 * @param database the database
	 * @return the listeners
	 */
	public List<DatabaseListener> getDatabaseListeners(String database) {
		synchronized (LOCK) {
			List<DatabaseListener> ls = listeners.get(database);
			if (ls == null)
				return new ArrayList<>();
			else
				return new ArrayList<>(ls);
		}
	}
	
	/**
	 * Adds a database action listener.
	 * 
	 * @param database the database
	 * @param listener the listener
	 */
	public void addDatabaseActionListener(String database,
			DatabaseActionListener listener) {
		synchronized (LOCK) {
			List<DatabaseActionListener> ls = actionListeners.computeIfAbsent(
					database, key -> new ArrayList<>());
			ls.add(listener);
		}
	}
	
	/**
	 * Removes a database action listener.
	 * 
	 * @param database the database
	 * @param listener the listener
	 */
	public void removeDatabaseActionListener(String database,
			DatabaseActionListener listener) {
		synchronized (LOCK) {
			List<DatabaseActionListener> ls = actionListeners.get(database);
			if (ls == null)
				return;
			ls.remove(listener);
			if (ls.isEmpty())
				actionListeners.remove(database);
		}
	}
	
	/**
	 * Returns all current database action listeners for the specified database.
	 * 
	 * @param database the database
	 * @return the listeners
	 */
	public List<DatabaseActionListener> getDatabaseActionListeners(
			String database) {
		synchronized (LOCK) {
			List<DatabaseActionListener> ls = actionListeners.get(database);
			if (ls == null)
				return new ArrayList<>();
			else
				return new ArrayList<>(ls);
		}
	}
	
	/**
	 * Called when a database event occurs on the database. Registered listeners
	 * will be notified.
	 * 
	 * @param event the event
	 */
	public void notifyDatabaseEvent(DatabaseEvent event) {
		List<DatabaseListener> ls = getDatabaseListeners(event.getDatabase());
		for (DatabaseListener l : ls) {
			l.onDatabaseEvent(event);
		}
	}

	/**
	 * Called when one or more database actions are added. All actions in the
	 * specified list should have the same type {@link
	 * nl.rrd.senseeact.dao.DatabaseAction.Action DatabaseAction.Action}.
	 * 
	 * @param database the database
	 * @param table the table
	 * @param actions the new database actions
	 */
	public void notifyAddDatabaseActions(String database, String table,
			List<DatabaseAction> actions) {
		List<DatabaseActionListener> ls = getDatabaseActionListeners(database);
		for (DatabaseActionListener l : ls) {
			l.onAddDatabaseActions(database, table, actions);
		}
	}
}
