package nl.rrd.senseeact.dao;

import java.util.Comparator;
import java.util.Map;

public class DatabaseObjectComparator<T extends DatabaseObject> implements
Comparator<T> {
	private DatabaseObjectMapper mapper;
	private DatabaseObjectMapComparator mapComparator;

	public DatabaseObjectComparator(DatabaseSort[] sort) {
		mapper = new DatabaseObjectMapper();
		mapComparator = new DatabaseObjectMapComparator(sort);
	}

	@Override
	public int compare(T o1, T o2) {
		if (o1 == null && o2 == null)
			return 0;
		if (o1 == null)
			return 1;
		if (o2 == null)
			return -1;
		Map<String,Object> map1 = mapper.objectToMap(o1, false);
		Map<String,Object> map2 = mapper.objectToMap(o2, false);
		return mapComparator.compare(map1, map2);
	}
}
