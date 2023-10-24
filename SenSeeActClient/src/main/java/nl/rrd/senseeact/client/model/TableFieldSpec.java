package nl.rrd.senseeact.client.model;

import nl.rrd.utils.json.JsonObject;
import nl.rrd.senseeact.dao.DatabaseFieldSpec;
import nl.rrd.senseeact.dao.DatabaseType;

public class TableFieldSpec {
	private String name;
	private DatabaseType type;

	public TableFieldSpec() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DatabaseType getType() {
		return type;
	}

	public void setType(DatabaseType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return JsonObject.toString(this);
	}
	
	public static TableFieldSpec fromDatabaseFieldSpec(
			DatabaseFieldSpec fieldSpec) {
		TableFieldSpec result = new TableFieldSpec();
		result.name = fieldSpec.getPropSpec().getName();
		result.type = fieldSpec.getDbField().value();
		return result;
	}
}
