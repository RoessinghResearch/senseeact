package nl.rrd.senseeact.dao.sync;

import java.util.List;
import java.util.Map;

import nl.rrd.senseeact.dao.DatabaseAction;

/**
 * Group of database actions of the same action type and the same table. It
 * defines actions that were downloaded from the remote side and should be run
 * on the local database. Some actions may have been skipped. The group defines
 * the last action. This can be the last action in "actionsToRun" or a later
 * action that was skipped. It can be used to update the sync progress.
 *
 * @author Dennis Hofs (RRD)
 */
public class DatabaseActionGroup {
	private List<Item> actionsToRun;
	private DatabaseAction lastAction = null;

	/**
	 * Returns the database actions that should be run. This excludes any
	 * skipped actions. It can therefore be empty.
	 *
	 * @return the database actions that should be run
	 */
	public List<Item> getActionsToRun() {
		return actionsToRun;
	}

	/**
	 * Sets the database actions that should be run. This excludes any skipped
	 * actions. It can therefore be empty.
	 *
	 * @param actionsToRun the database actions that should be run
	 */
	public void setActionsToRun(List<Item> actionsToRun) {
		this.actionsToRun = actionsToRun;
	}

	/**
	 * Returns the last database action. This can be the last action in
	 * "actionsToRun" or a later action that was skipped. It can be used to
	 * update the sync progress.
	 *
	 * @return the last database action
	 */
	public DatabaseAction getLastAction() {
		return lastAction;
	}

	/**
	 * Sets the last database action. This can be the last action in
	 * "actionsToRun" or a later action that was skipped. It can be used to
	 * update the sync progress.
	 *
	 * @param lastAction the last database action
	 */
	public void setLastAction(DatabaseAction lastAction) {
		this.lastAction = lastAction;
	}

	/**
	 * Item in an action group. It defines a validated action that should be run
	 * and contains the action and possible validated data that is associated
	 * with the action (to insert or update). The data may be null.
	 */
	public static class Item {
		public DatabaseAction action;
		public Map<String,Object> data;

		public Item(DatabaseAction action, Map<String,Object> data) {
			this.action = action;
			this.data = data;
		}
	}
}
