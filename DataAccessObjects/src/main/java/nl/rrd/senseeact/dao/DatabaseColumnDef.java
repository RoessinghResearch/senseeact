package nl.rrd.senseeact.dao;

/**
 * The definition of a database column.
 * 
 * @author Dennis Hofs (RRD)
 */
public class DatabaseColumnDef {
	private String name;
	private DatabaseType type;
	private boolean index = false;
	
	/**
	 * Constructs a new instance.
	 * 
	 * @param name the column name
	 * @param type the data type
	 */
	public DatabaseColumnDef(String name, DatabaseType type) {
		this.name = name;
		this.type = type;
	}
	
	/**
	 * Constructs a new instance. You may set "index" to true, so the database
	 * will create an index to make select queries run faster.
	 * 
	 * @param name the column name the column name
	 * @param type the data type the data type
	 * @param index true if the database should create an index, false
	 * otherwise
	 */
	public DatabaseColumnDef(String name, DatabaseType type, boolean index) {
		this.name = name;
		this.type = type;
		this.index = index;
	}
	
	/**
	 * Returns the column name.
	 * 
	 * @return the column name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the data type.
	 * 
	 * @return the data type
	 */
	public DatabaseType getType() {
		return type;
	}

	/**
	 * Returns whether the database should create an index on this column to
	 * make select queries run faster. The default is false.
	 * 
	 * @return true if the database should create an index, false otherwise
	 */
	public boolean isIndex() {
		return index;
	}

	/**
	 * Sets whether the database should create an index on this column to make
	 * select queries run faster. The default is false.
	 * 
	 * @param index true if the database should create an index, false
	 * otherwise
	 */
	public void setIndex(boolean index) {
		this.index = index;
	}
}
