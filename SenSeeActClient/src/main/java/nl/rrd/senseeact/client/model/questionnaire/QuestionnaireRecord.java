package nl.rrd.senseeact.client.model.questionnaire;

import nl.rrd.senseeact.client.model.sample.ResourceUTCSample;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

import java.time.ZonedDateTime;

/**
 * This record defines a questionnaire definition. The time indicates the time
 * when the record was published from the questionnaire editor.
 *
 * @author Dennis Hofs (RRD)
 */
public class QuestionnaireRecord extends ResourceUTCSample {
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String name;

	@DatabaseField(value=DatabaseType.TEXT, json=true)
	private String questionnaire;
	private Questionnaire questionnaireObject;

	/**
	 * Constructs a new empty record. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public QuestionnaireRecord() {
	}

	/**
	 * Constructs a new record at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 *
	 * @param tzTime the time
	 */
	public QuestionnaireRecord(ZonedDateTime tzTime) {
		super(tzTime);
	}

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
