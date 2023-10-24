package nl.rrd.senseeact.dao;

import java.util.List;

/**
 * This class defines metadata about a database table. See also {@link
 * TableMetadataTableDef TableMetadataTableDef}.
 * 
 * @author Dennis Hofs (RRD)
 */
public class TableMetadata extends AbstractDatabaseObject {

	/**
	 * The value is the current version of the specified table.
	 */
	public static final String KEY_VERSION = "version";

	/**
	 * The value is a JSON array with the names of all database fields in the
	 * specified table, excluding "id".
	 */
	public static final String KEY_FIELDS = "fields";
	
	/**
	 * The value is the qualified name of the Java data class, which should
	 * extend {@link DatabaseObject DatabaseObject}. The field/column
	 * definitions can be derived from it.
	 */
	public static final String KEY_DATA_CLASS = "data_class";
	
	/**
	 * The value is a JSON array with compound indexes on the table
	 * (single-field indexes are usually defined with a {@link DatabaseField
	 * DatabaseField} annotation in the data class. Each index is JSON object
	 * with these properties:
	 * 
	 * <p><ul>
	 * <li>name: the index name</li>
	 * <li>fields: JSON array with the names of the database fields</li>
	 * </ul></p>
	 */
	public static final String KEY_INDEXES = "compound_indexes";
	
	/**
	 * The value is "true" or "false". It defines whether the underlying
	 * database should have a separate table per user.
	 */
	public static final String KEY_SPLIT_BY_USER = "split_by_user";
	
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String table;

	@DatabaseField(value=DatabaseType.STRING)
	private String key;

	@DatabaseField(value=DatabaseType.TEXT)
	private String value;

	/**
	 * Returns the table name (lower case).
	 * 
	 * @return the table name (lower case)
	 */
	public String getTable() {
		return table;
	}

	/**
	 * Sets the table name (lower case).
	 * 
	 * @param table the table name (lower case)
	 */
	public void setTable(String table) {
		this.table = table;
	}

	/**
	 * Returns the metadata key. This should be one of the KEY_* constants
	 * defined in this class.
	 * 
	 * @return the metadata key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Sets the metadata key. This should be one of the KEY_* constants defined
	 * in this class.
	 * 
	 * @param key the metadata key
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Returns the metadata value.
	 * 
	 * @return the metadata value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the metadata value.
	 * 
	 * @param value the metadata value
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	/**
	 * This method takes a list of metadata objects for one table and tries
	 * to find the object with the specified key. If no object is found, this
	 * method returns null.
	 * 
	 * @param tableMetas the metadata objects for one table
	 * @param key the key
	 * @return the metadata object or null
	 */
	public static TableMetadata findKey(List<TableMetadata> tableMetas,
			String key) {
		for (TableMetadata meta : tableMetas) {
			if (meta.getKey().equals(key))
				return meta;
		}
		return null;
	}
}
