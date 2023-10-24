package nl.rrd.senseeact.client;

/**
 * This class defines an authentication header. It consists of a header name
 * and value. Usually this is used for an authentication token.
 *
 * @author Dennis Hofs (RRD)
 */
public class AuthHeader {
	private String name;
	private String value;

	public AuthHeader(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return name + ": " + value;
	}
}
