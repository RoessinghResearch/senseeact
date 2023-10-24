package nl.rrd.senseeact.dao;

import nl.rrd.utils.exception.DatabaseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a special database table that is used internally in {@link Database
 * Database}. It can define metadata about a database table (including itself).
 * It stores a version number for each table, to support automatic database
 * upgrades, as well as other metadata. In the database it's stored in a table
 * with name "_table_metadata".
 * 
 * @author Dennis Hofs (RRD)
 */
public class TableMetadataTableDef extends DatabaseTableDef<TableMetadata> {
	public static final String NAME = "_table_metadata";
	private static final int VERSION = 3;
	
	public TableMetadataTableDef() {
		super(NAME, TableMetadata.class, VERSION, false);
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

	public int upgradeTableV0(Database db, String physTable)
			throws DatabaseException {
		List<Map<String,?>> maps = db.selectMaps(physTable, null, null, 0,
				null);
		List<Map<String,Object>> insertMaps = new ArrayList<>();
		for (Map<String,?> map : maps) {
			Map<String,Object> insertMap = new LinkedHashMap<>();
			for (String key : map.keySet()) {
				insertMap.put(key, map.get(key));
			}
			insertMaps.add(insertMap);
		}
		db.delete(physTable, null, null);
		db.dropColumn(physTable, "value");
		db.addColumn(physTable, new DatabaseColumnDef("value", DatabaseType.TEXT));
		db.insertMaps(physTable, insertMaps);
		return 1;
	}
	
	public int upgradeTableV1(Database db, String physTable)
			throws DatabaseException {
		List<Map<String,?>> maps = db.selectMaps(physTable, null, null, 0,
				null);
		List<Map<String,Object>> insertMaps = new ArrayList<>();
		for (Map<String,?> map : maps) {
			Map<String,Object> insertMap = new LinkedHashMap<>();
			for (String key : map.keySet()) {
				insertMap.put(key, map.get(key));
			}
			insertMaps.add(insertMap);
		}
		db.delete(physTable, null, null);
		db.insertMaps(physTable, insertMaps);
		return 2;
	}
	
	public int upgradeTableV2(Database db, String physTable)
			throws DatabaseException {
		DatabaseIndex index = new DatabaseIndex("table", "table");
		db.createIndex(physTable, index);
		return 3;
	}
}
