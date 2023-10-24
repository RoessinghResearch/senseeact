package nl.rrd.senseeact.client.model.sample;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseIndex;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.senseeact.dao.UpgradeTableSplitByUser;

public class UserInteractionEventTable extends
DatabaseTableDef<UserInteractionEvent> {
	public static final String NAME = "user_interaction";

	private static final int VERSION = 3;

	public UserInteractionEventTable() {
		super(NAME, UserInteractionEvent.class, VERSION, true);
	}
	
	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		if (version == 0)
			return upgradeTableV0(db, physTable);
		else if (version == 1)
			return upgradeTableV1(db, physTable);
		else if (version == 2)
			return upgradeTableV2(db, physTable);
		else
			return 3;
	}

	private int upgradeTableV0(Database db, String physTable)
			throws DatabaseException {
		db.dropIndex(physTable, "localTime");
		return 1;
	}
	
	private int upgradeTableV1(Database db, String physTable)
			throws DatabaseException {
		if (!physTable.equals(NAME))
			return 2;
		UpgradeTableSplitByUser.upgradeSplit(db, this);
		return 2;
	}

	private int upgradeTableV2(Database db, String physTable)
			throws DatabaseException {
		db.createIndex(physTable, new DatabaseIndex("localTime", "localTime"));
		return 3;
	}
}
