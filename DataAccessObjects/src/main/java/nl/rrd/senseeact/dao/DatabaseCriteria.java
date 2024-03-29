package nl.rrd.senseeact.dao;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class can be passed to database queries to restrict the set of selected
 * objects. It has a number of subclasses for common operators. They can be
 * subdivided into comparison operators and logical operators.
 * 
 * <p><b>Comparison operators</b></p>
 * 
 * <p><ul>
 * <li>{@link Equal Equal}</li>
 * <li>{@link NotEqual NotEqual}</li>
 * <li>{@link LessThan LessThan}</li>
 * <li>{@link GreaterThan GreaterThan}</li>
 * <li>{@link LessEqual LessEqual}</li>
 * <li>{@link GreaterEqual GreaterEqual}</li>
 * </ul></p>
 * 
 * <p>Each of them take two arguments: a column name and a literal value
 * (string or number). In the future the second argument could include other
 * types of expressions as well (for example another column name).</p>
 * 
 * <p>Note that string comparisons are sensitive to case and diacritics. This
 * is normal in MongoDB and SQLite, but different than the default in
 * MariaDB.</p>
 * 
 * <p><b>Logical operators</b></p>
 * 
 * <p><ul>
 * <li>{@link And And}</li>
 * <li>{@link Or Or}</li>
 * </ul></p>
 * 
 * <p>These operators take a list of arguments, which are again {@link
 * DatabaseCriteria DatabaseCriteria}.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
public abstract class DatabaseCriteria {
	
	/**
	 * Formats the specified value for logging. Depending on the argument type
	 * it returns:
	 * 
	 * <p><ul>
	 * <li>null: null</li>
	 * <li>number: value.toString()</li>
	 * <li>string: JSON string</li>
	 * </ul></p>
	 * 
	 * @param value the value (number, string or null)
	 * @return the formatted value
	 */
	protected String formatValue(Object value) {
		if (value == null)
			return "null";
		if (value instanceof Number)
			return value.toString();
		String result = value.toString();
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(result);
		} catch (JsonProcessingException ex) {
			throw new RuntimeException("Can't convert string to JSON: " +
					ex.getMessage(), ex);
		}
	}

	public abstract boolean containsColumn(String column);
	
	@Override
	public abstract int hashCode();
	
	@Override
	public abstract boolean equals(Object obj);
	
	@Override
	public abstract String toString();
	
	public static class Equal extends DatabaseCriteria {
		private String column;
		private Object value;
		
		/**
		 * Note that string comparisons are sensitive to case and diacritics.
		 * This is normal in MongoDB and SQLite, but different than the default
		 * in MariaDB.
		 * 
		 * @param column the column name
		 * @param value the value
		 */
		public Equal(String column, String value) {
			this.column = column;
			this.value = value;
		}
		
		public Equal(String column, Number value) {
			this.column = column;
			this.value = value;
		}

		public String getColumn() {
			return column;
		}
		
		public Object getValue() {
			return value;
		}

		@Override
		public boolean containsColumn(String column) {
			return column.equals(this.column);
		}

		@Override
		public int hashCode() {
			int result = column.hashCode();
			Object normVal = PrimitiveValueComparison.normalizeValue(value);
			result = 31 * result + (normVal != null ? normVal.hashCode() : 0);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Equal other = (Equal)obj;
			if (!column.equals(other.column))
				return false;
			if (!PrimitiveValueComparison.isEqual(value, other.value))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return column + " = " + formatValue(value);
		}
	}
	
	public static class NotEqual extends DatabaseCriteria {
		private String column;
		private Object value;

		/**
		 * Note that string comparisons are sensitive to case and diacritics.
		 * This is normal in MongoDB and SQLite, but different than the default
		 * in MariaDB.
		 * 
		 * @param column the column name
		 * @param value the value
		 */
		public NotEqual(String column, String value) {
			this.column = column;
			this.value = value;
		}
		
		public NotEqual(String column, Number value) {
			this.column = column;
			this.value = value;
		}

		public String getColumn() {
			return column;
		}
		
		public Object getValue() {
			return value;
		}

		@Override
		public boolean containsColumn(String column) {
			return column.equals(this.column);
		}

		@Override
		public int hashCode() {
			int result = column.hashCode();
			Object normVal = PrimitiveValueComparison.normalizeValue(value);
			result = 31 * result + (normVal != null ? normVal.hashCode() : 0);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NotEqual other = (NotEqual)obj;
			if (!column.equals(other.column))
				return false;
			if (!PrimitiveValueComparison.isEqual(value, other.value))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return column + " != " + formatValue(value);
		}
	}
	
	public static class LessThan extends DatabaseCriteria {
		private String column;
		private Object value;

		/**
		 * Note that string comparisons are sensitive to case and diacritics.
		 * This is normal in MongoDB and SQLite, but different than the default
		 * in MariaDB.
		 * 
		 * @param column the column name
		 * @param value the value
		 */
		public LessThan(String column, String value) {
			this.column = column;
			this.value = value;
		}
		
		public LessThan(String column, Number value) {
			this.column = column;
			this.value = value;
		}
		
		public String getColumn() {
			return column;
		}
		
		public Object getValue() {
			return value;
		}

		@Override
		public boolean containsColumn(String column) {
			return column.equals(this.column);
		}

		@Override
		public int hashCode() {
			int result = column.hashCode();
			Object normVal = PrimitiveValueComparison.normalizeValue(value);
			result = 31 * result + (normVal != null ? normVal.hashCode() : 0);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LessThan other = (LessThan)obj;
			if (!column.equals(other.column))
				return false;
			if (!PrimitiveValueComparison.isEqual(value, other.value))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return column + " < " + formatValue(value);
		}
	}
	
	public static class GreaterThan extends DatabaseCriteria {
		private String column;
		private Object value;

		/**
		 * Note that string comparisons are sensitive to case and diacritics.
		 * This is normal in MongoDB and SQLite, but different than the default
		 * in MariaDB.
		 * 
		 * @param column the column name
		 * @param value the value
		 */
		public GreaterThan(String column, String value) {
			this.column = column;
			this.value = value;
		}
		
		public GreaterThan(String column, Number value) {
			this.column = column;
			this.value = value;
		}
		
		public String getColumn() {
			return column;
		}
		
		public Object getValue() {
			return value;
		}

		@Override
		public boolean containsColumn(String column) {
			return column.equals(this.column);
		}

		@Override
		public int hashCode() {
			int result = column.hashCode();
			Object normVal = PrimitiveValueComparison.normalizeValue(value);
			result = 31 * result + (normVal != null ? normVal.hashCode() : 0);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GreaterThan other = (GreaterThan)obj;
			if (!column.equals(other.column))
				return false;
			if (!PrimitiveValueComparison.isEqual(value, other.value))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return column + " > " + formatValue(value);
		}
	}
	
	public static class LessEqual extends DatabaseCriteria {
		private String column;
		private Object value;

		/**
		 * Note that string comparisons are sensitive to case and diacritics.
		 * This is normal in MongoDB and SQLite, but different than the default
		 * in MariaDB.
		 * 
		 * @param column the column name
		 * @param value the value
		 */
		public LessEqual(String column, String value) {
			this.column = column;
			this.value = value;
		}
		
		public LessEqual(String column, Number value) {
			this.column = column;
			this.value = value;
		}
		
		public String getColumn() {
			return column;
		}
		
		public Object getValue() {
			return value;
		}

		@Override
		public boolean containsColumn(String column) {
			return column.equals(this.column);
		}

		@Override
		public int hashCode() {
			int result = column.hashCode();
			Object normVal = PrimitiveValueComparison.normalizeValue(value);
			result = 31 * result + (normVal != null ? normVal.hashCode() : 0);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LessEqual other = (LessEqual)obj;
			if (!column.equals(other.column))
				return false;
			if (!PrimitiveValueComparison.isEqual(value, other.value))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return column + " <= " + formatValue(value);
		}
	}
	
	public static class GreaterEqual extends DatabaseCriteria {
		private String column;
		private Object value;

		/**
		 * Note that string comparisons are sensitive to case and diacritics.
		 * This is normal in MongoDB and SQLite, but different than the default
		 * in MariaDB.
		 * 
		 * @param column the column name
		 * @param value the value
		 */
		public GreaterEqual(String column, String value) {
			this.column = column;
			this.value = value;
		}
		
		public GreaterEqual(String column, Number value) {
			this.column = column;
			this.value = value;
		}
		
		public String getColumn() {
			return column;
		}
		
		public Object getValue() {
			return value;
		}

		@Override
		public boolean containsColumn(String column) {
			return column.equals(this.column);
		}

		@Override
		public int hashCode() {
			int result = column.hashCode();
			Object normVal = PrimitiveValueComparison.normalizeValue(value);
			result = 31 * result + (normVal != null ? normVal.hashCode() : 0);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GreaterEqual other = (GreaterEqual)obj;
			if (!column.equals(other.column))
				return false;
			if (!PrimitiveValueComparison.isEqual(value, other.value))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return column + " >= " + formatValue(value);
		}
	}
	
	public static class And extends DatabaseCriteria {
		private DatabaseCriteria[] operands;
		
		public And(DatabaseCriteria... operands) {
			this.operands = operands;
		}
		
		public DatabaseCriteria[] getOperands() {
			return operands;
		}

		@Override
		public boolean containsColumn(String column) {
			for (DatabaseCriteria operand : operands) {
				if (operand.containsColumn(column))
					return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			int result = 0;
			for (DatabaseCriteria operand : operands) {
				result += operand.hashCode();
			}
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			And other = (And)obj;
			Set<DatabaseCriteria> thisOps = new HashSet<>(
					Arrays.asList(operands));
			Set<DatabaseCriteria> otherOps = new HashSet<>(
					Arrays.asList(other.operands));
			if (!thisOps.equals(otherOps))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			if (operands.length == 0)
				return "";
			if (operands.length == 1)
				return operands[0].toString();
			StringBuilder result = new StringBuilder(
					"(" + operands[0].toString() + ")");
			for (int i = 1; i < operands.length; i++) {
				result.append(" AND (" + operands[i] + ")");
			}
			return result.toString();
		}
	}
	
	public static class Or extends DatabaseCriteria {
		private DatabaseCriteria[] operands;
		
		public Or(DatabaseCriteria... operands) {
			this.operands = operands;
		}
		
		public DatabaseCriteria[] getOperands() {
			return operands;
		}

		@Override
		public boolean containsColumn(String column) {
			for (DatabaseCriteria operand : operands) {
				if (operand.containsColumn(column))
					return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			int result = 0;
			for (DatabaseCriteria operand : operands) {
				result += operand.hashCode();
			}
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Or other = (Or)obj;
			Set<DatabaseCriteria> thisOps = new HashSet<>(
					Arrays.asList(operands));
			Set<DatabaseCriteria> otherOps = new HashSet<>(
					Arrays.asList(other.operands));
			if (!thisOps.equals(otherOps))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			if (operands.length == 0)
				return "";
			if (operands.length == 1)
				return operands[0].toString();
			StringBuilder result = new StringBuilder(
					"(" + operands[0].toString() + ")");
			for (int i = 1; i < operands.length; i++) {
				result.append(" OR (" + operands[i] + ")");
			}
			return result.toString();
		}
	}
}
