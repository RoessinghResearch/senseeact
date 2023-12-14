package nl.rrd.senseeact.client.model.questionnaire;

import org.xml.sax.Attributes;

import java.util.List;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

public class ScaleLabel {
	private float value;
	private String text;

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public static SimpleSAXHandler<ScaleLabel> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<ScaleLabel> {
		private ScaleLabel result;
		private int rootLevel = -1;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startLabel(atts);
			} else {
				throw new ParseException("Unexpected element: " + name);
			}
		}

		private void startLabel(Attributes atts) throws ParseException {
			result = new ScaleLabel();
			result.value = readFloatAttribute(atts, "value");
			result.text = readAttribute(atts, "text", 1, null);
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
		public ScaleLabel getObject() {
			return result;
		}
	}
}
