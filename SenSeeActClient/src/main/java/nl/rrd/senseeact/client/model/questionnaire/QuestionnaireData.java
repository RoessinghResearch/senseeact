package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;
import nl.rrd.senseeact.client.model.sample.UTCSample;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class models the answers that the user gave in a questionnaire, and the
 * current questionnaire state. The time of this sample indicates the time when
 * the questionnaire was first shown.
 *
 * @author Dennis Hofs (RRD)
 */
public class QuestionnaireData extends UTCSample {
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String questionnaireId;

	@DatabaseField(value=DatabaseType.DATETIME, index=true)
	private LocalDateTime scheduledTime = null;

	@DatabaseField(value=DatabaseType.STRING)
	private String currentQuestionId;

	// JSON code for answerMap
	@DatabaseField(value=DatabaseType.TEXT)
	@JsonIgnore
	private String answers;

	@JsonProperty("answers")
	private List<QuestionData> answerList = new ArrayList<>();

	/**
	 * Constructs new questionnaire data. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public QuestionnaireData() {
	}

	/**
	 * Constructs new questionnaire data at the specified time. It should define
	 * the local time and location-based time zone (not an offset).
	 *
	 * @param user the user (user ID)
	 * @param tzTime the time
	 */
	public QuestionnaireData(String user, ZonedDateTime tzTime) {
		super(user, tzTime);
	}

	/**
	 * Returns the questionnaire ID.
	 *
	 * @return the questionnaire ID
	 */
	public String getQuestionnaireId() {
		return questionnaireId;
	}

	/**
	 * Sets the questionnaire ID.
	 *
	 * @param questionnaireId the questionnaire ID
	 */
	public void setQuestionnaireId(String questionnaireId) {
		this.questionnaireId = questionnaireId;
	}

	/**
	 * Returns the time when the questionnaire was scheduled to be started.
	 * The milliseconds are always 0. This method may return null.
	 *
	 * @return the time when the questionnaire was scheduled to be started or
	 * null
	 */
	public LocalDateTime getScheduledTime() {
		return scheduledTime;
	}

	/**
	 * Sets the time when the questionnaire was scheduled to be started. The
	 * milliseconds should be 0. This may be null.
	 *
	 * @param scheduledTime the time when the questionnaire was scheduled to be
	 * started or null
	 */
	public void setScheduledTime(LocalDateTime scheduledTime) {
		this.scheduledTime = scheduledTime;
	}

	/**
	 * Returns the current question ID. Initially it will be set to the ID of
	 * the first question. When the questionnaire is completed, it will be null.
	 *
	 * @return the current question ID or null
	 */
	public String getCurrentQuestionId() {
		return currentQuestionId;
	}

	/**
	 * Sets the current question ID. Initially it will be set to the ID of the
	 * first question. When the questionnaire is completed, it will be null.
	 *
	 * @param currentQuestionId the current question ID or null
	 */
	public void setCurrentQuestionId(String currentQuestionId) {
		this.currentQuestionId = currentQuestionId;
	}

	/**
	 * Returns the JSON code for the answer data. This method is used for the
	 * DAO. Users can call {@link #getAnswerList() getAnswerList()}.
	 *
	 * @return the JSON code for the answer data
	 */
	public String getAnswers() {
		return JsonMapper.generate(answerList);
	}

	/**
	 * Sets the JSON code for the answer data. This method is used for the DAO.
	 * Users can call {@link #setAnswerList(List) setAnswerList()}.
	 *
	 * @param answers the JSON code for the answer data
	 * @throws ParseException if an error occurs while parsing the JSON code
	 */
	public void setAnswers(String answers) throws ParseException {
		answerList = JsonMapper.parse(answers,
				new TypeReference<List<QuestionData>>() {});
	}

	/**
	 * Returns the answers that the user gave.
	 *
	 * @return the answers that the user gave
	 */
	public List<QuestionData> getAnswerList() {
		return answerList;
	}

	/**
	 * Sets the answers that the user gave.
	 *
	 * @param answerList the answers that the user gave
	 */
	public void setAnswerList(List<QuestionData> answerList) {
		this.answerList = answerList;
	}

	/**
	 * Returns the answers as a map from question ID to data map.
	 *
	 * @return the answer map
	 */
	public Map<String,Object> getAnswerMap() {
		Map<String,Object> result = new LinkedHashMap<>();
		for (QuestionData answer : answerList) {
			result.put(answer.getQuestionId(), answer.getData());
		}
		return result;
	}

	/**
	 * Tries to find the answer for the specified question ID. If there is no
	 * such answer, then this method returns null.
	 *
	 * @param questionId the question ID
	 * @return the answer or null
	 */
	public QuestionData findAnswer(String questionId) {
		for (QuestionData answer : answerList) {
			if (answer.getQuestionId().equals(questionId))
				return answer;
		}
		return null;
	}
}
