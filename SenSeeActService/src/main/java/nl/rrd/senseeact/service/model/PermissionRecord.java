package nl.rrd.senseeact.service.model;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.senseeact.dao.AbstractDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PermissionRecord extends AbstractDatabaseObject {
	/**
	 * This permission allows users to write records to a project table without
	 * a user field. This kind of tables is used for general project resources.
	 * Parameters:
	 *
	 * - project: project code
	 * - table: table name (wildcard * for all tables)
	 */
	public static final String PERMISSION_WRITE_RESOURCE_TABLE =
			"write_resource_table";

	public static final List<String> PERMISSIONS = List.of(
			PERMISSION_WRITE_RESOURCE_TABLE
	);

	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String user;
	@DatabaseField(value=DatabaseType.STRING)
	private String permission;

	// JSON code for paramsMap
	@DatabaseField(value=DatabaseType.TEXT, json=true)
	private String params;

	private Map<String,Object> paramsMap = new LinkedHashMap<>();

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getParams() {
		return JsonMapper.generate(paramsMap);
	}

	public void setParams(String params) throws ParseException {
		paramsMap = JsonMapper.parse(params, new TypeReference<>() {});
	}

	public Map<String,Object> getParamsMap() {
		return paramsMap;
	}

	public void setParamsMap(Map<String,Object> paramsMap) {
		this.paramsMap = paramsMap;
	}
}
