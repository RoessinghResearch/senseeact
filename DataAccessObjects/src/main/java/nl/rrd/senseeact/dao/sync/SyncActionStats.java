package nl.rrd.senseeact.dao.sync;

import java.util.List;

import nl.rrd.utils.json.JsonObject;

/**
 * Statistics about new database actions that should be written to a remote
 * database. It defines the number of new (unmerged) database actions and the
 * time of the latest database action (if any). The values are obtained from
 * multiple queries (for each table first the count, then the latest time). If
 * new database actions are added between them, the actual count will be higher.
 *
 * <p>The number of database actions you will actually receive is often lower,
 * because actions may be merged. For example an insert and some updates on the
 * same record can be merged into one insert.</p>
 *
 * @author Dennis Hofs (RRD)
 */
public class SyncActionStats extends JsonObject {
	private List<SyncProgress> progress = null;
	private int count = 0;
	private Long latestTime = null;

	/**
	 * Constructs a new empty instance.
	 */
	public SyncActionStats() {
	}
	
	/**
	 * Constructs a new instance.
	 * 
	 * @param progress the progress from which the new database actions were
	 * retrieved (can be null)
	 * @param count an estimation of the number of new unmerged database actions
	 * @param latestTime the time of the latest database action, as a unix time
	 * in milliseconds. If there are no new database actions, this is null.
	 */
	public SyncActionStats(List<SyncProgress> progress, int count,
			Long latestTime) {
		this.progress = progress;
		this.count = count;
		this.latestTime = latestTime;
	}

	/**
	 * Returns the progress from which the new database actions were retrieved.
	 * This can be null.
	 * 
	 * @return the progress or null
	 */
	public List<SyncProgress> getProgress() {
		return progress;
	}

	/**
	 * Sets the progress from which the new database actions were retrieved.
	 * This can be null.
	 * 
	 * @param progress the progress or null
	 */
	public void setProgress(List<SyncProgress> progress) {
		this.progress = progress;
	}

	/**
	 * Returns the number of new unmerged database actions. This is an
	 * estimation. See the top of this page for more info.
	 * 
	 * @return the number of new database actions
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Sets the number of new database actions. This is an estimation. See the
	 * top of this page for more info.
	 * 
	 * @param count the number of new database actions
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Returns the time of the latest database action, as a unix time in
	 * milliseconds. If there are no new database actions, this is null.
	 * 
	 * @return the time of the latest database action or null
	 */
	public Long getLatestTime() {
		return latestTime;
	}

	/**
	 * Sets the time of the latest database action, as a unix time in
	 * milliseconds. If there are no new database actions, this is null.
	 * 
	 * @param latestTime the time of the latest database action or null
	 */
	public void setLatestTime(Long latestTime) {
		this.latestTime = latestTime;
	}
}
