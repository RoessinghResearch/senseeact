package nl.rrd.senseeact.client.model.questionnaire;

import org.xml.sax.Attributes;

import java.util.List;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.expressions.StringExpression;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

public class FixedTextQuestion extends Question {
	private StringExpression id;
	private String title;
	private String text;

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

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public static SimpleSAXHandler<FixedTextQuestion> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<FixedTextQuestion> {
		private FixedTextQuestion result;
		private int rootLevel = -1;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startFixedText(atts);
			} else {
				throw new ParseException("Unexpected element: " + name);
			}
		}

		private void startFixedText(Attributes atts) throws ParseException {
			result = new FixedTextQuestion();
			String idExpr = readAttribute(atts, "id", 1, null);
			try {
				result.id = new StringExpression(idExpr);
			} catch (ParseException ex) {
				throw new ParseException("Invalid value of attribute \"id\": " +
						idExpr + ": " + ex.getMessage(), ex);
			}
			if (atts.getValue("title") != null)
				result.title = readAttribute(atts, "title", 1, null);
			result.text = readAttribute(atts, "text", 1, null);
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
		}

		@Override
		public void characters(String ch, List<String> parents) throws ParseException {
			if (ch.trim().length() > 0)
				throw new ParseException("Unexpected text content");
		}

		@Override
		public FixedTextQuestion getObject() {
			return result;
		}
	}
}
