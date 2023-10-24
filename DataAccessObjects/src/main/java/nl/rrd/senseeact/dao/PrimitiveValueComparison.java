package nl.rrd.senseeact.dao;

/**
 * This class can be used to compare primitive database values. The values must
 * be numbers or strings or null.
 * 
 * @author Dennis Hofs (RRD)
 */
public class PrimitiveValueComparison {
	
	/**
	 * Normalizes the specified value, so that two equal values have the same
	 * hash code.
	 * 
	 * @param val the value (can be null)
	 * @return the normalized value (can be null)
	 */
	public static Object normalizeValue(Object val) {
		if (val == null)
			return null;
		if (isInt(val))
			return ((Number)val).longValue();
		if (val instanceof Number)
			return ((Number)val).doubleValue();
		return val.toString();
	}
	
	/**
	 * Returns whether the two specified values are equal.
	 * 
	 * @param val1 the first value
	 * @param val2 the second value
	 * @return true if the values are equal, false otherwise
	 */
	public static boolean isEqual(Object val1, Object val2) {
		if (val1 == null && val2 == null)
			return true;
		if (val1 == null || val2 == null)
			return false;
		if (isInt(val1) && isInt(val2))
			return ((Number)val1).longValue() == ((Number)val2).longValue();
		if (val1 instanceof Number && val2 instanceof Number)
			return ((Number)val1).doubleValue() == ((Number)val2).doubleValue();
		return val1.toString().equals(val2.toString());
	}

	/**
	 * Returns whether a value is less than another value. A null value is
	 * less than a non-null value.
	 * 
	 * @param val1 the first value
	 * @param val2 the second value
	 * @return true if the first value is less than the second value, false
	 * otherwise
	 */
	public static boolean isLessThan(Object val1, Object val2) {
		if (val1 == null && val2 == null)
			return false;
		if (val1 == null)
			return true;
		if (val2 == null)
			return false;
		if (isInt(val1) && isInt(val2))
			return ((Number)val1).longValue() < ((Number)val2).longValue();
		if (val1 instanceof Number && val2 instanceof Number)
			return ((Number)val1).doubleValue() < ((Number)val2).doubleValue();
		return val1.toString().compareTo(val2.toString()) < 0;
	}
	
	/**
	 * Returns whether a value is less than or equal to another value. A null
	 * value is less than a non-null value.
	 * 
	 * @param val1 the first value
	 * @param val2 the second value
	 * @return if the first value is less than or equal to the second value,
	 * false otherwise
	 */
	public static boolean isLessEqual(Object val1, Object val2) {
		if (val1 == null && val2 == null)
			return true;
		if (val1 == null)
			return true;
		if (val2 == null)
			return false;
		if (isInt(val1) && isInt(val2))
			return ((Number)val1).longValue() <= ((Number)val2).longValue();
		if (val1 instanceof Number && val2 instanceof Number)
			return ((Number)val1).doubleValue() <= ((Number)val2).doubleValue();
		return val1.toString().compareTo(val2.toString()) <= 0;
	}
	
	/**
	 * Returns whether a value is greater than another value. A null value is
	 * less than a non-null value.
	 * 
	 * @param val1 the first value
	 * @param val2 the second value
	 * @return true if the first value is greater than the second value, false
	 * otherwise
	 */
	public static boolean isGreaterThan(Object val1, Object val2) {
		if (val1 == null && val2 == null)
			return false;
		if (val1 == null)
			return false;
		if (val2 == null)
			return true;
		if (isInt(val1) && isInt(val2))
			return ((Number)val1).longValue() > ((Number)val2).longValue();
		if (val1 instanceof Number && val2 instanceof Number)
			return ((Number)val1).doubleValue() > ((Number)val2).doubleValue();
		return val1.toString().compareTo(val2.toString()) > 0;
	}
	
	/**
	 * Returns whether a value is greater than or equal to another value. A
	 * null value is less than a non-null value.
	 * 
	 * @param val1 the first value
	 * @param val2 the second value
	 * @return if the first value is greater than or equal to the second value,
	 * false otherwise
	 */
	public static boolean isGreaterEqual(Object val1, Object val2) {
		if (val1 == null && val2 == null)
			return true;
		if (val1 == null)
			return false;
		if (val2 == null)
			return true;
		if (isInt(val1) && isInt(val2))
			return ((Number)val1).longValue() >= ((Number)val2).longValue();
		if (val1 instanceof Number && val2 instanceof Number)
			return ((Number)val1).doubleValue() >= ((Number)val2).doubleValue();
		return val1.toString().compareTo(val2.toString()) >= 0;
	}

	/**
	 * Returns whether the specified object is an integer object (Byte, Short,
	 * Integer or Long).
	 * 
	 * @param obj the object
	 * @return true if it is an integer object, false otherwise
	 */
	private static boolean isInt(Object obj) {
		return obj instanceof Byte || obj instanceof Short ||
				obj instanceof Integer || obj instanceof Long;
	}
}
