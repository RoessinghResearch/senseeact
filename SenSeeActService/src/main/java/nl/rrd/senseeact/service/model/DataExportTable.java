package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.utils.exception.DatabaseException;

public class DataExportTable extends DatabaseTableDef<DataExportRecord> {
	public static final String NAME = "data_exports";

	private static final int VERSION = 0;

	public DataExportTable() {
		super(NAME, DataExportRecord.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
