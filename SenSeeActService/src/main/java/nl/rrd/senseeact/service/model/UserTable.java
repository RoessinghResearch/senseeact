package nl.rrd.senseeact.service.model;

import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserTable extends DatabaseTableDef<User> {
	public static final String NAME = "user";
	
	private static final int VERSION = 15;

	public UserTable() {
		super(NAME, User.class, VERSION, false);
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
		else if (version == 3)
			return upgradeTableV3(db, physTable);
		else if (version == 4)
			return upgradeTableV4(db, physTable);
		else if (version == 5)
			return upgradeTableV5(db, physTable);
		else if (version == 6)
			return upgradeTableV6(db, physTable);
		else if (version == 7)
			return upgradeTableV7(db, physTable);
		else if (version == 8)
			return upgradeTableV8(db, physTable);
		else if (version == 9)
			return upgradeTableV9(db, physTable);
		else if (version == 10)
			return upgradeTableV10(db, physTable);
		else if (version == 11)
			return upgradeTableV11(db, physTable);
		else if (version == 12)
			return upgradeTableV12(db, physTable);
		else if (version == 13)
			return upgradeTableV13(db, physTable);
		else if (version == 14)
			return upgradeTableV14(db, physTable);
		else
			return 15;
	}
	
	private int upgradeTableV0(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("timeZone",
				DatabaseType.STRING));
		return 1;
	}
	
	private int upgradeTableV1(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("altEmail",
				DatabaseType.STRING));
		return 2;
	}
	
	private int upgradeTableV2(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("created",
				DatabaseType.ISOTIME));
		return 3;
	}
	
	private int upgradeTableV3(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef(
				"resetPasswordRequestCode", DatabaseType.STRING));
		db.addColumn(physTable, new DatabaseColumnDef(
				"resetPasswordRequestTime", DatabaseType.ISOTIME));
		return 4;
	}
	
	private int upgradeTableV4(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef(
				"status", DatabaseType.STRING));
		return 5;
	}
	
	private int upgradeTableV5(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef(
				"active", DatabaseType.BYTE));
		Map<String,Object> values = new LinkedHashMap<>();
		values.put("active", 1);
		db.update(physTable, null, null, values);
		return 6;
	}

	private int upgradeTableV6(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("fullName",
				DatabaseType.STRING));
		db.addColumn(physTable, new DatabaseColumnDef("nickName",
				DatabaseType.STRING));
		return 7;
	}

	private int upgradeTableV7(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("languageFormality",
				DatabaseType.STRING));
		return 8;
	}

	private int upgradeTableV8(Database db, String physTable)
			throws DatabaseException {
		db.dropColumn(physTable, "userid");
		db.addColumn(physTable, new DatabaseColumnDef("userid",
				DatabaseType.STRING, true));
		List<Map<String,?>> maps = db.selectMaps(physTable, null, null, 0,
				null);
		for (Map<String,?> map : maps) {
			String id = (String)map.get("id");
			String email = (String)map.get("email");
			DatabaseCriteria criteria = new DatabaseCriteria.Equal("id", id);
			Map<String,Object> values = new LinkedHashMap<>();
			values.put("userid", email);
			values.put("email", email.toLowerCase());
			db.update(physTable, null, criteria, values);
		}
		return 9;
	}

	private int upgradeTableV9(Database db, String physTable)
			throws DatabaseException {
		db.renameColumn(physTable, "deathDate", "deceasedDate");
		return 10;
	}

	private int upgradeTableV10(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("emailVerified",
				DatabaseType.BYTE));
		db.addColumn(physTable, new DatabaseColumnDef("hasTemporaryPassword",
				DatabaseType.BYTE));
		db.addColumn(physTable, new DatabaseColumnDef("verifyEmailRequestCode",
				DatabaseType.STRING));
		db.addColumn(physTable, new DatabaseColumnDef("verifyEmailRequestTime",
				DatabaseType.ISOTIME));
		return 11;
	}

	private int upgradeTableV11(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("hasTemporaryEmail",
				DatabaseType.BYTE));
		Map<String,Object> values = new LinkedHashMap<>();
		values.put("emailVerified", 0);
		values.put("hasTemporaryEmail", 0);
		values.put("hasTemporaryPassword", 0);
		db.update(physTable, User.class, null, values);
		return 12;
	}

	private int upgradeTableV12(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef(
				"emailPendingVerification", DatabaseType.STRING));
		return 13;
	}

	private int upgradeTableV13(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("lastActive",
				DatabaseType.ISOTIME));
		List<Map<String,?>> users = db.selectMaps(physTable, null, null, 0,
				null);
		for (Map<String,?> user : users) {
			String created = (String)user.get("created");
			String id = (String)user.get("id");
			DatabaseCriteria criteria = new DatabaseCriteria.Equal("id", id);
			Map<String,Object> values = new LinkedHashMap<>();
			values.put("lastActive", created);
			db.update(physTable, null, criteria, values);
		}
		return 14;
	}

	private int upgradeTableV14(Database db, String physTable)
			throws DatabaseException {
		db.addColumn(physTable, new DatabaseColumnDef("mfa",
				DatabaseType.TEXT));
		Map<String,Object> values = new LinkedHashMap<>();
		values.put("mfa", "{}");
		db.update(physTable, null, null, values);
		return 15;
	}
}
