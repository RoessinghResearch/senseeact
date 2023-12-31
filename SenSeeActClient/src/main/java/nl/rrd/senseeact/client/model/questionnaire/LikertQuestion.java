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
public class LikertQuestion extends Question {
	private int startValue;
	private int endValue;
	private List<ScaleLabel> labels = new ArrayList<>();

	public LikertQuestion() {
		super(TYPE_LIKERT);
	}

	public int getStartValue() {
		return startValue;
	}

	public void setStartValue(int startValue) {
		this.startValue = startValue;
	}

	public int getEndValue() {
		return endValue;
	}

	public void setEndValue(int endValue) {
		this.endValue = endValue;
	}

	public List<ScaleLabel> getLabels() {
		return labels;
	}

	public void setLabels(List<ScaleLabel> labels) {
		this.labels = labels;
	}

	public void addLabel(ScaleLabel label) {
		labels.add(label);
	}

	public static SimpleSAXHandler<LikertQuestion> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<LikertQuestion> {
		private LikertQuestion result;
		private int rootLevel = -1;
		private SimpleSAXHandler<ScaleLabel> labelHandler = null;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startLikert(atts);
			} else if (parents.size() == rootLevel + 1) {
				if (!name.equals("label")) {
					throw new ParseException(
							"Expected element \"label\", found: " + name);
				}
				labelHandler = ScaleLabel.getXMLHandler();
			}
			if (labelHandler != null)
				labelHandler.startElement(name, atts, parents);
		}

		private void startLikert(Attributes atts) throws ParseException {
			result = new LikertQuestion();
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
			result.startValue = readIntAttribute(atts, "start_value");
			result.endValue = readIntAttribute(atts, "end_value");
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
			if (labelHandler != null)
				labelHandler.endElement(name, parents);
			if (parents.size() == rootLevel + 1) {
				result.addLabel(labelHandler.getObject());
				labelHandler = null;
			}
		}

		@Override
		public void characters(String ch, List<String> parents)
				throws ParseException {
			if (labelHandler != null)
				labelHandler.characters(ch, parents);
			else if (!ch.trim().isEmpty())
				throw new ParseException("Unexpected text content");
		}

		@Override
		public LikertQuestion getObject() {
			return result;
		}
	}
}
