package nl.rrd.senseeact.dao.listener;

import nl.rrd.senseeact.dao.Database;

/**
 * A database listener can be notified when an action is performed on a
 * {@link Database Database}. Listeners can be registered in the {@link
 * DatabaseListenerRepository DatabaseListenerRepository}.
 * 
 * @author Dennis Hofs (RRD)
 */
public interface DatabaseListener {
	
	/**
	 * Called when a database event (insert, update, delete) occurs.
	 * 
	 * @param event the database event
	 */
	void onDatabaseEvent(DatabaseEvent event);
}
