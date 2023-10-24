package nl.rrd.senseeact.client.model;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.senseeact.dao.DatabaseFieldScanner;
import nl.rrd.senseeact.dao.DatabaseFieldSpec;
import nl.rrd.senseeact.dao.DatabaseIndex;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.utils.json.JsonObject;

public class TableSpec {
	private String name;
	private List<TableFieldSpec> fields = new ArrayList<>();
	private List<DatabaseIndex> indexes = new ArrayList<>();
	private boolean splitByUser;
	
	public TableSpec() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<TableFieldSpec> getFields() {
		return fields;
	}

	public void setFields(List<TableFieldSpec> fields) {
		this.fields = fields;
	}

	public List<DatabaseIndex> getIndexes() {
		return indexes;
	}

	public void setIndexes(List<DatabaseIndex> indexes) {
		this.indexes = indexes;
	}

	public boolean isSplitByUser() {
		return splitByUser;
	}

	public void setSplitByUser(boolean splitByUser) {
		this.splitByUser = splitByUser;
	}
	
	@Override
	public String toString() {
		return JsonObject.toString(this);
	}
	
	public static TableSpec fromDatabaseTableDef(DatabaseTableDef<?> tableDef) {
		TableSpec result = new TableSpec();
		result.name = tableDef.getName();
		result.splitByUser = tableDef.isSplitByUser();
		List<DatabaseFieldSpec> fields = DatabaseFieldScanner.getDatabaseFields(
				tableDef.getDataClass());
		for (DatabaseFieldSpec field : fields) {
			result.fields.add(TableFieldSpec.fromDatabaseFieldSpec(field));
			if (field.getDbField().index()) {
				String fieldName = field.getPropSpec().getName();
				result.indexes.add(new DatabaseIndex(fieldName, fieldName));
			}
		}
		result.indexes.addAll(tableDef.getCompoundIndexes());
		return result;
	}
}
