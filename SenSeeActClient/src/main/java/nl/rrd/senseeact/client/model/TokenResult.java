package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown=true)
public class TokenResult extends JsonObject {
	private String user;
	private String token;

	public TokenResult() {
	}

	public TokenResult(String user, String token) {
		this.user = user;
		this.token = token;
	}

	public String getUser() {
		return user;
	}

	public void setUserid(String user) {
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
