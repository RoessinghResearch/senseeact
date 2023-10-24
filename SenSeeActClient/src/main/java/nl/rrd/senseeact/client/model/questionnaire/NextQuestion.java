package nl.rrd.senseeact.client.model.questionnaire;

import java.util.Map;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.expressions.EvaluationException;
import nl.rrd.utils.expressions.StringExpression;
import nl.rrd.utils.expressions.Value;

public class NextQuestion {
	private StringExpression questionnaireId = null;
	private StringExpression questionId = null;

	private NextQuestion() {
	}

	public boolean isNone() {
		return questionnaireId == null && questionId == null;
	}

	public boolean isQuestionnaire() {
		return questionnaireId != null;
	}

	public String getNextQuestionnaireId(Map<String,Object> vars) {
		return evaluateIdExpr(questionnaireId, vars);
	}

	public boolean isQuestion() {
		return questionId != null;
	}

	public String getNextQuestionId(Map<String,Object> vars) {
		return evaluateIdExpr(questionId, vars);
	}

	private String evaluateIdExpr(StringExpression idExpr,
			Map<String,Object> vars) {
		try {
			Value val = idExpr.evaluate(vars);
			if (val.isString() || val.isNumber()) {
				return val.toString();
			} else {
				throw new RuntimeException(
						"ID expression must evaluate to string or number, found: " +
								val.getTypeString());
			}
		} catch (EvaluationException ex) {
			throw new RuntimeException("Can't evaluate ID expression: " +
					idExpr.toString() + ": " + ex.getMessage(), ex);
		}
	}

	public static NextQuestion parse(String s) throws ParseException {
		NextQuestion result = new NextQuestion();
		if (s.toLowerCase().equals("none"))
			return result;
		int sep = s.indexOf(':');
		String prefix = null;
		if (sep != -1) {
			prefix = s.substring(0, sep);
			s = s.substring(sep + 1);
		}
		if (prefix == null) {
			result.questionId = new StringExpression(s);
		} else if (prefix.toLowerCase().equals("questionnaire")) {
			result.questionnaireId = new StringExpression(s);
		} else {
			throw new ParseException("Unknown prefix: " + prefix);
		}
		return result;
	}
}
