package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class QuestionnaireSchedule {
	private String questionnaire;
	private DateTimeSchedule schedule;

	public QuestionnaireSchedule() {
	}

	public QuestionnaireSchedule(String questionnaire,
			DateTimeSchedule schedule) {
		this.questionnaire = questionnaire;
		this.schedule = schedule;
	}

	public String getQuestionnaire() {
		return questionnaire;
	}

	public DateTimeSchedule getSchedule() {
		return schedule;
	}
}
