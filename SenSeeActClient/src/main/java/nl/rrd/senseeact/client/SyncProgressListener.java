package nl.rrd.senseeact.client;

/**
 * This listener is notified each time a batch of database actions has been
 * synchronised from or to a remote database, and when the total
 * synchronisation is completed.
 * 
 * @author Dennis Hofs (RRD)
 */
public interface SyncProgressListener {
	
	/**
	 * Called when a new batch of database actions has been synchronised. It
	 * specifies the number of merged synchronised database actions until now
	 * and the estimated total number of unmerged database actions. The latter
	 * is determined at the start of the synchronisation and will not change.
	 *
	 * <p>The actual total number is often different than the estimated total.
	 * It could be significantly lower if database actions were merged. For
	 * example an insert and some updates can be merged into one insert. It
	 * is also possible that the actual total number becomes slightly higher
	 * than the estimation. This can happen if new database actions were added
	 * after the estimation was made.</p>
	 *
	 * @param current the number of (merged) synchronised database actions until
	 * now
	 * @param unmergedTotal the expected total number of (unmerged) database
	 * actions to synchronise
	 */
	void syncUpdate(int current, int unmergedTotal);
}
