package nl.rrd.senseeact.client.model.questionnaire;

import org.slf4j.Logger;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.LineNumberParseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.expressions.EvaluationException;
import nl.rrd.utils.expressions.Expression;
import nl.rrd.utils.expressions.ExpressionParser;
import nl.rrd.utils.expressions.StringExpression;
import nl.rrd.utils.expressions.Token;
import nl.rrd.utils.expressions.Tokenizer;
import nl.rrd.utils.expressions.Value;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

public abstract class Question {
	private QuestionCondition condition = null;

	/**
	 * Returns the ID of this question as a string expression, which may
	 * contain variables. Depending on the condition, this can be evaluated
	 * to zero or more data IDs with {@link #getDataIds(Map) getDataIds()}.
	 *
	 * @return the ID
	 */
	public abstract StringExpression getId();

	/**
	 * Returns the condition for this question. If no condition applies, then
	 * this method returns null.
	 *
	 * @return the condition or null
	 */
	public QuestionCondition getCondition() {
		return condition;
	}

	/**
	 * This method can be called if the this question is the current question.
	 * It returns the current data ID based on the answer data from previous
	 * questions.
	 *
	 * <p>The specified answer map should be a map from question data ID to
	 * a data map. It can also be null.</p>
	 *
	 * <p>This method searches the first data ID for this question that does
	 * not occur in the answer map yet. If all data IDs already occur, then
	 * this method returns null.</p>
	 *
	 * @param answerMap the answer map or null
	 * @return the current data ID or null
	 */
	public String getCurrentDataId(Map<String,Object> answerMap) {
		List<String> dataIds = getDataIds(answerMap);
		for (String dataId : dataIds) {
			if (answerMap == null || !answerMap.containsKey(dataId))
				return dataId;
		}
		return null;
	}

	/**
	 * Returns all data IDs for the current question based on the answer data
	 * from previous questions.
	 *
	 * <p>The specified answer map should be a map from question data ID to
	 * a data map. It can also be null.</p>
	 *
	 * <p>This method evaluates the condition if specified.</p>
	 *
	 * @param answerMap the answer map or null
	 * @return the data IDs
	 */
	public List<String> getDataIds(Map<String,Object> answerMap) {
		if (condition == null) {
			String dataId = getDataId(answerMap);
			if (dataId != null)
				return Collections.singletonList(dataId);
			else
				return Collections.emptyList();
		} else if (condition instanceof QuestionCondition.If ifCond) {
			boolean matches;
			try {
				matches = ifCond.getExpression().evaluate(answerMap)
						.asBoolean();
			} catch (EvaluationException ex) {
				matches = false;
			}
			if (!matches)
				return Collections.emptyList();
			String dataId = getDataId(answerMap);
			if (dataId != null)
				return Collections.singletonList(dataId);
			else
				return Collections.emptyList();
		} else {
			QuestionCondition.Foreach forCond =
					(QuestionCondition.Foreach) condition;
			List<?> list = getConditionForeachItems(forCond, answerMap);
			List<String> result = new ArrayList<>();
			for (Object item : list) {
				String dataId = getConditionForeachDataId(forCond, item,
						answerMap);
				if (dataId != null)
					result.add(dataId);
			}
			return result;
		}
	}

	/**
	 * Returns the data ID by evaluating the ID expression with the specified
	 * set of variables. If an evaluation error occurs, this method logs a
	 * warning and returns null.
	 *
	 * <p>The specified variables should contain answers from previous questions
	 * and possible variables from a condition.</p>
	 *
	 * @param variables the variables
	 * @return the data ID or null
	 */
	private String getDataId(Map<String,Object> variables) {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		Expression idExpr = getId();
		try {
			Value val = idExpr.evaluate(variables);
			if (val.isString() || val.isNumber()) {
				return val.toString();
			} else {
				logger.warn("Ignoring invalid question ID: " +
						val.getTypeString());
				return null;
			}
		} catch (EvaluationException ex) {
			logger.warn("Can't evaluate question ID: " + idExpr + ": " +
					ex.getMessage(), ex);
			return null;
		}
	}

	public Map<String,?> getConditionVariables(QuestionnaireData data) {
		Map<String,Object> result = new LinkedHashMap<>();
		if (condition == null) {
			return result;
		} else if (condition instanceof QuestionCondition.If) {
			return result;
		} else {
			QuestionCondition.Foreach forCond =
					(QuestionCondition.Foreach)condition;
			Map<String,Object> answerMap = data.getAnswerMap();
			List<?> items = getConditionForeachItems(forCond, answerMap);
			for (Object item : items) {
				String dataId = getConditionForeachDataId(forCond, item,
						answerMap);
				if (data.getCurrentQuestionId().equals(dataId)) {
					result.put(forCond.getName(), item);
					break;
				}
			}
			return result;
		}
	}

	private List<?> getConditionForeachItems(QuestionCondition.Foreach cond,
			Map<String,Object> answerMap) {
		try {
			return (List<?>)cond.getList().evaluate(answerMap).getValue();
		} catch (EvaluationException ex) {
			return Collections.emptyList();
		}
	}

	/**
	 * Returns the data ID for an instance of this question from a foreach
	 * condition. It creates a variable set from the specified answer map plus
	 * the foreach variable name, which will be set to "item". Using that
	 * variable set, it will evaluate the ID expression. If an evaluation error
	 * occurs, this method logs a warning and returns null.
	 *
	 * @param cond the foreach condition
	 * @param item one item from the foreach list
	 * @param answerMap answers from previous questions
	 * @return the data ID or null
	 */
	private String getConditionForeachDataId(QuestionCondition.Foreach cond,
			Object item, Map<String,?> answerMap) {
		Map<String,Object> itemVars = new LinkedHashMap<>();
		if (answerMap != null)
			itemVars.putAll(answerMap);
		itemVars.put(cond.getName(), item);
		return getDataId(itemVars);
	}

	public static SimpleSAXHandler<? extends Question> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends AbstractSimpleSAXHandler<Question> {
		private int rootLevel = -1;
		private SimpleSAXHandler<? extends Question> delegate = null;
		private QuestionCondition condition = null;
		private boolean inCondition = false;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1)
				rootLevel = parents.size();
			if (delegate == null) {
				switch (name) {
					case "emoji":
						delegate = EmojiQuestion.getXMLHandler();
						break;
					case "fixed_text":
						delegate = FixedTextQuestion.getXMLHandler();
						break;
					case "free_text":
						delegate = FreeTextQuestion.getXMLHandler();
						break;
					case "likert":
						delegate = LikertQuestion.getXMLHandler();
						break;
					case "multiple_choice":
						delegate = MultipleChoiceQuestion.getXMLHandler();
						break;
					case "set_selection":
						delegate = SetSelectionQuestion.getXMLHandler();
						break;
					case "vas":
						delegate = VASQuestion.getXMLHandler();
						break;
					default:
						throw new ParseException("Unknown question type: " +
								name);
				}
			}
			if (inCondition) {
				throw new ParseException(
						"Unexpected element in \"condition\": " + name);
			}
			if (parents.size() == rootLevel + 1 && name.equals("condition")) {
				startCondition();
			} else {
				delegate.startElement(name, atts, parents);
			}
			if (rootLevel == parents.size())
				parseCommonAttributes(atts);
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
			if (inCondition)
				inCondition = false;
			else
				delegate.endElement(name, parents);
		}

		@Override
		public void characters(String ch, List<String> parents)
				throws ParseException {
			if (inCondition) {
				condition = parseCondition(ch);
			} else {
				delegate.characters(ch, parents);
			}
		}

		private void startCondition() throws ParseException {
			if (condition != null) {
				throw new ParseException(
						"Found attribute \"condition\" and element \"condition\"");
			}
			inCondition = true;
		}

		private void parseCommonAttributes(Attributes atts)
				throws ParseException {
			String condStr = atts.getValue("condition");
			if (condStr != null && !condStr.trim().isEmpty())
				condition = parseCondition(condStr);
		}

		private QuestionCondition parseCondition(String condStr)
				throws ParseException {
			Tokenizer tokenizer = new Tokenizer(condStr);
			ExpressionParser exprParser = new ExpressionParser(tokenizer);
			try {
				try {
					return parseCondition(tokenizer, exprParser);
				} catch (LineNumberParseException ex) {
					throw new ParseException(
							"Invalid value of attribute \"condition\": " +
							condStr + ": " + ex.getMessage(), ex);
				} finally {
					exprParser.close();
				}
			} catch (IOException ex) {
				throw new RuntimeException("I/O exception in string reader: " +
						ex.getMessage(), ex);
			}
		}

		private QuestionCondition parseCondition(Tokenizer tokenizer,
				ExpressionParser exprParser) throws ParseException,
				IOException {
			Token token = tokenizer.readToken();
			if (token == null)
				throw new ParseException("Unexpected end of expression");
			return switch (token.getText()) {
				case "foreach" -> parseForeachCondition(tokenizer, exprParser);
				case "if" -> parseIfCondition(tokenizer, exprParser);
				default ->
					throw new ParseException("Invalid token: " +
							token.getText());
			};
		}

		private QuestionCondition.Foreach parseForeachCondition(
				Tokenizer tokenizer, ExpressionParser exprParser)
				throws ParseException, IOException {
			Token token = tokenizer.readToken();
			if (token == null) {
				throw new ParseException(
						"Expected name, found end of expression");
			}
			if (token.getType() != Token.Type.NAME) {
				throw new ParseException("Expected name, found: " +
						token.getText());
			}
			String name = token.getValue().toString();
			token = tokenizer.readToken();
			if (token == null) {
				throw new ParseException(
						"Expected \"in\", found end of expression");
			}
			if (token.getType() != Token.Type.IN) {
				throw new ParseException("Expected \"in\", found: " +
						token.getText());
			}
			Expression list = exprParser.readExpression();
			if (list == null) {
				throw new ParseException(
						"Expected list, found end of expression");
			}
			token = tokenizer.readToken();
			if (token != null) {
				throw new ParseException("Unexpected token after expression: " +
						token.getText());
			}
			return new QuestionCondition.Foreach(name, list);
		}

		private QuestionCondition.If parseIfCondition(Tokenizer tokenizer,
				ExpressionParser exprParser) throws ParseException,
				IOException {
			Expression expr = exprParser.readExpression();
			if (expr == null)
				throw new ParseException("Unexpected end of expression");
			Token token = tokenizer.readToken();
			if (token != null) {
				throw new ParseException("Unexpected token after expression: " +
						token.getText());
			}
			return new QuestionCondition.If(expr);
		}

		@Override
		public Question getObject() {
			Question result = delegate.getObject();
			result.condition = condition;
			return result;
		}
	}
}
