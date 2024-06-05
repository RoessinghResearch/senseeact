package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.validation.ValidateNotEmpty;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;
import org.xml.sax.Attributes;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Questionnaire {
	@ValidateNotEmpty
	private String id;
	private boolean randomOrder = false;
	private List<Question> questions = new ArrayList<>();

	private Random random = new Random();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isRandomOrder() {
		return randomOrder;
	}

	public void setRandomOrder(boolean randomOrder) {
		this.randomOrder = randomOrder;
	}

	public List<Question> getQuestions() {
		return questions;
	}

	public void setQuestions(List<Question> questions) {
		this.questions = questions;
	}

	public void addQuestion(Question question) {
		questions.add(question);
	}

	public Question findCurrentQuestion(String questionId,
			Map<String,Object> answerMap) {
		int index = findCurrentQuestionIndex(questionId, answerMap);
		if (index == -1)
			return null;
		else
			return questions.get(index);
	}

	private int findCurrentQuestionIndex(String questionId,
			Map<String,Object> answerMap) {
		for (int i = 0; i < questions.size(); i++) {
			Question question = questions.get(i);
			if (question.getDataIds(answerMap).contains(questionId))
				return i;
		}
		return -1;
	}

	public String findStartQuestionId() {
		if (randomOrder)
			return findRandomStartQuestionId();
		else
			return findOrderedStartQuestionId();
	}

	private String findOrderedStartQuestionId() {
		for (Question question : questions) {
			List<String> dataIds = question.getDataIds(null);
			if (!dataIds.isEmpty())
				return dataIds.get(0);
		}
		return null;
	}

	private String findRandomStartQuestionId() {
		List<String> candidates = new ArrayList<>();
		for (Question question : questions) {
			String dataId = question.getCurrentDataId(null);
			if (dataId != null && !candidates.contains(dataId))
				candidates.add(dataId);
		}
		if (candidates.isEmpty())
			return null;
		return candidates.get(random.nextInt(candidates.size()));
	}

	public String findDefaultNextQuestionId(QuestionnaireData qnData,
			Map<String,?> currAnswerData) {
		String questionId = qnData.getCurrentQuestionId();
		Map<String, Object> newAnswerMap = new LinkedHashMap<>(
				qnData.getAnswerMap());
		newAnswerMap.put(questionId, currAnswerData);
		if (randomOrder)
			return findRandomNextQuestionId(newAnswerMap);
		else
			return findOrderedNextQuestionId(qnData, newAnswerMap);
	}

	private String findOrderedNextQuestionId(QuestionnaireData qnData,
			Map<String,Object> newAnswerMap) {
		int index = findCurrentQuestionIndex(qnData.getCurrentQuestionId(),
				qnData.getAnswerMap());
		if (index == -1)
			return null;
		while (index < questions.size()) {
			Question question = questions.get(index);
			String dataId = question.getCurrentDataId(newAnswerMap);
			if (dataId != null)
				return dataId;
			index++;
		}
		return null;
	}

	private String findRandomNextQuestionId(Map<String,Object> newAnswerMap) {
		List<String> candidates = new ArrayList<>();
		for (Question question : questions) {
			String dataId = question.getCurrentDataId(newAnswerMap);
			if (dataId != null &&
					!newAnswerMap.containsKey(dataId) &&
					!candidates.contains(dataId)) {
				candidates.add(dataId);
			}
		}
		if (candidates.isEmpty())
			return null;
		return candidates.get(random.nextInt(candidates.size()));
	}

	public String findNextQuestionId(String nextId, QuestionnaireData qnData,
			Map<String,?> currAnswerData) {
		String questionId = qnData.getCurrentQuestionId();
		Map<String, Object> newAnswerMap = new LinkedHashMap<>(
				qnData.getAnswerMap());
		newAnswerMap.put(questionId, currAnswerData);
		int index = findCurrentQuestionIndex(nextId, newAnswerMap);
		while (index < questions.size()) {
			Question question = questions.get(index);
			String dataId = question.getCurrentDataId(newAnswerMap);
			if (dataId != null)
				return dataId;
			index++;
		}
		return null;
	}

	public static SimpleSAXHandler<Questionnaire> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<Questionnaire> {
		private Questionnaire result;
		private int rootLevel = -1;
		private SimpleSAXHandler<? extends Question> questionHandler = null;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startQuestionnaire(atts);
			} else if (parents.size() == rootLevel + 1) {
				questionHandler = Question.getXMLHandler();
			}
			if (questionHandler != null)
				questionHandler.startElement(name, atts, parents);
		}

		private void startQuestionnaire(Attributes atts) throws ParseException {
			result = new Questionnaire();
			if (atts.getValue("random_order") != null)
				result.randomOrder = readBooleanAttribute(atts, "random_order");
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
			if (questionHandler != null)
				questionHandler.endElement(name, parents);
			if (parents.size() == rootLevel + 1) {
				result.addQuestion(questionHandler.getObject());
				questionHandler = null;
			}
		}

		@Override
		public void characters(String ch, List<String> parents)
				throws ParseException {
			if (questionHandler != null)
				questionHandler.characters(ch, parents);
			else if (!ch.trim().isEmpty())
				throw new ParseException("Unexpected text content");
		}

		@Override
		public Questionnaire getObject() {
			return result;
		}
	}
}
