package nl.rrd.senseeact.client.model.questionnaire;

import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.expressions.StringExpression;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

public class VASQuestion extends Question {
	private StringExpression id;
	private String title = null;
	private String question;
	private float startValue;
	private float endValue;
	private float step;
	private boolean snap = false;
	private boolean showNumbers = false;
	private List<ScaleLabel> labels = new ArrayList<>();

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

	public float getStartValue() {
		return startValue;
	}

	public void setStartValue(float startValue) {
		this.startValue = startValue;
	}

	public float getEndValue() {
		return endValue;
	}

	public void setEndValue(float endValue) {
		this.endValue = endValue;
	}

	public float getStep() {
		return step;
	}

	public void setStep(float step) {
		this.step = step;
	}

	public boolean isSnap() {
		return snap;
	}

	public void setSnap(boolean snap) {
		this.snap = snap;
	}

	public boolean isShowNumbers() {
		return showNumbers;
	}

	public void setShowNumbers(boolean showNumbers) {
		this.showNumbers = showNumbers;
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

	public ScaleLabel findLabelForValue(int value) {
		for (ScaleLabel label : labels) {
			if (label.getValue() == value)
				return label;
		}
		return null;
	}

	public static SimpleSAXHandler<VASQuestion> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<VASQuestion> {
		private VASQuestion result;
		private int rootLevel = -1;
		private SimpleSAXHandler<ScaleLabel> labelHandler = null;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startVas(atts);
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

		private void startVas(Attributes atts) throws ParseException {
			result = new VASQuestion();
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
			result.startValue = readFloatAttribute(atts, "start_value");
			result.endValue = readFloatAttribute(atts, "end_value");
			result.step = readFloatAttribute(atts, "step");
			if (result.startValue < result.endValue && result.step <= 0) {
				throw new ParseException(
						"Invalid value of attribute \"step\": start_value < end_value so step must be > 0");
			} else if (result.startValue > result.endValue &&
					result.step >= 0) {
				throw new ParseException(
						"Invalid value of attribute \"step\": start_value > end_value so step must be < 0");
			}
			if (atts.getValue("snap") != null)
				result.snap = readBooleanAttribute(atts, "snap");
			if (atts.getValue("show_numbers") != null)
				result.showNumbers = readBooleanAttribute(atts, "show_numbers");
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
			else if (ch.trim().length() > 0)
				throw new ParseException("Unexpected text content");
		}

		@Override
		public VASQuestion getObject() {
			return result;
		}
	}
}
