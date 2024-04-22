package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.xml.sax.Attributes;

import java.util.List;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MultipleChoiceAnswer {
	private String value;
	private String text;
	private NextQuestion nextQuestion = null;

	public MultipleChoiceAnswer() {
	}

	public MultipleChoiceAnswer(String value, String text) {
		this.value = value;
		this.text = text;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public NextQuestion getNextQuestion() {
		return nextQuestion;
	}

	public void setNextQuestion(NextQuestion nextQuestion) {
		this.nextQuestion = nextQuestion;
	}

	public static SimpleSAXHandler<MultipleChoiceAnswer> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<MultipleChoiceAnswer> {
		private MultipleChoiceAnswer result = null;
		private int rootLevel = -1;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startAnswer(atts);
			} else {
				throw new ParseException("Unexpected element: " + name);
			}
		}

		private void startAnswer(Attributes atts) throws ParseException {
			String value = readAttribute(atts, "value", 1, null);
			String text = readAttribute(atts, "text", 1, null);
			result = new MultipleChoiceAnswer();
			result.value = value;
			result.text = text;
			String next = atts.getValue("next");
			if (next != null && !next.trim().isEmpty()) {
				result.nextQuestion = NextQuestion.parse(next.trim());
			}
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
		}

		@Override
		public void characters(String ch, List<String> parents)
				throws ParseException {
			if (!ch.trim().isEmpty())
				throw new ParseException("Unexpected text content");
		}

		@Override
		public MultipleChoiceAnswer getObject() {
			return result;
		}
	}
}
