package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import nl.rrd.senseeact.client.model.notification.AppNotification;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;
import nl.rrd.utils.xml.SimpleSAXParser;

public class QuestionnaireNotificationData {
	private String questionnaireId = null;
	private Questionnaire questionnaire = null;
	private Map<String,Map<String,String>> stringMaps = new LinkedHashMap<>();

	public String getQuestionnaireId() {
		return questionnaireId;
	}

	public void setQuestionnaireId(String questionnaireId) {
		this.questionnaireId = questionnaireId;
	}

	public Questionnaire getQuestionnaire() {
		return questionnaire;
	}

	public void setQuestionnaire(Questionnaire questionnaire) {
		this.questionnaire = questionnaire;
	}

	public Map<String, Map<String, String>> getStringMaps() {
		return stringMaps;
	}

	public void setStringMaps(Map<String, Map<String, String>> stringMaps) {
		this.stringMaps = stringMaps;
	}

	public static QuestionnaireNotificationData readData(
			AppNotification notif) throws ParseException {
		QuestionnaireNotificationData data =
				new QuestionnaireNotificationData();
		String dataStr = notif.getData();
		if (dataStr == null)
			throw new ParseException("Questionnaire notification data is null");
		if (!dataStr.startsWith("{")) {
			data.setQuestionnaireId(dataStr);
			return data;
		}
		Map<?,?> map;
		try {
			map = JsonMapper.parse(dataStr, Map.class);
		} catch (ParseException ex) {
			throw new ParseException(
					"Can't parse questionnaire notification data as JSON object: " +
					ex.getMessage(), ex);
		}
		String questionnaireId = (String)map.get("questionnaireId");
		data.setQuestionnaireId(questionnaireId);
		String xml = (String)map.get("questionnaire");
		if (xml != null) {
			SimpleSAXParser<Questionnaire> parser = new SimpleSAXParser<>(
					Questionnaire.getXMLHandler());
			Questionnaire qn;
			try {
				qn = parser.parse(xml);
			} catch (ParseException | IOException ex) {
				throw new ParseException("Can't read questionnaire XML: " +
						ex.getMessage(), ex);
			}
			qn.setId(questionnaireId);
			data.setQuestionnaire(qn);
		}
		Map<String,Map<String,String>> stringMaps;
		try {
			stringMaps = JsonMapper.convert(map.get("stringMaps"),
					new TypeReference<>() {});
		} catch (ParseException ex) {
			throw new ParseException(
					"Invalid value for property \"stringMaps\": " +
					ex.getMessage(), ex);
		}
		if (stringMaps != null)
			data.setStringMaps(stringMaps);
		return data;
	}
}
