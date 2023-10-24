package nl.rrd.senseeact.dao;

import java.util.Comparator;
import java.util.Map;

public class DatabaseObjectMapComparator implements Comparator<Map<String,?>> {
	private DatabaseSort[] sort;

	public DatabaseObjectMapComparator(DatabaseSort[] sort) {
		this.sort = sort;
	}

	@Override
	public int compare(Map<String,?> o1, Map<String,?> o2) {
		if (o1 == null && o2 == null)
			return 0;
		if (o1 == null)
			return 1;
		if (o2 == null)
			return -1;
		for (DatabaseSort sortCol : sort) {
			Object val1 = o1.get(sortCol.getColumn());
			Object val2 = o2.get(sortCol.getColumn());
			if (val1 == null && val2 == null)
				continue;
			if (val1 == null)
				return 1;
			if (val2 == null)
				return -1;
			if (isInt(val1) && isInt(val2)) {
				long n1 = ((Number)val1).longValue();
				long n2 = ((Number)val2).longValue();
				if (n1 < n2)
					return -1;
				else if (n1 > n2)
					return 1;
			} else if (val1 instanceof Number && val2 instanceof Number) {
				double n1 = ((Number)val1).doubleValue();
				double n2 = ((Number)val2).doubleValue();
				if (n1 < n2)
					return -1;
				else if (n1 > n2)
					return 1;
			} else {
				String s1 = val1.toString();
				String s2 = val2.toString();
				int cmp = s1.compareTo(s2);
				if (cmp != 0)
					return cmp;
			}
		}
		return 0;
	}

	/**
	 * Returns whether the specified object is an integer object (Byte, Short,
	 * Integer or Long).
	 *
	 * @param obj the object
	 * @return true if it is an integer object, false otherwise
	 */
	private boolean isInt(Object obj) {
		return obj instanceof Byte || obj instanceof Short ||
				obj instanceof Integer || obj instanceof Long;
	}
}
