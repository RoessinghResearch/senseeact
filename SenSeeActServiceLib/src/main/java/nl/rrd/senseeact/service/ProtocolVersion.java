package nl.rrd.senseeact.service;

public enum ProtocolVersion {
	V5("5"),
	V5_0_3("5.0.3"),
	V5_0_4("5.0.4"),
	V5_0_5("5.0.5"),
	V5_0_6("5.0.6"),
	V5_0_7("5.0.7"),
	V5_0_8("5.0.8"),
	V5_1_0("5.1.0"),
	V5_1_1("5.1.1"),
	V5_1_2("5.1.2"),
	V5_1_3("5.1.3"),
	V6_0_0("6.0.0"),
	V6_0_1("6.0.1"),
	V6_0_2("6.0.2"),
	V6_0_3("6.0.3"),
	V6_0_4("6.0.4"),
	V6_0_5("6.0.5"),
	V6_0_6("6.0.6"),
	V6_0_7("6.0.7"),
	V6_0_8("6.0.8");

	private final String versionName;
	
	ProtocolVersion(String versionName) {
		this.versionName = versionName;
	}
	
	public String versionName() {
		return versionName;
	}
	
	public static ProtocolVersion forVersionName(String versionName)
			throws IllegalArgumentException {
		for (ProtocolVersion value : ProtocolVersion.values()) {
			if (value.versionName.equals(versionName))
				return value;
		}
		throw new IllegalArgumentException("Version not found: " +
				versionName);
	}
}
