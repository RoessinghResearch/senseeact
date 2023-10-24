package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ProjectDataModule extends JsonObject {
	private String name;
	private List<String> tables = new ArrayList<>();

	public ProjectDataModule() {
	}

	public ProjectDataModule(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getTables() {
		return tables;
	}

	public void setTables(List<String> tables) {
		this.tables = tables;
	}

	public void addTable(String table) {
		tables.add(table);
	}
}
