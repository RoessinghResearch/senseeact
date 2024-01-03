package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.exception.LineNumberParseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.expressions.Expression;
import nl.rrd.utils.expressions.ExpressionParser;
import nl.rrd.utils.expressions.Token;
import nl.rrd.utils.expressions.Tokenizer;

import java.io.IOException;

@JsonSerialize(using=QuestionCondition.ConditionSerializer.class)
@JsonDeserialize(using=QuestionCondition.ConditionDeserializer.class)
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

		@Override
		public String toString() {
			return "foreach " + name + " in " + list.toCode();
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

		@Override
		public String toString() {
			return "if " + expression.toCode();
		}
	}

	public static QuestionCondition parse(String condStr)
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

	private static QuestionCondition parseCondition(Tokenizer tokenizer,
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

	private static QuestionCondition.Foreach parseForeachCondition(
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

	private static QuestionCondition.If parseIfCondition(Tokenizer tokenizer,
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

	public static class ConditionSerializer
			extends JsonSerializer<QuestionCondition> {
		@Override
		public void serialize(QuestionCondition questionCondition,
				JsonGenerator jsonGenerator,
				SerializerProvider serializerProvider) throws IOException {
			jsonGenerator.writeString(questionCondition.toString());
		}
	}

	public static class ConditionDeserializer
			extends JsonDeserializer<QuestionCondition> {
		@Override
		public QuestionCondition deserialize(JsonParser jsonParser,
				DeserializationContext deserializationContext)
				throws IOException, JacksonException {
			String s = jsonParser.getValueAsString();
			try {
				return QuestionCondition.parse(s);
			} catch (ParseException ex) {
				throw new JsonParseException(jsonParser,
						"Invalid condition: " + s + ": " + ex.getMessage(), ex);
			}
		}
	}
}
