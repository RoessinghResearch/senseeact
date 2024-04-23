package nl.rrd.senseeact.dao;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class models a database action that can be logged for synchronisation
 * with a remote database.
 *
 * @author Dennis Hofs (RRD)
 */
public class DatabaseAction extends BaseDatabaseObject {
	public static final String SOURCE_LOCAL = "local";
	
	@JsonIgnore
	private String id;

	@DatabaseField(value=DatabaseType.STRING)
	private String table;
	@DatabaseField(value=DatabaseType.STRING)
	private String user = null;
	@DatabaseField(value=DatabaseType.STRING)
	private Action action;
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String recordId;
	@DatabaseField(value=DatabaseType.TEXT)
	private String jsonData = null;
	// "sampleTime" and "time" are stored as LONG instead of ISOTIME so times
	// can be compared with GreaterThan / LessThan
	@DatabaseField(value=DatabaseType.LONG, index=true)
	private Long sampleTime = null;
	@DatabaseField(value=DatabaseType.LONG)
	private long time;
	@DatabaseField(value=DatabaseType.INT)
	private int order = 0;
	@DatabaseField(value=DatabaseType.STRING)
	private String source = SOURCE_LOCAL;
	@DatabaseField(value=DatabaseType.STRING)
	private String author = null;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the table on which the action was run.
	 *
	 * @return the table name
	 */
	public String getTable() {
		return table;
	}

	/**
	 * Sets the table on which the action was run.
	 *
	 * @param table the table name
	 */
	public void setTable(String table) {
		this.table = table;
	}

	/**
	 * Returns the user whose data was affected by this action. The user should
	 * be obtained from the "user" field of a database object. For database
	 * actions in a table without a user field, this method returns null.
	 *
	 * @return the user name or null
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the user whose data was affected by this action. The user should be
	 * obtained from the "user" field of a database object. For database actions
	 * in a table without a user field, this should be null (default).
	 *
	 * @param user the user name or null
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Returns the action.
	 *
	 * @return the action
	 */
	public Action getAction() {
		return action;
	}

	/**
	 * Sets the action.
	 *
	 * @param action the action
	 */
	public void setAction(Action action) {
		this.action = action;
	}

	/**
	 * Returns the ID of the record that was affected by the action.
	 *
	 * @return the record ID
	 */
	public String getRecordId() {
		return recordId;
	}

	/**
	 * Sets the ID of the record that was affected by the action.
	 *
	 * @param recordId the record ID
	 */
	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	/**
	 * Returns the data associated with the action, as a JSON object. For an
	 * insert, it contains the complete record including "id". For an update it
	 * contains the updated columns. For a select or delete, the data is null.
	 *
	 * @return the JSON data or null
	 */
	public String getJsonData() {
		return jsonData;
	}
	
	/**
	 * Returns the data associated with the action. For an insert, it contains
	 * the complete record including "id". For an update it contains the updated
	 * columns. For a select or delete, the data is null.
	 * 
	 * <p>This method parses the jsonData field and returns the result. Any
	 * changes to the map will not be stored in this object.</p>
	 * 
	 * @return the data or null
	 */
	@JsonIgnore
	public Map<?,?> getData() {
		if (jsonData == null)
			return null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(jsonData, Map.class);
		} catch (JsonMappingException ex) {
			throw new RuntimeException("Can't convert JSON data to map: " +
					ex.getMessage(), ex);
		} catch (JsonParseException ex) {
			throw new RuntimeException("Can't parse JSON data: " +
					ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new RuntimeException("Can't read JSON data: " +
					ex.getMessage(), ex);
		}
	}

	/**
	 * Sets the data associated with the action, as a JSON object. For an
	 * insert, it contains the complete record including "id". For an update it
	 * contains the updated columns. For a select or delete, the data is null.
	 *
	 * @param jsonData the JSON data or null
	 */
	public void setJsonData(String jsonData) {
		this.jsonData = jsonData;
	}
	
	/**
	 * Sets the data associated with the action, as a JSON object. For an
	 * insert, it contains the complete record including "id". For an update it
	 * contains the updated columns. For a select or delete, the data is null.
	 * 
	 * <p>This method converts the data map to a JSON string and sets it in the
	 * jsonData field. Any later changes to the map will not be stored in this
	 * object.</p>
	 * 
	 * @param data the data or null
	 */
	@JsonIgnore
	public void setData(Map<?,?> data) {
		if (data == null) {
			this.jsonData = null;
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		try {
			this.jsonData = mapper.writeValueAsString(data);
		} catch (JsonProcessingException ex) {
			throw new RuntimeException("Can't convert map to JSON string: " +
					ex.getMessage(), ex);
		}
	}

	/**
	 * If this is an action on a sample table, this method returns the time of
	 * the sample that was affected by the action. Otherwise it returns null.
	 * 
	 * @return the sample time or null
	 */
	public Long getSampleTime() {
		return sampleTime;
	}

	/**
	 * This method only applies for database actions on sample tables. It sets
	 * the time of the sample that was affected by the action.
	 * 
	 * @param sampleTime the sample time
	 */
	public void setSampleTime(Long sampleTime) {
		this.sampleTime = sampleTime;
	}

	/**
	 * Returns the time at which the action was run.
	 *
	 * @return the time at which the action was run
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Sets the time at which the action was run.
	 *
	 * @param time the time at which the action was run
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * Returns an order number. This can be used to order sequential actions
	 * that occurred within the same millisecond. Actions with another time
	 * or parallel actions may have the same number.
	 *
	 * @return the order number
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * Returns an order number. This can be used to order sequential actions
	 * that occurred within the same millisecond. Actions with another time
	 * or parallel actions may have the same number.
	 *
	 * @param order the order number
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Returns the source of this action. This can be {@link #SOURCE_LOCAL
	 * SOURCE_LOCAL} (default) or the ID of a remote server. In the latter case,
	 * the action was added as a result of a synchronisation from the remote
	 * server.
	 *
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * Sets the source of this action. This can be {@link #SOURCE_LOCAL
	 * SOURCE_LOCAL} (default) or the ID of a remote server. In the latter case,
	 * the action was added as a result of a synchronisation from the remote
	 * server.
	 *
	 * @param source the source
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * This was used for audit logging, which is no longer supported.
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * This was used for audit logging, which is no longer supported.
	 */
	public void setAuthor(String author) {
		this.author = author;
	}
	
	/**
	 * The possible actions.
	 */
	public enum Action {
		INSERT,
		UPDATE,
		DELETE
	}
}
