package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.senseeact.client.model.sample.ResourceUTCSample;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * This record defines a list of schedules for a questionnaire. The time
 * indicates the time when the record was published from the questionnaire
 * editor.
 *
 * @author Dennis Hofs (RRD)
 */
public class QuestionnaireSchedulesRecord extends ResourceUTCSample {
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String questionnaire;

	@DatabaseField(value=DatabaseType.TEXT, json=true)
	private String schedules;
	private List<DateTimeSchedule> schedulesList = new ArrayList<>();

	/**
	 * Constructs a new empty record. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public QuestionnaireSchedulesRecord() {
	}

	/**
	 * Constructs a new record at the specified time. It should define the
	 * local time and location-based time zone (not an offset).
	 *
	 * @param tzTime the time
	 */
	public QuestionnaireSchedulesRecord(ZonedDateTime tzTime) {
		super(tzTime);
	}

	/**
	 * Returns the name of the questionnaire that should be triggered by the
	 * schedules in this record.
	 *
	 * @return the name of the questionnaire
	 */
	public String getQuestionnaire() {
		return questionnaire;
	}

	/**
	 * Sets the name of the questionnaire that should be triggered by the
	 * schedules in this record.
	 *
	 * @param questionnaire the name of the questionnaire
	 */
	public void setQuestionnaire(String questionnaire) {
		this.questionnaire = questionnaire;
	}

	/**
	 * Returns the JSON code for the schedules. This method is used for the DAO.
	 * Users can call {@link #getSchedulesList() getSchedulesList()}.
	 *
	 * @return the JSON code for the schedule
	 */
	public String getSchedules() {
		return JsonMapper.generate(schedulesList);
	}

	/**
	 * Sets the JSON code for the schedule. This method is used for the DAO.
	 * Users can call {@link #setSchedulesList(List) setSchedulesList()}.
	 *
	 * @param schedules the JSON code for the schedule
	 * @throws ParseException if the JSON code is invalid
	 */
	public void setSchedules(String schedules) throws ParseException {
		schedulesList = JsonMapper.parse(schedules, new TypeReference<>() {});
	}

	/**
	 * Returns the schedules.
	 *
	 * @return the schedules
	 */
	public List<DateTimeSchedule> getSchedulesList() {
		return schedulesList;
	}

	/**
	 * Sets the schedules.
	 *
	 * @param schedulesList the schedules
	 */
	public void setSchedulesList(List<DateTimeSchedule> schedulesList) {
		this.schedulesList = schedulesList;
	}

	public List<QuestionnaireSchedule> toQuestionnaireSchedules() {
		List<QuestionnaireSchedule> result = new ArrayList<>();
		for (DateTimeSchedule schedule : schedulesList) {
			result.add(new QuestionnaireSchedule(questionnaire, schedule));
		}
		return result;
	}
}
