package nl.rrd.senseeact.exampleclient;

import nl.rrd.utils.AppComponent;
import nl.rrd.senseeact.client.MobileApp;
import nl.rrd.senseeact.client.MobileAppRepository;

import java.util.ArrayList;
import java.util.List;

@AppComponent
public class ExampleMobileAppRepository extends MobileAppRepository {
	@Override
	protected List<MobileApp> getMobileApps() {
		List<MobileApp> apps = new ArrayList<>();
		apps.add(new MobileApp(
				"senseeact",
				"SenSeeAct",
				"nl.rrd.senseeact"));
		return apps;
	}
}
