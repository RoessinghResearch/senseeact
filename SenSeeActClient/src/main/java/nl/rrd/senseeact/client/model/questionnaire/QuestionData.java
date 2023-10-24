package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import nl.rrd.utils.json.DateTimeFromIsoDateTimeDeserializer;
import nl.rrd.utils.json.IsoDateTimeSerializer;

/**
 * This class models the answer that a user gave to a question.
 *
 * @author Dennis Hofs (RRD)
 */
public class QuestionData {
	@JsonSerialize(using=IsoDateTimeSerializer.class)
	@JsonDeserialize(using=DateTimeFromIsoDateTimeDeserializer.class)
	private ZonedDateTime time;
	private String questionId;
	private Map<String,?> data = new LinkedHashMap<>();

	/**
	 * Returns the time when the question was answered.
	 *
	 * @return the time when the question was answered
	 */
	public ZonedDateTime getTime() {
		return time;
	}

	/**
	 * Sets the time when the question was answered.
	 *
	 * @param time the time when the question was answered
	 */
	public void setTime(ZonedDateTime time) {
		this.time = time;
	}

	/**
	 * Returns the question ID.
	 *
	 * @return the question ID
	 */
	public String getQuestionId() {
		return questionId;
	}

	/**
	 * Sets the question ID.
	 *
	 * @param questionId the question ID
	 */
	public void setQuestionId(String questionId) {
		this.questionId = questionId;
	}

	/**
	 * Returns the answer data.
	 *
	 * @return the answer data
	 */
	public Map<String,?> getData() {
		return data;
	}

	/**
	 * Sets the answer data.
	 *
	 * @param data the answer data
	 */
	public void setData(Map<String,?> data) {
		this.data = data;
	}
}
