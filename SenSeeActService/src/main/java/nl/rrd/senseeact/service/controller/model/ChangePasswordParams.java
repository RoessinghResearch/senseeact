package nl.rrd.senseeact.service.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.JsonObject;
import nl.rrd.senseeact.client.model.LoginParams;
import nl.rrd.senseeact.client.model.TokenExpirationDeserializer;
import nl.rrd.senseeact.client.model.TokenExpirationSerializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangePasswordParams extends JsonObject {
	private String user = null;
	private String email = null;
	private String oldPassword = null;
	private String newPassword = null;
	private Integer tokenExpiration = LoginParams.DEFAULT_EXPIRATION;
	private boolean cookie = false;
	private boolean autoExtendCookie = false;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	@JsonSerialize(using=TokenExpirationSerializer.class)
	public Integer getTokenExpiration() {
		return tokenExpiration;
	}

	@JsonDeserialize(using=TokenExpirationDeserializer.class)
	public void setTokenExpiration(Integer tokenExpiration) {
		this.tokenExpiration = tokenExpiration;
	}

	public boolean isCookie() {
		return cookie;
	}

	public void setCookie(boolean cookie) {
		this.cookie = cookie;
	}

	public boolean isAutoExtendCookie() {
		return autoExtendCookie;
	}

	public void setAutoExtendCookie(boolean autoExtendCookie) {
		this.autoExtendCookie = autoExtendCookie;
	}
}
