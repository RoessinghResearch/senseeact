package nl.rrd.senseeact.client.model.sample;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class IntListSample extends UTCSample {
	@DatabaseField(value=DatabaseType.TEXT, json=true)
	private String values;

	private List<Integer> valuesList = new ArrayList<>();

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public IntListSample() {
	}

	/**
	 * Constructs a new sample at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 *
	 * @param user the user (user ID)
	 * @param tzTime the time
	 */
	public IntListSample(String user, ZonedDateTime tzTime) {
		super(user, tzTime);
	}

	/**
	 * Returns the JSON code for the values. This method is used for the DAO.
	 * Users can call {@link #getValuesList() getValuesList()}.
	 *
	 * @return the JSON code for the values
	 */
	public String getValues() {
		return JsonMapper.generate(valuesList);
	}

	/**
	 * Sets the JSON code for the values. This method is used for the DAO. Users
	 * can call {@link #setValuesList(List) setValuesList()}.
	 *
	 * @param values the JSON code for the values
	 */
	public void setValues(String values) throws ParseException {
		this.valuesList = JsonMapper.parse(values, new TypeReference<>() {});
	}

	public List<Integer> getValuesList() {
		return valuesList;
	}

	public void setValuesList(List<Integer> valuesList) {
		this.valuesList = valuesList;
	}
}
