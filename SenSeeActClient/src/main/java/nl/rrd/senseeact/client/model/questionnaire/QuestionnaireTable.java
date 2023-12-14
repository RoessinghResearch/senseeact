package nl.rrd.senseeact.client.model.questionnaire;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.utils.exception.DatabaseException;

public class QuestionnaireTable extends DatabaseTableDef<QuestionnaireRecord> {
	public static final String NAME = "questionnaires";

	private static final int VERSION = 0;

	public QuestionnaireTable() {
		super(NAME, QuestionnaireRecord.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
