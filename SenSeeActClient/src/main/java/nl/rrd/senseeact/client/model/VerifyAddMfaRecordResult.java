package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown=true)
public class VerifyAddMfaRecordResult extends JsonObject {
	private MfaRecord mfaRecord;
	private String token;

	public VerifyAddMfaRecordResult() {
	}

	public VerifyAddMfaRecordResult(MfaRecord mfaRecord, String token) {
		this.mfaRecord = mfaRecord;
		this.token = token;
	}

	public MfaRecord getMfaRecord() {
		return mfaRecord;
	}

	public void setMfaRecord(MfaRecord mfaRecord) {
		this.mfaRecord = mfaRecord;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
