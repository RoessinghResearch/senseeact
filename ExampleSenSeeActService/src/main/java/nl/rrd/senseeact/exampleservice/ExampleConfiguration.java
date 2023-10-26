package nl.rrd.senseeact.exampleservice;

import nl.rrd.utils.AppComponent;
import nl.rrd.senseeact.service.Configuration;

import java.net.URL;

/**
 * Configuration of the SenSeeAct Service. This is initialised from resource
 * service.properties. Known property keys are defined as constants in this
 * class.
 * 
 * @author Dennis Hofs (RRD)
 */
@AppComponent
public class ExampleConfiguration extends Configuration {
	private static final Object LOCK = new Object();
	private static ExampleConfiguration instance = null;

	/**
	 * Returns the configuration. At startup of the service it should be
	 * initialised with {@link #loadProperties(URL) loadProperties()}.
	 * 
	 * @return the configuration
	 */
	public static ExampleConfiguration getInstance() {
		synchronized (LOCK) {
			if (instance == null)
				instance = new ExampleConfiguration();
			return instance;
		}
	}

	/**
	 * This private constructor is used in {@link #getInstance()
	 * getInstance()}.
	 */
	private ExampleConfiguration() {
	}
}
