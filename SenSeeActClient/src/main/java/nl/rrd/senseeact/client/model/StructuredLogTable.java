package nl.rrd.senseeact.client.model;

import nl.rrd.senseeact.client.model.sample.ObjectMapSample;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.utils.exception.DatabaseException;

/** Stores logs in a structured manner. Standard fields are "tag" and "message".
 */
public class StructuredLogTable extends DatabaseTableDef<ObjectMapSample> {
	public static final String NAME = "structured_log";

	private static final int VERSION = 0;

	public StructuredLogTable() {
		super(NAME, ObjectMapSample.class, VERSION, true);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}

}
