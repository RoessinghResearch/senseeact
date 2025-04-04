package nl.rrd.senseeact.client.model.sample;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ObjectMapSample extends UTCSample {
	@DatabaseField(value=DatabaseType.TEXT, json=true)
	private String values;

	private Map<String,Object> valuesMap = new LinkedHashMap<>();

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public ObjectMapSample() {}

	/**
	 * Constructs a new sample at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 *
	 * @param user the user (user ID)
	 * @param tzTime the time
	 */
	public ObjectMapSample(String user, ZonedDateTime tzTime) {
		super(user, tzTime);
	}

	/**
	 * Returns the JSON code for the values. This method is used for the DAO.
	 * Users can call {@link #getValuesMap() getValuesMap()}.
	 *
	 * @return the JSON code for the values
	 */
	public String getValues() {
		return JsonMapper.generate(valuesMap);
	}

	/**
	 * Sets the JSON code for the values. This method is used for the DAO. Users
	 * can call {@link #setValuesMap(Map) setValuesMap()}.
	 *
	 * @param values the JSON code for the values
	 */
	public void setValues(String values) throws ParseException {
		this.valuesMap = JsonMapper.parse(values, new TypeReference<>() {});
	}

	public Map<String,Object> getValuesMap() {
		return valuesMap;
	}

	public void setValuesMap(Map<String,Object> valuesMap) {
		this.valuesMap = valuesMap;
	}
}
