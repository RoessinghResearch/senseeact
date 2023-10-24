package nl.rrd.senseeact.service;

import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpURL;

public class SenSeeActContext {
	public static final String LOGTAG = "SenSeeActService";

	private static String currentVersion = null;

	public static void setCurrentVersion(String currentVersion) {
		SenSeeActContext.currentVersion = currentVersion;
	}

	/**
	 * Returns the base URL. For example:<br />
	 * https://www.example.com/servlets/senseeact
	 * 
	 * @return the base URL
	 */
	public static String getBaseUrl() {
		BaseConfiguration config = AppComponents.get(BaseConfiguration.class);
		return config.get(BaseConfiguration.BASE_URL);
	}
	
	/**
	 * Returns the base path. For example:<br />
	 * /servlets/senseeact
	 * 
	 * @return the base path
	 */
	public static String getBasePath() {
		String url = getBaseUrl();
		HttpURL httpUrl;
		try {
			httpUrl = HttpURL.parse(url);
		} catch (ParseException ex) {
			throw new RuntimeException(
					"Invalid base URL: " + url + ": " + ex.getMessage(), ex);
		}
		return httpUrl.getPath();
	}
	
	/**
	 * Returns the current protocol version.
	 * 
	 * @return the current protocol version
	 */
	public static String getCurrentVersion() {
		if (currentVersion != null)
			return currentVersion;
		ProtocolVersion[] versions = ProtocolVersion.values();
		return versions[versions.length - 1].versionName();
	}
}
