package nl.rrd.senseeact.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.rrd.utils.json.JsonObject;

/**
 * This class defines synchronization restrictions for reading database actions
 * from the server or writing database actions to the server. It defines what
 * tables should be included or excluded. If you specify one or more include
 * tables, only those tables will be synchronized. Otherwise all tables are
 * synchronized. Excluded tables will never be synchronized, even if they are
 * listed in the include tables.
 *
 * @author Dennis Hofs (RRD)
 */
public class SyncTableRestriction extends JsonObject {
	private List<String> includeTables = new ArrayList<>();
	private List<String> excludeTables = new ArrayList<>();

	/**
	 * Returns the included tables.
	 *
	 * @return the included tables
	 */
	public List<String> getIncludeTables() {
		return includeTables;
	}

	/**
	 * Sets the included tables.
	 *
	 * @param includeTables the included tables
	 */
	@JsonProperty("includeTables")
	public void setIncludeTables(List<String> includeTables) {
		if (includeTables == null)
			this.includeTables = new ArrayList<>();
		else
			this.includeTables = includeTables;
	}

	/**
	 * Sets the included tables.
	 *
	 * @param includeTables the included tables
	 */
	public void setIncludeTables(String... includeTables) {
		this.includeTables = Arrays.asList(includeTables);
	}

	/**
	 * Adds an included table.
	 *
	 * @param table the included table
	 */
	public void addIncludeTable(String table) {
		includeTables.add(table);
	}

	/**
	 * Returns the excluded tables.
	 *
	 * @return the excluded tables
	 */
	public List<String> getExcludeTables() {
		return excludeTables;
	}

	/**
	 * Sets the excluded tables.
	 *
	 * @param excludeTables the excluded tables
	 */
	@JsonProperty("excludeTables")
	public void setExcludeTables(List<String> excludeTables) {
		if (excludeTables == null)
			this.excludeTables = new ArrayList<>();
		else
			this.excludeTables = excludeTables;
	}

	/**
	 * Sets the excluded tables.
	 *
	 * @param excludeTables the excluded tables
	 */
	public void setExcludeTables(String... excludeTables) {
		this.excludeTables = Arrays.asList(excludeTables);
	}

	/**
	 * Adds an excluded table.
	 *
	 * @param table the excluded table
	 */
	public void addExcludeTable(String table) {
		excludeTables.add(table);
	}
	
	/**
	 * Returns whether the specified table matches this restriction. The table
	 * should not be reserved and not be in the exclude tables. If there are
	 * include tables, the table must be in that list.
	 * 
	 * @param table the table
	 * @return true if this restriction matches the table, false otherwise
	 */
	public boolean matchesTable(String table) {
		if (table.startsWith("_"))
			return false;
		if (excludeTables != null && excludeTables.contains(table))
			return false;
		if (includeTables != null && !includeTables.isEmpty() &&
				!includeTables.contains(table)) {
			return false;
		}
		return true;
	}

	/**
	 * Creates a table restriction that includes all tables that matches this
	 * restriction or the specified other restriction. If no table matches,
	 * this method returns null. If all tables match, this method returns the
	 * default restriction.
	 *
	 * @param other the other table restriction
	 * @param projectTables the project tables (can be empty or null)
	 * @return the merged restriction
	 */
	public SyncTableRestriction mergeOr(SyncTableRestriction other,
			List<String> projectTables) {
		if (projectTables == null || projectTables.isEmpty())
			return null;
		List<String> includes = new ArrayList<>();
		for (String table : projectTables) {
			if (matchesTable(table) || other.matchesTable(table))
				includes.add(table);
		}
		if (includes.isEmpty())
			return null;
		SyncTableRestriction result = new SyncTableRestriction();
		if (includes.size() != projectTables.size())
			result.setIncludeTables(includes);
		return result;
	}
}
