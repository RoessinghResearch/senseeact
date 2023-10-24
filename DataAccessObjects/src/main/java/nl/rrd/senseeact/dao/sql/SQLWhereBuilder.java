package nl.rrd.senseeact.dao.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.DatabaseObject;

/**
 * This class can convert {@link DatabaseCriteria DatabaseCriteria} to the
 * content of an SQL where clause. The constructor takes criteria. After
 * construction you can get the content with {@link #getWhere() getWhere()} and
 * {@link #getArgs() getArgs()}.
 *
 * @author Dennis Hofs (RRD)
 */
public class SQLWhereBuilder {
	private SQLDatabase database;
	private Map<String,String> tableColumns;
	private String where = null;
	private String[] args = null;

	/**
	 * Constructs a new SQL where builder.
	 *
	 * @param database the database
	 * @param table the table name
	 * @param criteria the database criteria
	 * @throws DatabaseException if a database error occurs
	 */
	public SQLWhereBuilder(SQLDatabase database, String table,
			Class<? extends DatabaseObject> dataClass,
			DatabaseCriteria criteria) throws DatabaseException {
		if (criteria == null)
			return;
		this.database = database;
		tableColumns = database.getTableColumns(table, dataClass);
		StringBuffer where = new StringBuffer();
		List<String> args = new ArrayList<>();
		buildCriteria(criteria, where, args);
		this.where = where.toString();
		this.args = args.toArray(new String[0]);
	}

	/**
	 * Returns the where string. This excludes the where keyword. The string
	 * may contain argument placeholders (a ? character). For every placeholder
	 * there should be an argument in {@link #getArgs() getArgs()}. Each
	 * placeholder will be replaced with an escaped string.
	 *
	 * @return the where string
	 */
	public String getWhere() {
		return where;
	}

	/**
	 * Returns the arguments for ? placeholders in the where string.
	 *
	 * @return the arguments
	 */
	public String[] getArgs() {
		return args;
	}

	/**
	 * Reads the specified criteria and appends its content to the specified
	 * where string and argument list.
	 *
	 * @param criteria the database criteria
	 * @param where the where string
	 * @param args the argument list
	 * @throws DatabaseException if a column in the criteria does not exist
	 */
	private void buildCriteria(DatabaseCriteria criteria, StringBuffer where,
			List<String> args) throws DatabaseException {
		if (criteria instanceof DatabaseCriteria.Equal) {
			buildEqual((DatabaseCriteria.Equal)criteria, where, args);
		} else if (criteria instanceof DatabaseCriteria.NotEqual) {
			buildNotEqual((DatabaseCriteria.NotEqual)criteria, where, args);
		} else if (criteria instanceof DatabaseCriteria.LessThan) {
			buildLessThan((DatabaseCriteria.LessThan)criteria, where, args);
		} else if (criteria instanceof DatabaseCriteria.GreaterThan) {
			buildGreaterThan((DatabaseCriteria.GreaterThan)criteria, where,
					args);
		} else if (criteria instanceof DatabaseCriteria.LessEqual) {
			buildLessEqual((DatabaseCriteria.LessEqual) criteria, where, args);
		} else if (criteria instanceof DatabaseCriteria.GreaterEqual) {
			buildGreaterEqual((DatabaseCriteria.GreaterEqual)criteria, where,
					args);
		} else if (criteria instanceof DatabaseCriteria.And) {
			buildAnd((DatabaseCriteria.And)criteria, where, args);
		} else if (criteria instanceof DatabaseCriteria.Or) {
			buildOr((DatabaseCriteria.Or)criteria, where, args);
		}
	}

	/**
	 * Appends the where content for the "equal" operator.
	 *
	 * @param criteria the criteria
	 * @param where the where string
	 * @param args the argument list
	 * @throws DatabaseException if the column in the criteria does not exist
	 */
	private void buildEqual(DatabaseCriteria.Equal criteria,
			StringBuffer where, List<String> args) throws DatabaseException {
		buildComparison(criteria.getColumn(),
				criteria.getValue() == null ? "IS" : "=",
				criteria.getValue(), where, args);
	}

	/**
	 * Appends the where content for the "not equal" operator.
	 *
	 * @param criteria the criteria
	 * @param where the where string
	 * @param args the argument list
	 * @throws DatabaseException if the column in the criteria does not exist
	 */
	private void buildNotEqual(DatabaseCriteria.NotEqual criteria,
			StringBuffer where, List<String> args) throws DatabaseException {
		buildComparison(criteria.getColumn(),
				criteria.getValue() == null ? "IS NOT" : "!=",
				criteria.getValue(), where, args);
	}

	/**
	 * Appends the where content for the "less than" operator.
	 *
	 * @param criteria the criteria
	 * @param where the where string
	 * @param args the argument list
	 * @throws DatabaseException if the column in the criteria does not exist
	 */
	private void buildLessThan(DatabaseCriteria.LessThan criteria,
			StringBuffer where, List<String> args) throws DatabaseException {
		buildComparison(criteria.getColumn(), "<", criteria.getValue(), where,
				args);
	}

	/**
	 * Appends the where content for the "greater than" operator.
	 *
	 * @param criteria the criteria
	 * @param where the where string
	 * @param args the argument list
	 * @throws DatabaseException if the column in the criteria does not exist
	 */
	private void buildGreaterThan(DatabaseCriteria.GreaterThan criteria,
			StringBuffer where, List<String> args) throws DatabaseException {
		buildComparison(criteria.getColumn(), ">", criteria.getValue(), where,
				args);
	}

	/**
	 * Appends the where content for the "less or equal" operator.
	 *
	 * @param criteria the criteria
	 * @param where the where string
	 * @param args the argument list
	 * @throws DatabaseException if the column in the criteria does not exist
	 */
	private void buildLessEqual(DatabaseCriteria.LessEqual criteria,
			StringBuffer where, List<String> args) throws DatabaseException {
		buildComparison(criteria.getColumn(), "<=", criteria.getValue(), where,
				args);
	}

	/**
	 * Appends the where content for the "greater or equal" operator.
	 *
	 * @param criteria the criteria
	 * @param where the where string
	 * @param args the argument list
	 * @throws DatabaseException if the column in the criteria does not exist
	 */
	private void buildGreaterEqual(DatabaseCriteria.GreaterEqual criteria,
			StringBuffer where, List<String> args) throws DatabaseException {
		buildComparison(criteria.getColumn(), ">=", criteria.getValue(), where,
				args);
	}

	/**
	 * Appends the where content for a binary comparison operator.
	 *
	 * @param column the column name
	 * @param op the operator
	 * @param value the comparison value
	 * @param where the where string
	 * @param args the argument list
	 * @throws DatabaseException if the column does not exist
	 */
	private void buildComparison(String column, String op, Object value,
			StringBuffer where, List<String> args) throws DatabaseException {
		if (column.equals("id")) {
			where.append(database.escapeName("_id"));
		} else {
			if (!tableColumns.containsKey(column)) {
				throw new DatabaseException(String.format(
						"Column \"%s\" not found", column));
			}
			where.append(database.getCompareColumn(column,
					tableColumns.get(column)));
		}
		where.append(" ");
		where.append(op);
		if (value == null) {
			where.append(" NULL");
		} else {
			where.append(" ?");
			args.add(value.toString());
		}
	}

	/**
	 * Appends the where content for the "and" operator.
	 *
	 * @param criteria the criteria
	 * @param where the where string
	 * @param args the argument list
	 * @throws DatabaseException if a column in the criteria does not exist
	 */
	private void buildAnd(DatabaseCriteria.And criteria,
			StringBuffer where, List<String> args) throws DatabaseException {
		boolean first = true;
		for (DatabaseCriteria op : criteria.getOperands()) {
			if (!first)
				where.append(" AND ");
			else
				first = false;
			where.append("(");
			buildCriteria(op, where, args);
			where.append(")");
		}
	}

	/**
	 * Appends the where content for the "or" operator.
	 *
	 * @param criteria the criteria
	 * @param where the where string
	 * @param args the argument list
	 * @throws DatabaseException if a column in the criteria does not exist
	 */
	private void buildOr(DatabaseCriteria.Or criteria,
			StringBuffer where, List<String> args) throws DatabaseException {
		boolean first = true;
		for (DatabaseCriteria op : criteria.getOperands()) {
			if (!first)
				where.append(" OR ");
			else
				first = false;
			where.append("(");
			buildCriteria(op, where, args);
			where.append(")");
		}
	}
}
