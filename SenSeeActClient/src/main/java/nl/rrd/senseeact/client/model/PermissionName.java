package nl.rrd.senseeact.client.model;

public class PermissionName {
	/**
	 * This permission allows users to write records to a project table without
	 * a user field. This kind of tables is used for general project resources.
	 *
	 * Parameters:
	 * - project: project code
	 * - table: table name (wildcard * for all tables)
	 */
	public static final String PERMISSION_WRITE_RESOURCE_TABLE =
			"write_resource_table";
}
