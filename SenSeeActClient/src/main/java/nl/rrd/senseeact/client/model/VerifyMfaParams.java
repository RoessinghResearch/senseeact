package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown=true)
public class VerifyMfaParams extends JsonObject {
	private String mfaId = null;
	private String code = null;

	public VerifyMfaParams() {
	}

	public VerifyMfaParams(String mfaId, String code) {
		this.mfaId = mfaId;
		this.code = code;
	}

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
