package nl.rrd.senseeact.client.model.questionnaire;

import nl.rrd.senseeact.dao.AbstractDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

public class QuestionnaireRecord extends AbstractDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING)
	private String name;

	@DatabaseField(value=DatabaseType.TEXT)
	private String questionnaireXml;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getQuestionnaireXml() {
		return questionnaireXml;
	}

	public void setQuestionnaireXml(String questionnaireXml) {
		this.questionnaireXml = questionnaireXml;
	}
}
