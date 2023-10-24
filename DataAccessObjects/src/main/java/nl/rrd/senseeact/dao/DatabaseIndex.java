package nl.rrd.senseeact.dao;

import nl.rrd.utils.json.JsonObject;

/**
 * A compound index defines a table index on multiple fields.
 * 
 * @author Dennis Hofs (RRD)
 */
public class DatabaseIndex extends JsonObject {
	private String name;
	private String[] fields;
	
	/**
	 * Constructs a new compound index that should still be initialized. This
	 * constructor is used for JSON object mapping.
	 */
	public DatabaseIndex() {
	}

	/**
	 * Constructs a new compound index.
	 * 
	 * @param name the index name
	 * @param fields the field names
	 */
	public DatabaseIndex(String name, String... fields) {
		this.name = name;
		this.fields = fields;
	}
	
	/**
	 * Returns the index name.
	 * 
	 * @return the index name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the index name.
	 * 
	 * @param name the index name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the field names.
	 * 
	 * @return the field names
	 */
	public String[] getFields() {
		return fields;
	}

	/**
	 * Sets the field names.
	 * 
	 * @param fields the field names
	 */
	public void setFields(String[] fields) {
		this.fields = fields;
	}
}
