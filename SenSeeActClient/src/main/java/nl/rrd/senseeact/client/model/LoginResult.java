package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LoginResult {
	public enum Status {
		COMPLETE,
		REQUIRES_MFA
	}

	private Status status;
	@JsonInclude(content=JsonInclude.Include.NON_NULL)
	private String user = null;
	@JsonInclude(content=JsonInclude.Include.NON_NULL)
	private String email = null;
	@JsonInclude(content=JsonInclude.Include.NON_NULL)
	private String token = null;
	@JsonInclude(content=JsonInclude.Include.NON_NULL)
	private MfaRecord mfaRecord = null;

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

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

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public MfaRecord getMfaRecord() {
		return mfaRecord;
	}

	public void setMfaRecord(MfaRecord mfaRecord) {
		this.mfaRecord = mfaRecord;
	}
}
