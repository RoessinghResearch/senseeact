package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.expressions.StringExpression;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonDeserialize(using=JsonDeserializer.None.class)
public class MultipleChoiceQuestion extends Question {
	private List<MultipleChoiceAnswer> answers = new ArrayList<>();

	public MultipleChoiceQuestion() {
		super(TYPE_MULTIPLE_CHOICE);
	}

	public List<MultipleChoiceAnswer> getAnswers() {
		return answers;
	}

	public void setAnswers(List<MultipleChoiceAnswer> answers) {
		this.answers = answers;
	}

	public void addAnswer(MultipleChoiceAnswer answer) {
		answers.add(answer);
	}

	public static SimpleSAXHandler<MultipleChoiceQuestion> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<MultipleChoiceQuestion> {
		private MultipleChoiceQuestion result;
		private int rootLevel = -1;
		private SimpleSAXHandler<MultipleChoiceAnswer> answerHandler = null;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startMultipleChoice(atts);
			} else if (parents.size() == rootLevel + 1) {
				if (!name.equals("answer")) {
					throw new ParseException(
							"Expected element \"answer\", found: " + name);
				}
				answerHandler = MultipleChoiceAnswer.getXMLHandler();
			}
			if (answerHandler != null)
				answerHandler.startElement(name, atts, parents);
		}

		private void startMultipleChoice(Attributes atts)
				throws ParseException {
			result = new MultipleChoiceQuestion();
			String idExpr = readAttribute(atts, "id", 1, null);
			try {
				result.setId(new StringExpression(idExpr));
			} catch (ParseException ex) {
				throw new ParseException("Invalid value of attribute \"id\": " +
						idExpr + ": " + ex.getMessage(), ex);
			}
			if (atts.getValue("title") != null)
				result.setTitle(readAttribute(atts, "title", 1, null));
			result.setQuestion(readAttribute(atts, "question", 1, null));
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
			if (answerHandler != null)
				answerHandler.endElement(name, parents);
			if (parents.size() == rootLevel + 1) {
				result.addAnswer(answerHandler.getObject());
				answerHandler = null;
			}
		}

		@Override
		public void characters(String ch, List<String> parents)
				throws ParseException {
			if (answerHandler != null)
				answerHandler.characters(ch, parents);
			else if (!ch.trim().isEmpty())
				throw new ParseException("Unexpected text content");
		}

		@Override
		public MultipleChoiceQuestion getObject() {
			return result;
		}
	}
}
