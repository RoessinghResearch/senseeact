package nl.rrd.senseeact.dao.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseAction;
import nl.rrd.senseeact.dao.DatabaseAction.Action;
import nl.rrd.senseeact.dao.DatabaseCriteria;

import java.io.IOException;
import java.util.*;

/**
 * This class can merge a list of database actions. The returned list will
 * have only one action per record. It takes the last action of each record
 * and merges any previous actions into it.
 * 
 * @author Dennis Hofs (RRD)
 */
public class DatabaseActionMerger {
	
	/**
	 * Merges a list of database actions.
	 *
	 * @param actions the database action
	 * @return the merged actions
	 * @throws MergeException if the actions can't be merged
	 */
	public List<DatabaseAction> mergeActions(List<DatabaseAction> actions)
			throws MergeException {
		// actionMap: map from record ID to list of actions
		// First item in "actionMap" is the record ID of the last item in
		// "actions".
		// Value list of actions starts with the most recent action on
		// respective record.
		Map<String, List<DatabaseAction>> actionMap = new LinkedHashMap<>();
		for (int i = actions.size() - 1; i >= 0; i--) {
			DatabaseAction action = actions.get(i);
			List<DatabaseAction> recordActions = actionMap.computeIfAbsent(
					action.getRecordId(), key -> new ArrayList<>());
			recordActions.add(action);
		}
		List<DatabaseAction> mergedActions = new ArrayList<>();
		for (String recordId : actionMap.keySet()) {
			List<DatabaseAction> recordActions = actionMap.get(recordId);
			MergeResult merge = mergeRecordActions(recordActions);
			mergedActions.add(0, merge.mergedAction);
		}
		return mergedActions;
	}

	private static class MergeResult {
		public DatabaseAction mergedAction;
		public int deleteCount = 0;
	}

	/**
	 * Merges a list of database actions on one record into one action. The
	 * list of actions should start with the most recent action. The returned
	 * action will have the same time and order as the last action.
	 *
	 * @param actions the actions from new to old
	 * @return the merged action
	 * @throws MergeException if the actions can't be merged
	 */
	private MergeResult mergeRecordActions(List<DatabaseAction> actions)
			throws MergeException {
		MergeResult result = new MergeResult();
		Iterator<DatabaseAction> it = actions.iterator();
		DatabaseAction action = it.next();
		result.mergedAction = action;
		boolean merged = false;
		if (action.getAction() == Action.INSERT ||
				action.getAction() == Action.DELETE ||
				!it.hasNext()) {
			merged = true;
		} else if (action.getAction() != Action.UPDATE) {
			throw new MergeException("Can't merge action " +
					action.getAction());
		}
		if (!merged) {
			// action is update and there are more actions
			Map<String, Object> data = parseActionData(action);
			while (!merged && it.hasNext()) {
				DatabaseAction prevAction = it.next();
				if (prevAction.getAction() != Action.INSERT &&
						prevAction.getAction() != Action.UPDATE) {
					throw new MergeException(String.format(
							"Can't merge action %s into update action",
							prevAction.getAction()));
				}
				result.deleteCount++;
				Map<String, Object> prevData = parseActionData(prevAction);
				mergeActionData(data, prevData);
				if (prevAction.getAction() == Action.INSERT) {
					action.setAction(Action.INSERT);
					writeActionData(action, data);
					merged = true;
				}
			}
			writeActionData(action, data);
		}
		while (it.hasNext()) {
			it.next();
			result.deleteCount++;
		}
		return result;
	}

	/**
	 * Merges the list of all database actions on one record into one action.
	 * The list of actions should start with the most recent action.
	 * 
	 * @param actions the actions from new to old
	 * @throws MergeException if the actions can't be merged
	 */
	public void mergeRecordActions(Database database, String table,
			List<DatabaseAction> actions) throws DatabaseException,
			MergeException {
		MergeResult merge = mergeRecordActions(actions);
		if (merge.deleteCount == 0)
			return;
		DatabaseAction action = merge.mergedAction;
		database.update(table, action);
		DatabaseCriteria criteria = new DatabaseCriteria.And(
			new DatabaseCriteria.NotEqual("id", action.getId()),
			new DatabaseCriteria.Equal("recordId", action.getRecordId())
		);
		database.delete(table, DatabaseAction.class, criteria);
	}

	/**
	 * Parses the JSON data of the specified insert or update action.
	 * 
	 * @param action the action
	 * @return the parsed data
	 * @throws MergeException if the data can't be parsed
	 */
	private Map<String,Object> parseActionData(DatabaseAction action)
			throws MergeException {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(action.getJsonData(),
					new TypeReference<Map<String,Object>>() {});
		} catch (IOException ex) {
			throw new MergeException(
					"Can't parse JSON data from database action: " +
					ex.getMessage(), ex);
		}
	}
	
	/**
	 * Merges "prevData" into "data". It copies any keys that are in "prevData"
	 * but not in "data".
	 * 
	 * @param data the data
	 * @param prevData the previous data
	 */
	private void mergeActionData(Map<String,Object> data,
			Map<String,Object> prevData) {
		for (String key : prevData.keySet()) {
			if (!data.containsKey(key)) {
				data.put(key, prevData.get(key));
			}
		}
	}
	
	/**
	 * Writes the specified data map as JSON data to the specified database
	 * action.
	 * 
	 * @param action the database action
	 * @param data the data
	 * @throws MergeException if the map can't be written as JSON data
	 */
	private void writeActionData(DatabaseAction action,
			Map<String,Object> data) throws MergeException {
		ObjectMapper mapper = new ObjectMapper();
		String jsonData;
		try {
			jsonData = mapper.writeValueAsString(data);
		} catch (JsonProcessingException ex) {
			throw new MergeException("Can't convert data map to JSON: " +
					ex.getMessage(), ex);
		}
		action.setJsonData(jsonData);
	}
}
