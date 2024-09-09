package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown=true)
public class VerifyMfaParams extends JsonObject {
	public static final int DEFAULT_EXPIRATION = 1440; // minutes

	private String mfaId = null;
	private String code = null;
	private Integer tokenExpiration = DEFAULT_EXPIRATION;
	private boolean cookie = false;
	private boolean autoExtendCookie = false;

	public String getMfaId() {
		return mfaId;
	}

	public void setMfaId(String mfaId) {
		this.mfaId = mfaId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
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
