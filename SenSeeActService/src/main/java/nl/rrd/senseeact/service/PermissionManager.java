package nl.rrd.senseeact.service;

import nl.rrd.senseeact.client.model.PermissionRecord;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.dao.DatabaseSort;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.senseeact.service.model.PermissionTable;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.DatabaseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PermissionManager {
	private static final Object LOCK = new Object();

	private static PermissionManager instance = null;

	private PermissionManager() {
	}

	public static PermissionManager getInstance() {
		synchronized (LOCK) {
			if (instance == null)
				instance = new PermissionManager();
			return instance;
		}
	}

	public void checkPermission(Database authDb, String user, String permission,
			Map<String,Object> params) throws ForbiddenException,
			DatabaseException {
		if (params == null)
			params = new LinkedHashMap<>();
		if (!hasPermission(authDb, user, permission, params)) {
			throw new ForbiddenException(String.format(
					"Required permission \"%s\" not found", permission));
		}
	}

	public void checkPermission(String permission, Map<String,Object> params,
			List<PermissionRecord> records) throws ForbiddenException {
		if (params == null)
			params = new LinkedHashMap<>();
		if (!hasPermission(permission, params, records)) {
			throw new ForbiddenException(String.format(
					"Required permission \"%s\" not found", permission));
		}
	}

	public boolean hasPermission(Database authDb, String user,
			String permission, Map<String,Object> params)
			throws DatabaseException {
		synchronized (LOCK) {
			List<PermissionRecord> records = findPermissions(authDb, user,
					null);
			return hasPermission(permission, params, records);
		}
	}

	public boolean hasPermission(String permission, Map<String,Object> params,
			List<PermissionRecord> records) {
		PermissionRecord required = new PermissionRecord();
		required.setPermission(permission);
		required.setParamsMap(params);
		List<PermissionRecord> requiredAtomicList = getAtomicPermissions(
				required);
		List<PermissionRecord> availableAtomicList = new ArrayList<>();
		for (PermissionRecord record : records) {
			availableAtomicList.addAll(getAtomicPermissions(record));
		}
		for (PermissionRecord requiredAtomic : requiredAtomicList) {
			if (!hasAtomicPermission(requiredAtomic, availableAtomicList))
				return false;
		}
		return true;
	}

	private boolean hasAtomicPermission(PermissionRecord required,
			List<PermissionRecord> availableList) {
		for (PermissionRecord available : availableList) {
			if (matchesPermission(required, available))
				return true;
		}
		return false;
	}

	private boolean matchesPermission(PermissionRecord required,
			PermissionRecord available) {
		if (!required.getPermission().equals(available.getPermission()))
			return false;
		Map<String,Object> requiredParams = required.getParamsMap();
		Map<String,Object> availableParams = available.getParamsMap();
		for (String requiredKey : requiredParams.keySet()) {
			if (!availableParams.containsKey(requiredKey))
				return false;
			Object requiredValue = requiredParams.get(requiredKey);
			Object availableValue = availableParams.get(requiredKey);
			if (!matchesValue(requiredValue, availableValue))
				return false;
		}
		return true;
	}

	private boolean matchesValue(Object requiredValue, Object permissionValue) {
		if (permissionValue instanceof String permissionStr) {
			if (permissionStr.equals("*"))
				return true;
		}
		if (requiredValue == null)
			return permissionValue == null;
		if (permissionValue == null)
			return false;
		return requiredValue.equals(permissionValue);
	}

	private List<PermissionRecord> getAtomicPermissions(
			PermissionRecord record) {
		PermissionRepository permRepo = AppComponents.get(
				PermissionRepository.class);
		PermissionType type = permRepo.findPermissionType(
				record.getPermission());
		if (type == null)
			return List.of(record);
		List<PermissionRecord> children = type.getChildren(record);
		if (children.isEmpty())
			return List.of(record);
		List<PermissionRecord> result = new ArrayList<>();
		for (PermissionRecord child : children) {
			result.addAll(getAtomicPermissions(child));
		}
		return result;
	}

	public List<PermissionRecord> getPermissions(Database authDb, String user)
			throws DatabaseException {
		synchronized (LOCK) {
			DatabaseCriteria criteria = new DatabaseCriteria.Equal(
					"user", user);
			DatabaseSort[] sort = new DatabaseSort[] {
					new DatabaseSort("permission", true),
					new DatabaseSort("id", true)
			};
			return authDb.select(new PermissionTable(), criteria, 0, sort);
		}
	}

	public void grant(Database authDb, String user, String permission)
			throws DatabaseException {
		grant(authDb, user, permission, null);
	}

	public void grant(Database authDb, String user, String permission,
			Map<String,Object> params) throws DatabaseException {
		synchronized (LOCK) {
			PermissionRecord record = findExactPermission(authDb, user,
					permission, params);
			if (record != null)
				return;
			record = new PermissionRecord();
			record.setUser(user);
			record.setPermission(permission);
			record.setParamsMap(params);
			authDb.insert(PermissionTable.NAME, record);
		}
	}

	public void revoke(Database authDb, String user, String permission)
			throws DatabaseException {
		revoke(authDb, user, permission, null);
	}

	public void revoke(Database authDb, String user, String permission,
			Map<String,Object> params) throws DatabaseException {
		if (params == null)
			params = new LinkedHashMap<>();
		synchronized (LOCK) {
			PermissionRecord record = findExactPermission(authDb, user,
					permission, params);
			if (record == null)
				return;
			authDb.delete(PermissionTable.NAME, record);
		}
	}

	public void revokeAll(Database authDb, String user, String permission)
			throws DatabaseException {
		synchronized (LOCK) {
			DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", user),
					new DatabaseCriteria.Equal("permission", permission)
			);
			authDb.delete(new PermissionTable(), criteria);
		}
	}

	private PermissionRecord findExactPermission(Database authDb, String user,
			String permission, Map<String,Object> params)
			throws DatabaseException {
		List<PermissionRecord> records = findPermissions(authDb, user,
				permission);
		for (PermissionRecord record : records) {
			if (record.getParamsMap().equals(params))
				return record;
		}
		return null;
	}

	private List<PermissionRecord> findPermissions(Database authDb, String user,
			String permission) throws DatabaseException {
		List<DatabaseCriteria> andCriteria = new ArrayList<>();
		andCriteria.add(new DatabaseCriteria.Equal("user", user));
		if (permission != null) {
			andCriteria.add(new DatabaseCriteria.Equal("permission",
					permission));
		}
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				andCriteria.toArray(new DatabaseCriteria[0]));
		return authDb.select(new PermissionTable(), criteria, 0, null);
	}
}
