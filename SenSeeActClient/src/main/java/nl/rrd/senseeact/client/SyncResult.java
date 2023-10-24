package nl.rrd.senseeact.client;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.sync.SyncProgress;
import nl.rrd.utils.json.JsonObject;

/**
 * The result of {@link
 * SenSeeActClient#syncWrite(String, int, Database, String, SyncRestriction, SyncProgressListener)
 * syncWrite()} or {@link
 * SenSeeActClient#syncRead(String, int, Database, String, boolean, SyncRestriction, SyncProgressListener)
 * syncRead()}. It contains the total number of synchronized database actions
 * and the sync progress after the sync has completed. The number of database
 * actions are merged actions. Database actions at the remote database may have
 * been merged into one action. For example an insert and a couple of updates
 * on the same record can be merged into one insert action.
 *
 * @author Dennis Hofs (RRD)
 */
public class SyncResult extends JsonObject {
	private int count = 0;
	private List<SyncProgress> progress = new ArrayList<>();

	/**
	 * Returns the total number of database actions that have been synchronized.
	 * The default is 0.
	 *
	 * @return the number of synchronized database actions
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Sets the total number of database actions that have been synchronized.
	 * The default is 0.
	 *
	 * @param count the number of synchronized database actions
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Returns the sync progress after the synchronization has completed. The
	 * default is an empty list.
	 *
	 * @return the sync progress
	 */
	public List<SyncProgress> getProgress() {
		return progress;
	}

	/**
	 * Sets the sync progress after the synchronization has completed. The
	 * default is an empty list.
	 *
	 * @param progress the sync progress
	 */
	public void setProgress(List<SyncProgress> progress) {
		this.progress = progress;
	}
}
