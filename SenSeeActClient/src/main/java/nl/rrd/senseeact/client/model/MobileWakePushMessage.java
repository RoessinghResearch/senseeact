package nl.rrd.senseeact.client.model;

import nl.rrd.utils.json.JsonObject;

public class MobileWakePushMessage extends JsonObject {
	private String type = "mobile_wake";
	private String user;

	public MobileWakePushMessage() {
	}

	public MobileWakePushMessage(String user) {
		this.user = user;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
}
