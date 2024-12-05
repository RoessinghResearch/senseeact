package nl.rrd.senseeact.service;

import nl.rrd.senseeact.client.model.PermissionName;
import nl.rrd.utils.AppComponent;

import java.util.List;

@AppComponent
public class PermissionRepository {
	public List<PermissionType> getPermissionTypes() {
		return List.of(new PermissionType(
				PermissionName.PERMISSION_WRITE_RESOURCE_TABLE));
	}

	public List<String> getPermissionNames() {
		return getPermissionTypes().stream().map(PermissionType::getName)
				.toList();
	}

	public PermissionType findPermissionType(String name) {
		for (PermissionType type : getPermissionTypes()) {
			if (type.getName().equals(name))
				return type;
		}
		return null;
	}
}
