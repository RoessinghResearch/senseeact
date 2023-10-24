package nl.rrd.senseeact.client.model.questionnaire;

import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.expressions.StringExpression;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

public class EmojiQuestion extends Question {
	private StringExpression id;
	private String title = null;
	private String question;
	private int optionCount = 3;
	private List<MultipleChoiceAnswer> extraAnswers = new ArrayList<>();

	@Override
	public StringExpression getId() {
		return id;
	}

	public void setId(StringExpression id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	/**
	 * Returns the number of emoji options in this question. This can be 2, 3,
	 * 4 or 5. The default is 3.
	 *
	 * @return the number of emoji options in this question
	 */
	public int getOptionCount() {
		return optionCount;
	}

	/**
	 * Sets the number of emoji options in this question. This can be 2, 3, 4 or
	 * 5. The default is 5.
	 *
	 * @param optionCount the number of emoji options in this question
	 */
	public void setOptionCount(int optionCount) {
		this.optionCount = optionCount;
	}

	public List<MultipleChoiceAnswer> getExtraAnswers() {
		return extraAnswers;
	}

	public void setExtraAnswers(List<MultipleChoiceAnswer> extraAnswers) {
		this.extraAnswers = extraAnswers;
	}

	public void addExtraAnswer(MultipleChoiceAnswer extraAnswer) {
		extraAnswers.add(extraAnswer);
	}

	public static SimpleSAXHandler<EmojiQuestion> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<EmojiQuestion> {
		private EmojiQuestion result;
		private int rootLevel = -1;
		private SimpleSAXHandler<MultipleChoiceAnswer> extraAnswerHandler =
				null;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startEmoji(atts);
			} else if (parents.size() == rootLevel + 1) {
				if (!name.equals("extra_answer")) {
					throw new ParseException(
							"Expected element \"extra_answer\", found: " + name);
				}
				extraAnswerHandler = MultipleChoiceAnswer.getXMLHandler();
			}
			if (extraAnswerHandler != null)
				extraAnswerHandler.startElement(name, atts, parents);
		}

		private void startEmoji(Attributes atts)
				throws ParseException {
			result = new EmojiQuestion();
			String idExpr = readAttribute(atts, "id", 1, null);
			try {
				result.id = new StringExpression(idExpr);
			} catch (ParseException ex) {
				throw new ParseException("Invalid value of attribute \"id\": " +
						idExpr + ": " + ex.getMessage(), ex);
			}
			if (atts.getValue("title") != null)
				result.title = readAttribute(atts, "title", 1, null);
			result.question = readAttribute(atts, "question", 1, null);
			if (atts.getValue("option_count") != null)
				result.optionCount = readIntAttribute(atts, "option_count");
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
			if (extraAnswerHandler != null)
				extraAnswerHandler.endElement(name, parents);
			if (parents.size() == rootLevel + 1) {
				result.addExtraAnswer(extraAnswerHandler.getObject());
				extraAnswerHandler = null;
			}
		}

		@Override
		public void characters(String ch, List<String> parents)
				throws ParseException {
			if (extraAnswerHandler != null)
				extraAnswerHandler.characters(ch, parents);
			else if (ch.trim().length() > 0)
				throw new ParseException("Unexpected text content");
		}

		@Override
		public EmojiQuestion getObject() {
			return result;
		}
	}
}
