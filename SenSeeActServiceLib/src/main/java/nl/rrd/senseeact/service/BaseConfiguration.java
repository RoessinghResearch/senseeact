package nl.rrd.senseeact.service;

import nl.rrd.utils.AppComponent;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Base configuration class for the SenSeeAct Service and project services. It
 * defines property keys that are shared by all services.
 * 
 * @author Dennis Hofs (RRD)
 */
@AppComponent
public class BaseConfiguration {
	public static final String VERSION = "version";
	public static final String BASE_URL = "baseUrl";
	public static final String DATA_DIR = "dataDir";

	private Map<String,String> properties = new HashMap<>();

	/**
	 * Loads the resource service.properties or deployment.properties into this
	 * configuration. This should only be called once at startup of the service.
	 *
	 * @param url the URL of service.properties or deployment.properties
	 * @throws IOException if a reading error occurs
	 */
	public void loadProperties(URL url) throws IOException {
		Properties props = new Properties();
		try (Reader reader = new InputStreamReader(url.openStream(),
				StandardCharsets.UTF_8)) {
			props.load(reader);
		}
		for (String name : props.stringPropertyNames()) {
			properties.put(name, props.getProperty(name));
		}
	}

	public String get(String key) {
		return get(key, null);
	}

	public String get(String key, String defaultValue) {
		if (!key.equals(DATA_DIR)) {
			String value = tryReadFromFile(key);
			if (value != null)
				return value;
		}
		String envKey = "ssaconfig" + key.substring(0, 1).toUpperCase() +
				key.substring(1);
		String envValue = System.getenv(envKey);
		if (envValue != null && !envValue.isEmpty())
			return envValue;
		String propValue = readProperty(key);
		if (propValue != null && !propValue.isEmpty())
			return propValue;
		if (envValue != null)
			return envValue;
		if (propValue != null)
			return propValue;
		return defaultValue;
	}

	private String tryReadFromFile(String key) {
		File dataDir = new File(get(DATA_DIR));
		File configFile = new File(dataDir, "ssaconfig.properties");
		if (!configFile.exists())
			return null;
		Properties props = new Properties();
		try (Reader reader = new InputStreamReader(
				new FileInputStream(configFile), StandardCharsets.UTF_8)) {
			props.load(reader);
		} catch (IOException ex) {
			return null;
		}
		return props.getProperty("ssaconfig" +
				key.substring(0, 1).toUpperCase() + key.substring(1));
	}

	private String readProperty(String key) {
		return properties.get(key);
	}
}
