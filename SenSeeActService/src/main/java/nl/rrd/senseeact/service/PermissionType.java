package nl.rrd.senseeact.service;

import nl.rrd.senseeact.client.model.PermissionRecord;

import java.util.ArrayList;
import java.util.List;

public class PermissionType {
	private String name;

	public PermissionType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<PermissionRecord> getChildren(PermissionRecord record) {
		return new ArrayList<>();
	}
}
