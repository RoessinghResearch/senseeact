package nl.rrd.senseeact.service.model;

import java.util.LinkedHashMap;
import java.util.Map;

import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseColumnDef;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.senseeact.dao.DatabaseType;

public class UserProjectTable extends DatabaseTableDef<UserProject> {
	public static final String NAME = "userproject";

	private static final int VERSION = 2;
	
	public UserProjectTable() {
		super(NAME, UserProject.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		if (version == 0)
			return upgradeTableV0(db, physTable);
		else if (version == 1)
			return upgradeTableV1(db, physTable);
		else
			return 2;
	}
	
	private int upgradeTableV0(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("asRole",
				DatabaseType.STRING));
		Map<String,String> values = new LinkedHashMap<>();
		values.put("asRole", Role.PATIENT.toString());
		db.update(physTable, null, null, values);
		return 1;
	}

	private int upgradeTableV1(Database db, String physTable)
			throws DatabaseException {
		db.renameColumn(physTable, "userEmail", "user");
		return 2;
	}
}
