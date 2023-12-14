package nl.rrd.senseeact.client.model.questionnaire;

import org.xml.sax.Attributes;

import java.util.List;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

public class SetSelectionOption {
	private String value;
	private String text;

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

	public static SimpleSAXHandler<SetSelectionOption> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<SetSelectionOption> {
		private SetSelectionOption result = null;
		private int rootLevel = -1;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startOption(atts);
			} else {
				throw new ParseException("Unexpected element: " + name);
			}
		}

		private void startOption(Attributes atts) throws ParseException {
			String value = readAttribute(atts, "value", 1, null);
			String text = readAttribute(atts, "text", 1, null);
			result = new SetSelectionOption();
			result.value = value;
			result.text = text;
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
		public SetSelectionOption getObject() {
			return result;
		}
	}
}
