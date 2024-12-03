package nl.rrd.senseeact.service;

import nl.rrd.senseeact.client.model.PermissionRecord;
import nl.rrd.utils.AppComponent;

import java.util.List;

@AppComponent
public class PermissionRepository {
	public List<PermissionType> getPermissionTypes() {
		return List.of(new PermissionType(
				PermissionRecord.PERMISSION_WRITE_RESOURCE_TABLE));
	}

	public List<String> getPermissionNames() {
		return getPermissionTypes().stream().map(PermissionType::getName)
				.toList();
	}
}
