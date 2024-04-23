package nl.rrd.senseeact.service;

import nl.rrd.senseeact.client.model.PermissionRecord;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.dao.DatabaseSort;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.senseeact.service.model.PermissionTable;
import nl.rrd.utils.exception.DatabaseException;

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

	public boolean hasPermission(Database authDb, String user,
			String permission, Map<String,Object> params)
			throws DatabaseException {
		synchronized (LOCK) {
			List<PermissionRecord> records = findPermissions(authDb, user,
					permission);
			for (PermissionRecord record : records) {
				if (matchesParams(params, record.getParamsMap()))
					return true;
			}
			return false;
		}
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

	public List<PermissionRecord> getPermissions(Database authDb, String user,
			String permission) throws DatabaseException {
		synchronized (LOCK) {
			DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", user),
					new DatabaseCriteria.Equal("permission", permission)
			);
			DatabaseSort[] sort = new DatabaseSort[] {
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
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("user", user),
				new DatabaseCriteria.Equal("permission", permission)
		);
		return authDb.select(new PermissionTable(), criteria, 0, null);
	}

	private boolean matchesParams(Map<String,Object> requiredParams,
			Map<String,Object> permissionParams) {
		for (String required : requiredParams.keySet()) {
			if (!permissionParams.containsKey(required))
				return false;
			Object requiredValue = requiredParams.get(required);
			Object permissionValue = permissionParams.get(required);
			if (!matchesValue(requiredValue, permissionValue))
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
}
