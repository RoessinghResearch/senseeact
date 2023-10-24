package nl.rrd.senseeact.client.model.questionnaire;

import nl.rrd.utils.expressions.Expression;

public abstract class QuestionCondition {
	public static class Foreach extends QuestionCondition {
		private String name;
		private Expression list;

		public Foreach(String name, Expression list) {
			this.name = name;
			this.list = list;
		}

		public String getName() {
			return name;
		}

		public Expression getList() {
			return list;
		}
	}

	public static class If extends QuestionCondition {
		private Expression expression;

		public If(Expression expression) {
			this.expression = expression;
		}

		public Expression getExpression() {
			return expression;
		}
	}
}
