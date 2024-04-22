package nl.rrd.senseeact.client.model.questionnaire;

import nl.rrd.senseeact.dao.AbstractDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

public class QuestionnaireRecord extends AbstractDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String name;

	@DatabaseField(value=DatabaseType.TEXT, json=true)
	private String questionnaire;
	private Questionnaire questionnaireObject;

	/**
	 * Returns the questionnaire name.
	 *
	 * @return the questionnaire name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the questionnaire name.
	 *
	 * @param name the questionnaire name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the JSON code for the questionnaire. This method is used for the
	 * DAO. Users can call {@link #getQuestionnaireObject()
	 * getQuestionnaireObject()}.
	 *
	 * @return the JSON code for the questionnaire
	 */
	public String getQuestionnaire() {
		return JsonMapper.generate(questionnaireObject);
	}

	/**
	 * Sets the JSON code for the questionnaire. This method is used for the
	 * DAO. Users can call {@link #setQuestionnaireObject(Questionnaire)
	 * setQuestionnaireObject()}.
	 *
	 * @param questionnaire the JSON code for the questionnaire
	 */
	public void setQuestionnaire(String questionnaire) throws ParseException {
		questionnaireObject = JsonMapper.parse(questionnaire,
				Questionnaire.class);
	}

	public Questionnaire getQuestionnaireObject() {
		return questionnaireObject;
	}

	public void setQuestionnaireObject(Questionnaire questionnaireObject) {
		this.questionnaireObject = questionnaireObject;
	}
}
