package nl.rrd.senseeact.dao;

import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.DatabaseException;
import org.slf4j.Logger;

import java.util.List;

/**
 * This is a special database table that is used internally in {@link Database
 * Database}. It doesn't contain any data, but it exists only to keep track of
 * the (shared) metadata and upgrades of the {@link DatabaseActionTable
 * DatabaseActionTable}s. Whenever the action tables need to be upgraded, you
 * should implement an upgrade in this table.
 *
 * @author Dennis Hofs (RRD)
 */
public class DatabaseActionMetaTable extends DatabaseTableDef<DatabaseAction> {
	public static final String NAME = "_action_log";

	private static final int VERSION = 9;

	public DatabaseActionMetaTable() {
		super(NAME, DatabaseAction.class, VERSION, false);
		addCompoundIndex(new DatabaseIndex("timeOrder", "time", "order"));
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		if (version < 7) {
			throw new DatabaseException(
					"Upgrade from version < 7 not supported");
		} else if (version == 7) {
			return upgradeTableV7(db, physTable);
		} else if (version == 8) {
			return upgradeTableV8(db, physTable);
		} else {
			return 9;
		}
	}
	
	private int upgradeTableV7(Database db, String physTable)
			throws DatabaseException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal(
				"key", TableMetadata.KEY_VERSION);
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("table", true)
		};
		List<TableMetadata> metas = db.select(new TableMetadataTableDef(),
				criteria, 0, sort);
		DatabaseCache cache = DatabaseCache.getInstance();
		for (TableMetadata meta : metas) {
			if (meta.getTable().startsWith(DatabaseActionTable.NAME_PREFIX)) {
				criteria = new DatabaseCriteria.Equal("table", meta.getTable());
				db.delete(TableMetadataTableDef.NAME, null, criteria);
				cache.removeLogicalTable(db, meta.getTable());
				cache.addPhysicalTable(db, meta.getTable());
			}
		}
		return 8;
	}

	private int upgradeTableV8(Database db, String physTable)
			throws DatabaseException {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		try {
			db.createIndex(physTable, new DatabaseIndex("recordId", "recordId"));
			logger.info("Created index recordId on table " + physTable);
		} catch (DatabaseException ex) {
			logger.warn(
					"Failed to create index recordId, probably already exists: " +
					ex.getMessage());
		}
		return 9;
	}
}
