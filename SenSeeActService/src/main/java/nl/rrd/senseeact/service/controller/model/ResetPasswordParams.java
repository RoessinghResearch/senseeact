package nl.rrd.senseeact.service.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResetPasswordParams extends JsonObject {
	private String email;
	private String code;
	private String password;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
