package nl.rrd.senseeact.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.rrd.senseeact.dao.DatabaseCriteria;

public class SelectFilterGenerator {
	public static Map<String,Object> toJsonObject(DatabaseCriteria criteria) {
		if (criteria instanceof DatabaseCriteria.Equal) {
			return toJsonObjectEqual((DatabaseCriteria.Equal)criteria);
		} else if (criteria instanceof DatabaseCriteria.NotEqual) {
			return toJsonObjectNotEqual((DatabaseCriteria.NotEqual)criteria);
		} else if (criteria instanceof DatabaseCriteria.LessThan) {
			return toJsonObjectLessThan((DatabaseCriteria.LessThan)criteria);
		} else if (criteria instanceof DatabaseCriteria.GreaterThan) {
			return toJsonObjectGreaterThan((DatabaseCriteria.GreaterThan)criteria);
		} else if (criteria instanceof DatabaseCriteria.LessEqual) {
			return toJsonObjectLessEqual((DatabaseCriteria.LessEqual)criteria);
		} else if (criteria instanceof DatabaseCriteria.GreaterEqual) {
			return toJsonObjectGreaterEqual((DatabaseCriteria.GreaterEqual)criteria);
		} else if (criteria instanceof DatabaseCriteria.And) {
			return toJsonObjectAnd((DatabaseCriteria.And)criteria);
		} else if (criteria instanceof DatabaseCriteria.Or) {
			return toJsonObjectOr((DatabaseCriteria.Or)criteria);
		} else{
			throw new RuntimeException("DatabaseCriteria class " +
					criteria.getClass().getName() + " not supported");
		}
	}
	
	private static Map<String,Object> toJsonObjectEqual(
			DatabaseCriteria.Equal criteria) {
		Map<String,Object> map = new LinkedHashMap<>();
		map.put(criteria.getColumn(), criteria.getValue());
		return map;
	}
	
	private static Map<String,Object> toJsonObjectNotEqual(
			DatabaseCriteria.NotEqual criteria) {
		Map<String,Object> opMap = new LinkedHashMap<>();
		opMap.put("$ne", criteria.getValue());
		Map<String,Object> map = new LinkedHashMap<>();
		map.put(criteria.getColumn(), opMap);
		return map;
	}
	
	private static Map<String,Object> toJsonObjectLessThan(
			DatabaseCriteria.LessThan criteria) {
		Map<String,Object> opMap = new LinkedHashMap<>();
		opMap.put("$lt", criteria.getValue());
		Map<String,Object> map = new LinkedHashMap<>();
		map.put(criteria.getColumn(), opMap);
		return map;
	}
	
	private static Map<String,Object> toJsonObjectGreaterThan(
			DatabaseCriteria.GreaterThan criteria) {
		Map<String,Object> opMap = new LinkedHashMap<>();
		opMap.put("$gt", criteria.getValue());
		Map<String,Object> map = new LinkedHashMap<>();
		map.put(criteria.getColumn(), opMap);
		return map;
	}
	
	private static Map<String,Object> toJsonObjectLessEqual(
			DatabaseCriteria.LessEqual criteria) {
		Map<String,Object> opMap = new LinkedHashMap<>();
		opMap.put("$le", criteria.getValue());
		Map<String,Object> map = new LinkedHashMap<>();
		map.put(criteria.getColumn(), opMap);
		return map;
	}
	
	private static Map<String,Object> toJsonObjectGreaterEqual(
			DatabaseCriteria.GreaterEqual criteria) {
		Map<String,Object> opMap = new LinkedHashMap<>();
		opMap.put("$ge", criteria.getValue());
		Map<String,Object> map = new LinkedHashMap<>();
		map.put(criteria.getColumn(), opMap);
		return map;
	}
	
	private static Map<String,Object> toJsonObjectAnd(
			DatabaseCriteria.And criteria) {
		List<Object> andList = new ArrayList<>();
		for (DatabaseCriteria op : criteria.getOperands()) {
			andList.add(toJsonObject(op));
		}
		Map<String,Object> map = new LinkedHashMap<>();
		map.put("$and", andList);
		return map;
	}
	
	private static Map<String,Object> toJsonObjectOr(
			DatabaseCriteria.Or criteria) {
		List<Object> orList = new ArrayList<>();
		for (DatabaseCriteria op : criteria.getOperands()) {
			orList.add(toJsonObject(op));
		}
		Map<String,Object> map = new LinkedHashMap<>();
		map.put("$or", orList);
		return map;
	}
}
