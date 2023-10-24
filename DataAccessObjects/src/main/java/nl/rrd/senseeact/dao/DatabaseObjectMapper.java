package nl.rrd.senseeact.dao;

import nl.rrd.utils.beans.PropertyReader;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * This class can map between {@link DatabaseObject DatabaseObject}s and data
 * maps for database storage.
 * 
 * <p>First it maps the ID field of the database object to key "id" with a
 * string value. Then it scans the Java class for fields with annotation {@link
 * DatabaseField DatabaseField}, which specifies a database type. A field
 * should be public or the class should have a public get and set method for
 * the field. A boolean field may have an is... method instead of a get...
 * method. The data map has keys that correspond to the field names. The values
 * should only be numbers or strings. The table below describes how the Java
 * object fields are converted from and to map values.</p>
 * 
 * <p>A set method may throw a {@link ParseException ParseException}. The {@link
 * #mapToObject(Map, Class, boolean) mapToObject()} value will handle it
 * specially as an extra validation. This can be useful for example if a field
 * contains a JSON string. The set method can try to parse the JSON string. If
 * it throws a {@link ParseException ParseException}, then {@link
 * #mapToObject(Map, Class, boolean) mapToObject()} will throw a {@link
 * DatabaseFieldException DatabaseFieldException}.</p>
 * 
 * <style type="text/css">
 *   table.bordered {
 *     border-collapse: collapse;
 *   }
 *   table.bordered th, table.bordered td {
 *     text-align: left;
 *     border: 1px solid black;
 *     padding: 4px 8px;
 *     vertical-align: top;
 *   }
 * </style>
 * <p><table class="bordered">
 * <caption>Database types</caption>
 * <tbody><tr>
 * <th>Database type</th>
 * <th>Java field type</th>
 * <th>From map value</th>
 * <th>To map value</th>
 * </tr><tr>
 * <td>{@link DatabaseType#BYTE BYTE}</td>
 * <td>boolean (false = 0, true = 1), byte, short, int, long,
 * {@link Boolean Boolean}, {@link Byte Byte}, {@link Short Short},
 * {@link Integer Integer}, {@link Long Long}</td>
 * <td>{@link Byte Byte}, {@link Short Short}, {@link Integer Integer},
 * {@link Long Long}, {@link Float Float}, {@link Double Double}</td>
 * <td>{@link Byte Byte}</td>
 * </tr><tr>
 * <td>{@link DatabaseType#SHORT SHORT}</td>
 * <td>boolean (false = 0, true = 1), byte, short, int, long,
 * {@link Boolean Boolean}, {@link Byte Byte}, {@link Short Short},
 * {@link Integer Integer}, {@link Long Long}</td>
 * <td>{@link Byte Byte}, {@link Short Short}, {@link Integer Integer},
 * {@link Long Long}, {@link Float Float}, {@link Double Double}</td>
 * <td>{@link Byte Byte}, {@link Short Short}</td>
 * </tr><tr>
 * <td>{@link DatabaseType#INT INT}</td>
 * <td>boolean (false = 0, true = 1), byte, short, int, long,
 * {@link Boolean Boolean}, {@link Byte Byte}, {@link Short Short},
 * {@link Integer Integer}, {@link Long Long}</td>
 * <td>{@link Byte Byte}, {@link Short Short}, {@link Integer Integer},
 * {@link Long Long}, {@link Float Float}, {@link Double Double}</td>
 * <td>{@link Byte Byte}, {@link Short Short}, {@link Integer Integer}</td>
 * </tr><tr>
 * <td>{@link DatabaseType#LONG LONG}</td>
 * <td>boolean (false = 0, true = 1), byte, short, int, long,
 * {@link Boolean Boolean}, {@link Byte Byte}, {@link Short Short},
 * {@link Integer Integer}, {@link Long Long}</td>
 * <td>{@link Byte Byte}, {@link Short Short}, {@link Integer Integer},
 * {@link Long Long}, {@link Float Float}, {@link Double Double}</td>
 * <td>{@link Byte Byte}, {@link Short Short}, {@link Integer Integer},
 * {@link Long Long}</td>
 * </tr><tr>
 * <td>{@link DatabaseType#FLOAT FLOAT}</td>
 * <td>float, double, {@link Float Float}, {@link Double Double}</td>
 * <td>{@link Byte Byte}, {@link Short Short}, {@link Integer Integer},
 * {@link Long Long}, {@link Float Float}, {@link Double Double}</td>
 * <td>{@link Float Float}</td>
 * </tr><tr>
 * <td>{@link DatabaseType#DOUBLE DOUBLE}</td>
 * <td>float, double, {@link Float Float}, {@link Double Double}</td>
 * <td>{@link Byte Byte}, {@link Short Short}, {@link Integer Integer},
 * {@link Long Long}, {@link Float Float}, {@link Double Double}</td>
 * <td>{@link Float Float}, {@link Double Double}</td>
 * </tr><tr>
 * <td>{@link DatabaseType#STRING STRING}</td>
 * <td>{@link String String}, enum</td>
 * <td>{@link String String}. For an enum it tries to call valueOf(String)
 * (takes the name of an enum value). If that fails, it tries
 * fromStringValue(String) (if it is defined on the enum class).</td>
 * <td>{@link String String}. For an enum it calls toString(). Its default
 * implementation returns the name of the enum value, but an enum class may
 * override toString().</td>
 * </tr><tr>
 * <td>{@link DatabaseType#TEXT TEXT}</td>
 * <td>{@link String String}</td>
 * <td>{@link String String}</td>
 * <td>{@link String String}</td>
 * </tr><tr>
 * <td>{@link DatabaseType#DATE DATE}</td>
 * <td>{@link LocalDate LocalDate}</td>
 * <td>{@link String String} in format yyyy-MM-dd</td>
 * <td>{@link String String} in format yyyy-MM-dd</td>
 * </tr><tr>
 * <td>{@link DatabaseType#TIME TIME}</td>
 * <td>{@link LocalTime LocalTime}</td>
 * <td>{@link String String} in format HH:mm:ss</td>
 * <td>{@link String String} in format HH:mm:ss (milliseconds are not
 * stored)</td>
 * </tr><tr>
 * <td>{@link DatabaseType#DATETIME DATETIME}</td>
 * <td>{@link LocalDateTime LocalDateTime}</td>
 * <td>{@link String String} in format yyyy-MM-dd HH:mm:ss</td>
 * <td>{@link String String} in format yyyy-MM-dd HH:mm:ss (milliseconds are
 * not stored)</td>
 * </tr><tr>
 * <td>{@link DatabaseType#ISOTIME ISOTIME}</td>
 * <td>{@link ZonedDateTime ZonedDateTime} (preferred), {@link Calendar
 * Calendar} (preferred), long (time in milliseconds), {@link Long Long}, {@link
 * Date Date}, {@link Instant Instant}</td>
 * <td>{@link String String} in format yyyy-MM-dd'T'HH:mm:ss.SSSXXX (for long,
 * Long, Date and Instant, the time is translated to UTC and the time zone is
 * lost)</td>
 * <td>{@link String String} in format yyyy-MM-dd'T'HH:mm:ss.SSSXXX (long, Long,
 * Date and Instant, which don't specify a timezone, are translated to the
 * default time zone)</td>
 * </tr></tbody></table></p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class DatabaseObjectMapper {
	
	/**
	 * Converts the specified object to a data map with a key for each object
	 * field that is annotated with {@link DatabaseField DatabaseField}. If the
	 * object ID is not null, it also sets key "id". See the table at the top
	 * of this page for the returned values.
	 * 
	 * @param obj the database object
	 * @param decodeJson if JSON strings should be decoded
	 * @return the data map
	 */
	public Map<String,Object> objectToMap(DatabaseObject obj,
			boolean decodeJson) {
		Map<String,Object> result = new LinkedHashMap<>();
		if (obj.getId() != null)
			result.put("id", obj.getId());
		List<DatabaseFieldSpec> fields = DatabaseFieldScanner.getDatabaseFields(
				obj.getClass());
		for (DatabaseFieldSpec field : fields) {
			Object value = PropertyReader.readProperty(obj,
					field.getPropSpec());
			result.put(field.getPropSpec().getName(), toDatabaseValue(value,
					field.getDbField(), decodeJson));
		}
		return result;
	}
	
	/**
	 * Converts the specified data map to an object of the specified class.
	 * It will read the key "id" from the map and write its string value to the
	 * database object. If there is no key "id", the ID in the object will be
	 * set to null. Then it scans the class for fields that are annotated with
	 * {@link DatabaseField DatabaseField}. For each field that occurs in the
	 * map, the value from the map will be converted and written to the object.
	 * The keys in the map may be in lower case.
	 * 
	 * @param map the data map (keys may be in lower case)
	 * @param clazz the class of database object
	 * @param encodeJson if the map contains objects that need to be encoded to
	 * a JSON string. This is done for each database field that is marked as a
	 * JSON field.
	 * @param <T> the type of database object
	 * @return the database object
	 * @throws DatabaseFieldException if the map value of a database field can't
	 * be converted
	 */
	public <T extends DatabaseObject> T mapToObject(Map<?,?> map,
			Class<? extends T> clazz, boolean encodeJson)
			throws DatabaseFieldException {
		T result = null;
		String error = null;
		Throwable exception = null;
		try {
			result = clazz.getConstructor().newInstance();
		} catch (NoSuchMethodException | IllegalAccessException ex) {
			error = ex.getMessage();
			exception = ex;
		} catch (InvocationTargetException | InstantiationException ex) {
			error = ex.getCause().getMessage();
			exception = ex.getCause();
		}
		if (exception != null) {
			throw new RuntimeException("Can't instantiate class \"" +
					clazz.getName() + "\": " + error, exception);
		}
		Object mapId = map.get("id");
		result.setId(mapId != null ? mapId.toString() : null);
		List<DatabaseFieldSpec> fields =
				DatabaseFieldScanner.getDatabaseFields(clazz);
		for (DatabaseFieldSpec field : fields) {
			String fieldName = field.getPropSpec().getName();
			if (!mapContainsKeyCaseInsensitive(map, fieldName)) {
				continue;
			}
			Object mapVal = getMapCaseInsensitive(map, fieldName);
			Object value;
			try {
				value = fromDatabaseValue(mapVal, field.getDbField(),
						field.getPropSpec().getField(), encodeJson);
			} catch (IllegalArgumentException ex) {
				throw new DatabaseFieldException(String.format(
						"Invalid value for field \"%s\": ", fieldName) +
						mapVal + ": " + ex.getMessage(), fieldName, ex);
			}
			try {
				if (field.getPropSpec().isPublic()) {
					field.getPropSpec().getField().set(result, value);
				} else {
					field.getPropSpec().getSetMethod().invoke(result, value);
				}
			} catch (IllegalAccessException | IllegalArgumentException ex) {
				error = ex.getMessage();
				exception = ex;
			} catch (InvocationTargetException ex) {
				if (ex.getCause() instanceof ParseException) {
					ParseException parseEx = (ParseException)ex.getCause();
					throw new DatabaseFieldException(String.format(
						"Invalid value for field \"%s\": ", fieldName) +
						mapVal + ": " + parseEx.getMessage(), fieldName,
						parseEx);
				} else {
					error = ex.getCause().getMessage();
					exception = ex.getCause();
				}
			}
			if (exception != null) {
				throw new RuntimeException("Can't write field \"" +
						field.getPropSpec().getName() + "\": " + error,
						exception);
			}
		}
		return result;
	}

	/**
	 * Returns whether the map contains the specified key, using
	 * case-insensitive string comparison. The map should have string keys that
	 * are not null.
	 *
	 * @param map the map with string keys
	 * @param key the key
	 * @return true if the map contains the key, false otherwise
	 */
	private boolean mapContainsKeyCaseInsensitive(Map<?,?> map, String key) {
		if (map.containsKey(key))
			return true;
		for (Object mapKeyObj : map.keySet()) {
			String mapKey = (String)mapKeyObj;
			if (mapKey.equalsIgnoreCase(key))
				return true;
		}
		return false;
	}

	/**
	 * Finds a key in a map using case-insensitive string comparison. The
	 * specified map should have string keys that are not null.
	 *
	 * @param map the map with string keys
	 * @param key the key
	 * @return the map value or null
	 */
	private Object getMapCaseInsensitive(Map<?,?> map, String key) {
		if (map.containsKey(key))
			return map.get(key);
		for (Object mapKeyObj : map.keySet()) {
			String mapKey = (String)mapKeyObj;
			if (mapKey.equalsIgnoreCase(key))
				return map.get(mapKey);
		}
		return null;
	}

	/**
	 * Converts a value from a {@link DatabaseObject DatabaseObject} to a value
	 * that can be written to the database. The returned database value should
	 * be a string or a number. The conversion depends on the specified data
	 * type. See also the table at the top of this page.
	 * 
	 * @param value the value from the database object
	 * @param dbField the database field
	 * @param decodeJson if JSON strings should be decoded. This is done if the
	 * database field is marked as a JSON field.
	 * @return the database value
	 * @throws IllegalArgumentException if the data object contains JSON code
	 * that cannot be parsed
	 */
	public Object toDatabaseValue(Object value, DatabaseField dbField,
			boolean decodeJson) {
		if (value == null)
			return null;
		DateTimeFormatter formatter;
		switch (dbField.value()) {
		case BYTE:
			if (value instanceof Boolean) {
				boolean b = (Boolean)value;
				return b ? (byte)1 : (byte)0;
			} else {
				return ((Number)value).byteValue();
			}
		case SHORT:
			if (value instanceof Boolean) {
				boolean b = (Boolean)value;
				return b ? (byte)1 : (byte)0;
			} else if (value instanceof Byte) {
				return value;
			} else {
				return ((Number)value).shortValue();
			}
		case INT:
			if (value instanceof Boolean) {
				boolean b = (Boolean) value;
				return b ? (byte)1 : (byte)0;
			} else if (value instanceof Byte ||
					value instanceof Short ||
					value instanceof Integer) {
				return value;
			} else {
				return ((Number)value).intValue();
			}
		case LONG:
			if (value instanceof Boolean) {
				boolean b = (Boolean) value;
				return b ? (byte)1 : (byte)0;
			} else if (value instanceof Byte ||
					value instanceof Short ||
					value instanceof Integer ||
					value instanceof Long) {
				return value;
			} else {
				return ((Number)value).longValue();
			}
		case FLOAT:
			return ((Number)value).floatValue();
		case DOUBLE:
			if (value instanceof Float) {
				return value;
			} else {
				return ((Number)value).doubleValue();
			}
		case STRING:
		case TEXT:
			if (dbField.json() && decodeJson) {
				try {
					return JsonMapper.parse(value.toString(), Object.class);
				} catch (ParseException ex) {
					throw new IllegalArgumentException(
							"Failed to parse JSON code: " + ex.getMessage(),
							ex);
				}
			} else {
				return value.toString();
			}
		case DATE:
			return DateTimeUtils.DATE_FORMAT.format((LocalDate)value);
		case TIME:
			formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
			return formatter.format((LocalTime)value);
		case DATETIME:
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			return formatter.format((LocalDateTime)value);
		case ISOTIME:
			ZonedDateTime time = getDateTimeValue(value);
			return DateTimeUtils.ZONED_FORMAT.format(time);
		default:
			break;
		}
		throw new RuntimeException("Unknown database type: " + dbField.value());
	}

	/**
	 * Takes the value for a date/time field from a {@link DatabaseObject
	 * DatabaseObject} (Long, Date, Instant, Calendar or ZonedDateTime) and
	 * returns a ZonedDateTime. Long, Date and Instant are converted to a
	 * ZonedDateTime with the default time zone.
	 * 
	 * @param value the date/time value
	 * @return the DateTime object
	 */
	private ZonedDateTime getDateTimeValue(Object value) {
		if (value instanceof Long) {
			Instant instant = Instant.ofEpochMilli((Long)value);
			return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
		} else if (value instanceof Date) {
			Instant instant = ((Date)value).toInstant();
			return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
		} else if (value instanceof Instant) {
			Instant instant = (Instant)value;
			return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
		} else if (value instanceof Calendar) {
			Calendar cal = (Calendar)value;
			return ZonedDateTime.ofInstant(cal.toInstant(),
					cal.getTimeZone().toZoneId());
		} else if (value instanceof ZonedDateTime) {
			return (ZonedDateTime)value;
		} else {
			throw new IllegalArgumentException("Invalid date/time value: " +
					value);
		}
	}

	/**
	 * Converts a value from the database to a value that is written to a
	 * {@link DatabaseObject DatabaseObject}. The database value should be a
	 * string or a number. The conversion depends on the specified data type.
	 * The table at the top of this page shows to what classes the data type
	 * can be converted. You should specify one of those classes, normally
	 * the class of the field to which the value will be written.
	 * 
	 * @param value the database value
	 * @param dbField the database field
	 * @param field the field to which the value will be written. This method
	 * does not actually write the value. It only uses this parameter to get
	 * the data type.
	 * @param encodeJson if the value needs to be encoded to a JSON string. This
	 * is done if the database field is marked as a JSON field.
	 * @return the value for the database object
	 * @throws IllegalArgumentException if the value can't be converted to the
	 * specified database field
	 */
	public Object fromDatabaseValue(Object value, DatabaseField dbField,
			Field field, boolean encodeJson) throws IllegalArgumentException {
		if (value == null)
			return null;
		DatabaseType dbType = dbField.value();
		Class<?> clazz = field.getType();
		Number n;
		DateTimeFormatter parser;
		switch (dbType) {
		case BYTE:
		case SHORT:
		case INT:
		case LONG:
			if (!(value instanceof Number)) {
				throw new IllegalArgumentException(
						"Invalid class for database type " + dbType + ": " +
						value.getClass().getName());
			}
			n = (Number)value;
			if (clazz == Boolean.TYPE || clazz == Boolean.class) {
				return n.longValue() != 0;
			} else if (clazz == Byte.TYPE || clazz == Byte.class) {
				return n.byteValue();
			} else if (clazz == Short.TYPE || clazz == Short.class) {
				return n.shortValue();
			} else if (clazz == Integer.TYPE || clazz == Integer.class) {
				return n.intValue();
			} else if (clazz == Long.TYPE || clazz == Long.class) {
				return n.longValue();
			} else {
				throw new IllegalArgumentException(
						"Invalid class for database type " + dbType + ": " +
						value.getClass().getName());
			}
		case FLOAT:
		case DOUBLE:
			if (!(value instanceof Number)) {
				throw new IllegalArgumentException(
						"Invalid class for database type " + dbType + ": " +
						value.getClass().getName());
			}
			n = (Number)value;
			if (clazz == Float.TYPE || clazz == Float.class) {
				return n.floatValue();
			} else if (clazz == Double.TYPE || clazz == Double.class) {
				return n.doubleValue();
			} else {
				throw new IllegalArgumentException(
						"Invalid class for database type " + dbType + ": " +
						value.getClass().getName());
			}
		case STRING:
			if (clazz.isEnum()) {
				try {
					Method method = clazz.getMethod("valueOf", String.class);
					return method.invoke(null, value.toString());
				} catch (NoSuchMethodException | IllegalAccessException |
						InvocationTargetException ex) {
				}
				try {
					Method method = clazz.getMethod("fromStringValue",
							String.class);
					return method.invoke(null, value.toString());
				} catch (NoSuchMethodException | IllegalAccessException |
						InvocationTargetException ex) {
				}
				throw new IllegalArgumentException(
						"Invalid value for enum type " + clazz.getName() +
						": " + value);
			} else {
				if (dbField.json() && encodeJson)
					return JsonMapper.generate(value);
				else
					return value.toString();
			}
		case TEXT:
			if (dbField.json() && encodeJson)
				return JsonMapper.generate(value);
			else
				return value.toString();
		case DATE:
			parser = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			try {
				return parser.parse(value.toString(), LocalDate::from);
			} catch (DateTimeParseException ex) {
				throw new IllegalArgumentException(
						"Invalid value for database type " + dbType + ": " +
						value + ": " + ex.getMessage(), ex);
			}
		case TIME:
			parser = DateTimeFormatter.ofPattern("HH:mm:ss");
			try {
				return parser.parse(value.toString(), LocalTime::from);
			} catch (DateTimeParseException ex) {
				throw new IllegalArgumentException(
						"Invalid value for database type " + dbType + ": " +
						value + ": " + ex.getMessage(), ex);
			}
		case DATETIME:
			parser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			try {
				return parser.parse(value.toString(), LocalDateTime::from);
			} catch (DateTimeParseException ex) {
				throw new IllegalArgumentException(
						"Invalid value for database type " + dbType + ": " +
						value + ": " + ex.getMessage(), ex);
			}
		case ISOTIME:
			try {
				return DateTimeUtils.parseDateTime(value.toString(), clazz);
			} catch (ParseException ex) {
				throw new IllegalArgumentException(ex.getMessage(), ex);
			}
		default:
			break;
		}
		throw new RuntimeException("Unknown database type: " + dbType);
	}
}
