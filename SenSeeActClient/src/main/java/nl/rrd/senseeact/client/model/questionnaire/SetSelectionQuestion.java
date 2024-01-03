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
public class SetSelectionQuestion extends Question {
	private List<SetSelectionOption> options = new ArrayList<>();
	private boolean allowAddCustom = false;

	public SetSelectionQuestion() {
		super(TYPE_SET_SELECTION);
	}

	public List<SetSelectionOption> getOptions() {
		return options;
	}

	public void setOptions(List<SetSelectionOption> options) {
		this.options = options;
	}

	public void addOption(SetSelectionOption option) {
		options.add(option);
	}

	public boolean isAllowAddCustom() {
		return allowAddCustom;
	}

	public void setAllowAddCustom(boolean allowAddCustom) {
		this.allowAddCustom = allowAddCustom;
	}

	public static SimpleSAXHandler<SetSelectionQuestion> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<SetSelectionQuestion> {
		private SetSelectionQuestion result;
		private int rootLevel = -1;
		private SimpleSAXHandler<SetSelectionOption> optionHandler = null;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1) {
				rootLevel = parents.size();
				startSetSelection(atts);
			} else if (parents.size() == rootLevel + 1) {
				if (!name.equals("option")) {
					throw new ParseException(
							"Expected element \"option\", found: " + name);
				}
				optionHandler = SetSelectionOption.getXMLHandler();
			}
			if (optionHandler != null)
				optionHandler.startElement(name, atts, parents);
		}

		private void startSetSelection(Attributes atts)
				throws ParseException {
			result = new SetSelectionQuestion();
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
			if (atts.getValue("allow_add_custom") != null) {
				result.allowAddCustom = readBooleanAttribute(atts,
						"allow_add_custom");
			}
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
			if (optionHandler != null)
				optionHandler.endElement(name, parents);
			if (parents.size() == rootLevel + 1) {
				result.addOption(optionHandler.getObject());
				optionHandler = null;
			}
		}

		@Override
		public void characters(String ch, List<String> parents)
				throws ParseException {
			if (optionHandler != null)
				optionHandler.characters(ch, parents);
			else if (!ch.trim().isEmpty())
				throw new ParseException("Unexpected text content");
		}

		@Override
		public SetSelectionQuestion getObject() {
			return result;
		}
	}
}
