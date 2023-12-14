package nl.rrd.senseeact.client.model.questionnaire;

import org.xml.sax.Attributes;

import java.util.List;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.expressions.StringExpression;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

public class FreeTextQuestion extends Question {
	private StringExpression id;
	private String title = null;
	private String question;
	private boolean isLong;

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

	public boolean isLong() {
		return isLong;
	}

	public void setLong(boolean aLong) {
		isLong = aLong;
	}

	public static SimpleSAXHandler<FreeTextQuestion> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<FreeTextQuestion> {
		private FreeTextQuestion result;
		private int rootLevel = -1;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startFreeText(atts);
			} else {
				throw new ParseException("Unexpected element: " + name);
			}
		}

		private void startFreeText(Attributes atts) throws ParseException {
			result = new FreeTextQuestion();
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
			if (atts.getValue("long") != null)
				result.isLong = readBooleanAttribute(atts, "long");
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
		}

		@Override
		public void characters(String ch, List<String> parents) throws ParseException {
			if (!ch.trim().isEmpty())
				throw new ParseException("Unexpected text content");
		}

		@Override
		public FreeTextQuestion getObject() {
			return result;
		}
	}
}
