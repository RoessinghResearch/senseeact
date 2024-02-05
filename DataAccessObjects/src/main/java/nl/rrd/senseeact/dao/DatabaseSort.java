package nl.rrd.senseeact.dao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This class defines how a set of database records should be sorted on a
 * column when the records are retrieved. A list of instances indicates that
 * the records should first be sorted on the first column, then on the second
 * column and so on.
 * 
 * <p>Note that string comparisons are sensitive to case and diacritics. This
 * is normal in MongoDB and SQLite, but different than the default in
 * MariaDB.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class DatabaseSort {
	private String column = null;
	private boolean ascending = true;

	/**
	 * This default constructor is used for JSON serialization.
	 */
	public DatabaseSort() {
	}
	
	/**
	 * Constructs a new instance. Note that string comparisons are sensitive to
	 * case and diacritics. This is normal in MongoDB and SQLite, but different
	 * than the default in MariaDB.
	 * 
	 * @param column the column name
	 * @param ascending true if the column should be sorted in ascending order,
	 * false if it should be sorted in descending order
	 */
	public DatabaseSort(String column, boolean ascending) {
		this.column = column;
		this.ascending = ascending;
	}
	
	/**
	 * Returns the column name.
	 * 
	 * @return the column name
	 */
	public String getColumn() {
		return column;
	}
	
	/**
	 * Sets the column name.
	 * 
	 * @param column the colum name
	 */
	public void setColumn(String column) {
		this.column = column;
	}

	/**
	 * Returns whether the column should be sorted in ascending order.
	 * 
	 * @return true if the column should be sorted in ascending order, false if
	 * it should be sorted in descending order
	 */
	public boolean isAscending() {
		return ascending;
	}

	/**
	 * Sets whether the column should be sorted in ascending order.
	 * 
	 * @param ascending true if the column should be sorted in ascending order,
	 * false if it should be sorted in descending order
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}
	
	@Override
	public String toString() {
		return column + " " + (ascending ? "ASC" : "DESC");
	}
	
	public static DatabaseSort[] reverse(DatabaseSort[] sort) {
		DatabaseSort[] result = new DatabaseSort[sort.length];
		for (int i = 0; i < result.length; i++) {
			DatabaseSort orig = sort[i];
			result[i] = new DatabaseSort(orig.column, !orig.ascending);
		}
		return result;
	}
}
