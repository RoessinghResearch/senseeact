package nl.rrd.senseeact.client.model;

import java.util.List;

import nl.rrd.senseeact.dao.DatabaseAction;
import nl.rrd.utils.json.JsonObject;

public class SyncWatchResult extends JsonObject {
	public enum ResultCode {
		OK,
		TIMEOUT,
		NO_DATA
	}
	
	private ResultCode resultCode;
	private List<DatabaseAction> actions = null;

	public ResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(ResultCode resultCode) {
		this.resultCode = resultCode;
	}

	public List<DatabaseAction> getActions() {
		return actions;
	}

	public void setActions(List<DatabaseAction> actions) {
		this.actions = actions;
	}
}
