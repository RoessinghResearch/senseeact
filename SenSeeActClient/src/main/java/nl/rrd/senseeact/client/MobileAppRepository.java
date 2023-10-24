package nl.rrd.senseeact.client;

import nl.rrd.utils.AppComponent;

import java.util.List;

@AppComponent
public abstract class MobileAppRepository {
	protected abstract List<MobileApp> getMobileApps();

	public MobileApp forAndroidPackage(String pkg)
			throws IllegalArgumentException {
		List<MobileApp> apps = getMobileApps();
		for (MobileApp app : apps) {
			if (pkg.equals(app.getAndroidPackage()))
				return app;
		}
		throw new IllegalArgumentException(String.format(
				"Mobile app with Android package %s not found", pkg));
	}

	public MobileApp forCode(String code) throws IllegalArgumentException {
		List<MobileApp> apps = getMobileApps();
		for (MobileApp app : apps) {
			if (code.equalsIgnoreCase(app.getCode()))
				return app;
		}
		throw new IllegalArgumentException(String.format(
				"Mobile app with code \"%s\" not found", code));
	}
}
