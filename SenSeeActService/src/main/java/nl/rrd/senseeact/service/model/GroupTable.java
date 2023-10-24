package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class GroupTable extends DatabaseTableDef<Group> {
	public static String NAME = "group";
	
	private static int VERSION = 0;

	public GroupTable() {
		super(NAME, Group.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
