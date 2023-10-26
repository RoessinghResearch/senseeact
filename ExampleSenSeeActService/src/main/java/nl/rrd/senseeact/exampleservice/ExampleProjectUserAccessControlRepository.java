package nl.rrd.senseeact.exampleservice;

import nl.rrd.senseeact.service.ProjectUserAccessControl;
import nl.rrd.senseeact.service.ProjectUserAccessControlRepository;
import nl.rrd.utils.AppComponent;

import java.util.HashMap;
import java.util.Map;

@AppComponent
public class ExampleProjectUserAccessControlRepository
		extends ProjectUserAccessControlRepository {
	@Override
	public Map<String, ProjectUserAccessControl> getProjectMap() {
		return new HashMap<>();
	}
}
