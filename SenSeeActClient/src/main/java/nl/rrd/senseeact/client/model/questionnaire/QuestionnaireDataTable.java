package nl.rrd.senseeact.client.model.questionnaire;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseColumnDef;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseSort;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.senseeact.dao.DatabaseType;

public class QuestionnaireDataTable extends
		DatabaseTableDef<QuestionnaireData> {
	public static final String NAME = "questionnaire_data";

	private static final int VERSION = 1;

	public QuestionnaireDataTable() {
		super(NAME, QuestionnaireData.class, VERSION, true);
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
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("utcTime", true)
		};
		List<Map<String,?>> maps = db.selectMaps(physTable, null, null, 0,
				sort);
		db.dropColumn(physTable, "newanswers");
		db.addColumn(physTable, new DatabaseColumnDef("newanswers",
				DatabaseType.TEXT));
		for (Map<String,?> map : maps) {
			DatabaseCriteria criteria = new DatabaseCriteria.Equal(
					"id", (String)map.get("id"));
			Map<String,Object> values = new LinkedHashMap<>();
			values.put("newanswers", map.get("answers"));
			db.update(physTable, null, criteria, values);
		}
		db.renameColumn(physTable, "answers", "oldanswers");
		db.renameColumn(physTable, "newanswers", "answers");
		db.dropColumn(physTable, "oldanswers");
		return 1;
	}
}
