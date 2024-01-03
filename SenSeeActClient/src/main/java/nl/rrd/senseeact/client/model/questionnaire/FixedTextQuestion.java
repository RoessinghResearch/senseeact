package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.xml.sax.Attributes;

import java.util.List;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.expressions.StringExpression;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonDeserialize(using=JsonDeserializer.None.class)
public class FixedTextQuestion extends Question {
	public FixedTextQuestion() {
		super(TYPE_FIXED_TEXT);
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
				result.setId(new StringExpression(idExpr));
			} catch (ParseException ex) {
				throw new ParseException("Invalid value of attribute \"id\": " +
						idExpr + ": " + ex.getMessage(), ex);
			}
			if (atts.getValue("title") != null)
				result.setTitle(readAttribute(atts, "title", 1, null));
			result.setQuestion(readAttribute(atts, "text", 1, null));
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
		public FixedTextQuestion getObject() {
			return result;
		}
	}
}
