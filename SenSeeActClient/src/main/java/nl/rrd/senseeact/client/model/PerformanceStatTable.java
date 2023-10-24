package nl.rrd.senseeact.client.model;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseTableDef;

public class PerformanceStatTable extends DatabaseTableDef<PerformanceStat> {
	public static final String NAME = "performance_stats";
	
	private static final int VERSION = 0;

	public PerformanceStatTable() {
		super(NAME, PerformanceStat.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
