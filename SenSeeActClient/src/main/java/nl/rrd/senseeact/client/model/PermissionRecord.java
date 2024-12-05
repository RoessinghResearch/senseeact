package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.senseeact.dao.BaseDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class PermissionRecord extends BaseDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String user;
	@DatabaseField(value=DatabaseType.STRING)
	private String permission;

	// JSON code for paramsMap
	@DatabaseField(value=DatabaseType.TEXT, json=true)
	@JsonIgnore
	private String params;

	@JsonProperty("params")
	private Map<String,Object> paramsMap = new LinkedHashMap<>();

	/**
	 * Returns the user ID of the user that holds the permission.
	 *
	 * @return the user ID of the user that holds the permission
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the user ID of the user that holds the permission.
	 *
	 * @param user the user ID of the user that holds the permission
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Returns the permission name. This should be one of the PERMISSION_*
	 * constants defined in this class.
	 *
	 * @return the permission name
	 */
	public String getPermission() {
		return permission;
	}

	/**
	 * Sets the permission name. This should be one of the PERMISSION_*
	 * constants defined in this class.
	 *
	 * @param permission the permission name
	 */
	public void setPermission(String permission) {
		this.permission = permission;
	}

	/**
	 * Returns the JSON code for the permission parameters. This method is used
	 * for the DAO. Users can call {@link #getParamsMap() getParamsMap()}.
	 *
	 * @return the JSON code for the permission parameters
	 */
	public String getParams() {
		return JsonMapper.generate(paramsMap);
	}

	/**
	 * Sets the JSON code for the permission parameters. This method is used for
	 * the DAO. Users can call {@link #setParamsMap(Map) setParamsMap()}.
	 *
	 * @param params the JSON code for the permission parameters
	 * @throws ParseException if the JSON code is invalid
	 */
	public void setParams(String params) throws ParseException {
		paramsMap = JsonMapper.parse(params, new TypeReference<>() {});
	}

	/**
	 * Returns the permission parameters. This depends on the permission and is
	 * documented at the respective PERMISSION_* constant in this class.
	 *
	 * @return the permission parameters
	 */
	public Map<String,Object> getParamsMap() {
		return paramsMap;
	}

	/**
	 * Sets the permission parameters. This depends on the permission and is
	 * documented at the respective PERMISSION_* constant in this class.
	 *
	 * @param paramsMap the permission parameters
	 */
	public void setParamsMap(Map<String,Object> paramsMap) {
		this.paramsMap = paramsMap;
	}
}
