package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown=true)
public class SignupTemporaryUserParams extends JsonObject {
	private String project = null;
	private Integer tokenExpiration = LoginParams.DEFAULT_EXPIRATION;
	private boolean cookie = false;
	private boolean autoExtendCookie = false;

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
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
