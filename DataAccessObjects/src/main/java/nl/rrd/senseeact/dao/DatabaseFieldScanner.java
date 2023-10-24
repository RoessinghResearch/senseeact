package nl.rrd.senseeact.dao;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.rrd.utils.beans.PropertyScanner;
import nl.rrd.utils.beans.PropertySpec;

/**
 * This scanner can scan a {@link DatabaseObject DatabaseObject} class for
 * fields that are annotated with {@link DatabaseField DatabaseField} and
 * returns specifications for those fields.
 * 
 * @author Dennis Hofs (RRD)
 */
public class DatabaseFieldScanner {
	private static final int MAX_CACHE_SIZE = 100;
	private static final Object LOCK = new Object();
	private static final Map<Class<? extends DatabaseObject>,List<DatabaseFieldSpec>> cache =
			new LinkedHashMap<Class<? extends DatabaseObject>,List<DatabaseFieldSpec>>();

	/**
	 * Scans the specified class and returns specifications for the database
	 * fields. This excludes the "id" field, which should not be annotated as
	 * a database field.
	 * 
	 * @param clazz the class
	 * @return the database fields
	 */
	public static List<DatabaseFieldSpec> getDatabaseFields(
			Class<? extends DatabaseObject> clazz) {
		synchronized (LOCK) {
			if (cache.containsKey(clazz)) {
				// move item to end (most recently used)
				List<DatabaseFieldSpec> result = cache.remove(clazz);
				cache.put(clazz, result);
				return result;
			}
		}
		List<DatabaseFieldSpec> result = new ArrayList<DatabaseFieldSpec>();
		if (DatabaseObject.class.isAssignableFrom(clazz.getSuperclass())) {
			result.addAll(getDatabaseFields(clazz.getSuperclass().asSubclass(
					DatabaseObject.class)));
		}
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			DatabaseField annot = field.getAnnotation(DatabaseField.class);
			if (annot == null)
				continue;
			PropertySpec propSpec = PropertyScanner.getProperty(clazz,
					field.getName());
			DatabaseFieldSpec spec = new DatabaseFieldSpec(propSpec, annot);
			result.add(spec);
		}
		synchronized (LOCK) {
			if (cache.size() == MAX_CACHE_SIZE) {
				Class<? extends DatabaseObject> oldest =
						cache.keySet().iterator().next();
				cache.remove(oldest);
			}
			cache.put(clazz, result);
		}
		return result;
	}

	/**
	 * Scans the specified class and returns the names of the database fields.
	 * This excludes the "id" field, which should not be annotated as a
	 * database field.
	 *
	 * @param clazz the class
	 * @return the names of the database fields
	 */
	public static List<String> getDatabaseFieldNames(
			Class<? extends DatabaseObject> clazz) {
		List<DatabaseFieldSpec> fields = getDatabaseFields(clazz);
		List<String> names = new ArrayList<String>();
		for (DatabaseFieldSpec field : fields) {
			names.add(field.getPropSpec().getName());
		}
		return names;
	}
}
