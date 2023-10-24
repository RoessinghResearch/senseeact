package nl.rrd.senseeact.dao;

import java.util.Map;

import nl.rrd.senseeact.dao.DatabaseCriteria.And;
import nl.rrd.senseeact.dao.DatabaseCriteria.Equal;
import nl.rrd.senseeact.dao.DatabaseCriteria.GreaterEqual;
import nl.rrd.senseeact.dao.DatabaseCriteria.GreaterThan;
import nl.rrd.senseeact.dao.DatabaseCriteria.LessEqual;
import nl.rrd.senseeact.dao.DatabaseCriteria.LessThan;
import nl.rrd.senseeact.dao.DatabaseCriteria.NotEqual;
import nl.rrd.senseeact.dao.DatabaseCriteria.Or;

/**
 * This class can check whether a database object matches specified criteria.
 * 
 * @author Dennis Hofs (RRD)
 */
public class DatabaseCriteriaMatcher {
	
	/**
	 * Returns whether a database object matches the specified criteria. 
	 * 
	 * @param object the database object
	 * @param criteria the criteria
	 * @return true if the record matches the criteria, false otherwise
	 */
	public static boolean matches(DatabaseObject object,
			DatabaseCriteria criteria) {
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		return matches(mapper.objectToMap(object, false), criteria);
	}
	
	/**
	 * Returns whether a database object matches the specified criteria. The
	 * object should be specified as a map that can be obtained from {@link
	 * DatabaseObjectMapper DatabaseObjectMapper}.
	 * 
	 * @param object the database object
	 * @param criteria the criteria
	 * @return true if the record matches the criteria, false otherwise
	 */
	public static boolean matches(Map<String,Object> object,
			DatabaseCriteria criteria) {
		if (criteria instanceof Equal) {
			return matchesEqual(object, (Equal)criteria);
		} else if (criteria instanceof NotEqual) {
			return matchesNotEqual(object, (NotEqual)criteria);
		} else if (criteria instanceof LessThan) {
			return matchesLessThan(object, (LessThan)criteria);
		} else if (criteria instanceof GreaterThan) {
			return matchesGreaterThan(object, (GreaterThan)criteria);
		} else if (criteria instanceof LessEqual) {
			return matchesLessEqual(object, (LessEqual)criteria);
		} else if (criteria instanceof GreaterEqual) {
			return matchesGreaterEqual(object, (GreaterEqual)criteria);
		} else if (criteria instanceof And) {
			return matchesAnd(object, (And)criteria);
		} else if (criteria instanceof Or) {
			return matchesOr(object, (Or)criteria);
		} else {
			throw new RuntimeException(
					"Subclass of DatabaseCriteria not supported: " +
					criteria.getClass().getName());
		}
	}
	
	/**
	 * Returns whether a database object matches the specified Equal criteria.
	 * 
	 * @param object the database object
	 * @param criteria the criteria
	 * @return true if the record matches the criteria, false otherwise
	 */
	private static boolean matchesEqual(Map<String,Object> object, Equal criteria) {
		Object val1 = object.get(criteria.getColumn());
		Object val2 = criteria.getValue();
		return PrimitiveValueComparison.isEqual(val1, val2);
	}

	/**
	 * Returns whether a database record matches the specified NotEqual
	 * criteria.
	 * 
	 * @param object the database object
	 * @param criteria the criteria
	 * @return true if the record matches the criteria, false otherwise
	 */
	private static boolean matchesNotEqual(Map<String,Object> object,
			NotEqual criteria) {
		Object val1 = object.get(criteria.getColumn());
		Object val2 = criteria.getValue();
		return !PrimitiveValueComparison.isEqual(val1, val2);
	}

	/**
	 * Returns whether a database record matches the specified LessThan
	 * criteria.
	 * 
	 * @param object the database object
	 * @param criteria the criteria
	 * @return true if the record matches the criteria, false otherwise
	 */
	private static boolean matchesLessThan(Map<String,Object> object,
			LessThan criteria) {
		Object val1 = object.get(criteria.getColumn());
		Object val2 = criteria.getValue();
		return PrimitiveValueComparison.isLessThan(val1, val2);
	}

	/**
	 * Returns whether a database record matches the specified GreaterThan
	 * criteria.
	 * 
	 * @param object the database object
	 * @param criteria the criteria
	 * @return true if the record matches the criteria, false otherwise
	 */
	private static boolean matchesGreaterThan(Map<String,Object> object,
			GreaterThan criteria) {
		Object val1 = object.get(criteria.getColumn());
		Object val2 = criteria.getValue();
		return PrimitiveValueComparison.isGreaterThan(val1, val2);
	}

	/**
	 * Returns whether a database record matches the specified LessEqual
	 * criteria.
	 * 
	 * @param object the database object
	 * @param criteria the criteria
	 * @return true if the record matches the criteria, false otherwise
	 */
	private static boolean matchesLessEqual(Map<String,Object> object,
			LessEqual criteria) {
		Object val1 = object.get(criteria.getColumn());
		Object val2 = criteria.getValue();
		return PrimitiveValueComparison.isLessEqual(val1, val2);
	}

	/**
	 * Returns whether a database record matches the specified GreaterEqual
	 * criteria.
	 * 
	 * @param object the database object
	 * @param criteria the criteria
	 * @return true if the record matches the criteria, false otherwise
	 */
	private static boolean matchesGreaterEqual(Map<String,Object> object,
			GreaterEqual criteria) {
		Object val1 = object.get(criteria.getColumn());
		Object val2 = criteria.getValue();
		return PrimitiveValueComparison.isGreaterEqual(val1, val2);
	}

	/**
	 * Returns whether a database record matches the specified And criteria.
	 * 
	 * @param object the database object
	 * @param criteria the criteria
	 * @return true if the record matches the criteria, false otherwise
	 */
	private static boolean matchesAnd(Map<String,Object> object, And criteria) {
		DatabaseCriteria[] ops = criteria.getOperands();
		for (DatabaseCriteria op : ops) {
			if (!matches(object, op))
				return false;
		}
		return true;
	}

	/**
	 * Returns whether a database record matches the specified Or criteria.
	 * 
	 * @param object the database object
	 * @param criteria the criteria
	 * @return true if the record matches the criteria, false otherwise
	 */
	private static boolean matchesOr(Map<String,Object> object, Or criteria) {
		DatabaseCriteria[] ops = criteria.getOperands();
		for (DatabaseCriteria op : ops) {
			if (matches(object, op))
				return true;
		}
		return false;
	}
}
