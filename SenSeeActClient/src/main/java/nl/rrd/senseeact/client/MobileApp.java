package nl.rrd.senseeact.client;

public class MobileApp {
	private String code;
	private String name;
	private String androidPackage;

	/**
	 * Constructs a new mobile app instance.
	 *
	 * @param code the code that is used to identify the app in the web service
	 * @param name the friendly name of the app
	 * @param androidPackage the package name that identifies the app in the
	 * Android platform
	 */
	public MobileApp(String code, String name, String androidPackage) {
		this.code = code;
		this.name = name;
		this.androidPackage = androidPackage;
	}

	/**
	 * Returns the code that is used to identify the app in the web service.
	 *
	 * @return the code that is used to identify the app in the web service
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Returns the friendly name of the app.
	 *
	 * @return the friendly name of the app
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the package name that identifies the app in the Android platform.
	 *
	 * @return the package name that identifies the app in the Android platform
	 */
	public String getAndroidPackage() {
		return androidPackage;
	}
}
