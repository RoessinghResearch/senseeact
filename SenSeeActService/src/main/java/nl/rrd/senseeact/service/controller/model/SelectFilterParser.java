package nl.rrd.senseeact.service.controller.model;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;
import nl.rrd.senseeact.dao.*;

import java.util.*;

public class SelectFilterParser {
	private static final List<String> FORBIDDEN_FIELDS = Arrays.asList("id",
			"user");
	
	private DatabaseTableDef<?> table;
	private DatabaseObjectMapper dbMapper;
	private List<DatabaseFieldSpec> dbFields;
	
	public SelectFilterParser(DatabaseTableDef<?> table) {
		this.table = table;
		dbMapper = new DatabaseObjectMapper();
		dbFields = DatabaseFieldScanner.getDatabaseFields(
				table.getDataClass());
	}
	
	public DatabaseCriteria parseFilter(Map<?,?> map) throws ParseException {
		return parseFilterMap(map);
	}
	
	private DatabaseCriteria parseFilterMap(Object value)
			throws ParseException {
		if (value == null) {
			throw new ParseException("Expected filter object, found null");
		}
		if (!(value instanceof Map)) {
			throw new ParseException("Expected filter object, found: " +
					value.getClass().getName());
		}
		Map<?,?> map = (Map<?,?>)value;
		if (map.size() != 1) {
			throw new ParseException(String.format(
					"Filter object has %s entries, expected 1", map.size()));
		}
		Object key = map.keySet().iterator().next();
		if (key == null) {
			throw new ParseException(
					"Expected string key in filter object, found null");
		}
		if (!(key instanceof String)) {
			throw new ParseException(
					"Expected string key in filter object, found: " +
					key.getClass().getName());
		}
		String strKey = (String)key;
		if (strKey.equals("$and"))
			return parseFilterAnd(map.get(strKey));
		if (strKey.equals("$or"))
			return parseFilterOr(map.get(strKey));
		return parseFilterField(strKey, map.get(strKey));
	}
	
	private DatabaseCriteria parseFilterAnd(Object value)
			throws ParseException {
		if (value == null) {
			throw new ParseException("Expected list after AND, found null");
		}
		if (!(value instanceof List)) {
			throw new ParseException("Expected list after AND, found: " +
					value.getClass().getName());
		}
		List<DatabaseCriteria> andList = new ArrayList<>();
		List<?> valueList = (List<?>)value;
		for (Object item : valueList) {
			andList.add(parseFilterMap(item));
		}
		if (andList.isEmpty())
			throw new ParseException("Empty AND list");
		if (andList.size() == 1)
			return andList.get(0);
		return new DatabaseCriteria.And(andList.toArray(
				new DatabaseCriteria[0]));
	}

	private DatabaseCriteria parseFilterOr(Object value) throws ParseException {
		if (value == null) {
			throw new ParseException("Expected list after OR, found null");
		}
		if (!(value instanceof List)) {
			throw new ParseException("Expected list after OR, found: " +
					value.getClass().getName());
		}
		List<DatabaseCriteria> orList = new ArrayList<>();
		List<?> valueList = (List<?>)value;
		for (Object item : valueList) {
			orList.add(parseFilterMap(item));
		}
		if (orList.isEmpty())
			throw new ParseException("Empty OR list");
		if (orList.size() == 1)
			return orList.get(0);
		return new DatabaseCriteria.Or(orList.toArray(new DatabaseCriteria[0]));
	}
	
	private DatabaseCriteria parseFilterField(String field, Object value)
			throws ParseException {
		if (FORBIDDEN_FIELDS.contains(field)) {
			throw new ParseException("Filter on field \"" + field +
					"\" not allowed");
		}
		DatabaseFieldSpec dbField = findDatabaseField(field);
		if (dbField == null) {
			throw new ParseException(String.format(
					"Field \"%s\" not found in table \"%s\"",
					field, table.getName()));
		}
		if (value instanceof Map)
			return parseFilterFieldOperator(field, (Map<?,?>)value);
		Object normValue = parseFilterFieldValue(field, value);
		if (normValue == null || normValue instanceof String)
			return new DatabaseCriteria.Equal(field, (String)normValue);
		if (normValue instanceof Number)
			return new DatabaseCriteria.Equal(field, (Number)normValue);
		throw new RuntimeException(String.format(
				"Value for field \"%s\" in table \"%s\" is not a string or number",
				field, table.getName()));
	}
	
	private DatabaseFieldSpec findDatabaseField(String field) {
		for (DatabaseFieldSpec dbField : dbFields) {
			if (dbField.getPropSpec().getName().equals(field))
				return dbField;
		}
		return null;
	}
	
	private Object parseFilterFieldValue(String field, Object value)
			throws ParseException {
		Map<String,Object> objMap = new LinkedHashMap<>();
		objMap.put(field, value);
		DatabaseObject dbObj;
		try {
			dbObj = dbMapper.mapToObject(objMap, table.getDataClass(), false);
		} catch (DatabaseFieldException ex) {
			throw new ParseException(ex.getMessage(), ex);
		}
		objMap = dbMapper.objectToMap(dbObj, false);
		return objMap.get(field);
	}
	
	private DatabaseCriteria parseFilterFieldOperator(String field,
			Map<?,?> map) throws ParseException {
		if (map.isEmpty()) {
			throw new ParseException("Empty select object after field \"" +
					field + "\"");
		}
		if (map.size() != 1) {
			throw new ParseException(String.format(
					"Select object after field \"%s\" has %s entries, expected 1",
					field, map.size()));
		}
		Object key = map.keySet().iterator().next();
		if (key == null) {
			throw new ParseException(
					"Expected string key in select object after field \"" +
					field + "\", found null");
		}
		if (!(key instanceof String)) {
			throw new ParseException(
					"Expected string key in select object after field \"" +
					field + "\", found: " + key.getClass().getName());
		}
		String strKey = (String)key;
		Object normValue = parseFilterFieldValue(field, map.get(strKey));
		if (normValue == null || normValue instanceof String) {
			return parseFilterStringFieldOperator(field, strKey,
					(String)normValue);
		} else if (normValue instanceof Number) {
			return parseFilterNumberFieldOperator(field, strKey,
					(Number)normValue);
		}
		throw new RuntimeException(String.format(
				"Value for field \"%s\" in table \"%s\" is not a string or number",
				field, table.getName()));
	}
	
	private DatabaseCriteria parseFilterStringFieldOperator(String field,
			String op, String value) throws ParseException {
		if (op.equals("$ne"))
			return new DatabaseCriteria.NotEqual(field, value);
		if (op.equals("$gt"))
			return new DatabaseCriteria.GreaterThan(field, value);
		if (op.equals("$lt"))
			return new DatabaseCriteria.LessThan(field, value);
		if (op.equals("$le"))
			return new DatabaseCriteria.LessEqual(field, value);
		if (op.equals("$ge"))
			return new DatabaseCriteria.GreaterEqual(field, value);
		throw new ParseException(String.format(
				"Invalid operator \"%s\" at field \"%s\"", op, field));
	}

	private DatabaseCriteria parseFilterNumberFieldOperator(String field,
			String op, Number value) throws ParseException {
		if (op.equals("$ne"))
			return new DatabaseCriteria.NotEqual(field, value);
		if (op.equals("$gt"))
			return new DatabaseCriteria.GreaterThan(field, value);
		if (op.equals("$lt"))
			return new DatabaseCriteria.LessThan(field, value);
		if (op.equals("$le"))
			return new DatabaseCriteria.LessEqual(field, value);
		if (op.equals("$ge"))
			return new DatabaseCriteria.GreaterEqual(field, value);
		throw new ParseException(String.format(
				"Invalid operator \"%s\" at field \"%s\"", op, field));
	}
	
	public DatabaseSort[] parseSort(Object sort) throws ParseException {
		List<DatabaseSort> list = JsonMapper.convert(sort,
				new TypeReference<List<DatabaseSort>>() {});
		for (DatabaseSort item : list) {
			if (item.getColumn() == null)
				throw new ParseException("Sort column not specified");
			DatabaseFieldSpec field = findDatabaseField(item.getColumn());
			if (field == null) {
				throw new ParseException(String.format(
						"Sort column \"%s\" not found in table \"%s\"",
						field, table.getName()));
			}
		}
		return list.toArray(new DatabaseSort[0]);
	}
}
