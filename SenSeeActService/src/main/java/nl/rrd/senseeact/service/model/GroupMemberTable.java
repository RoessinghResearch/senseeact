package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class GroupMemberTable extends DatabaseTableDef<GroupMember> {
	public static final String NAME = "groupmember";
	
	private static final int VERSION = 1;

	public GroupMemberTable() {
		super(NAME, GroupMember.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		if (version == 0)
			return upgradeTableV0(db, physTable);
		else
			return 1;
	}

	private int upgradeTableV0(Database db, String physTable)
			throws DatabaseException {
		db.renameColumn(physTable, "userEmail", "user");
		return 1;
	}
}
