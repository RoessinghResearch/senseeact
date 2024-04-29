package nl.rrd.senseeact.client.model.questionnaire;

import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.utils.exception.DatabaseException;

public class QuestionnaireSchedulesTable extends
		DatabaseTableDef<QuestionnaireSchedulesRecord> {
	public static final String NAME = "qn_schedules";

	private static final int VERSION = 0;

	public QuestionnaireSchedulesTable() {
		super(NAME, QuestionnaireSchedulesRecord.class, VERSION, false);
	}

	@Override
	public int upgradeTable(int version, Database db, String physTable)
			throws DatabaseException {
		return 0;
	}
}
