package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown=true)
public class VerifyMfaParams extends JsonObject {
	public static final int DEFAULT_EXPIRATION = 1440; // minutes

	private String mfaId = null;
	private String code = null;

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
}
