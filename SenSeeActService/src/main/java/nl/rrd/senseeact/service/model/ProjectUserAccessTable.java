package nl.rrd.senseeact.service.model;

import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class ProjectUserAccessTable extends
		DatabaseTableDef<ProjectUserAccessRecord> {
	public static final String NAME = "project_user_access_rules";

	private static final int VERSION = 0;

	public ProjectUserAccessTable() {
		super(NAME, ProjectUserAccessRecord.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
