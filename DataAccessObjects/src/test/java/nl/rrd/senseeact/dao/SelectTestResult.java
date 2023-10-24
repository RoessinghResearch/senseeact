package nl.rrd.senseeact.dao;

import java.util.List;

public class SelectTestResult<T extends DatabaseObject> {
	private String label;
	private DatabaseCriteria criteria;
	private int limit;
	private DatabaseSort[] sort;
	private List<T> result;

	public SelectTestResult(String label, DatabaseCriteria criteria, int limit,
			DatabaseSort[] sort, List<T> result) {
		this.label = label;
		this.criteria = criteria;
		this.limit = limit;
		this.sort = sort;
		this.result = result;
	}

	public String getLabel() {
		return label;
	}

	public DatabaseCriteria getCriteria() {
		return criteria;
	}

	public int getLimit() {
		return limit;
	}

	public DatabaseSort[] getSort() {
		return sort;
	}

	public List<T> getResult() {
		return result;
	}
}
