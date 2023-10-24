package nl.rrd.senseeact.client.model;

import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class AppSettingsTable extends DatabaseTableDef<AppSettings> {
	public static final String NAME = "app_settings";

	private static final int VERSION = 0;

	public AppSettingsTable() {
		super(NAME, AppSettings.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
