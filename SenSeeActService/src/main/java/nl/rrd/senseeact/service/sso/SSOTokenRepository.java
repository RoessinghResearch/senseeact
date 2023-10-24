package nl.rrd.senseeact.service.sso;

import nl.rrd.utils.AppComponent;

import java.util.List;

@AppComponent
public abstract class SSOTokenRepository {
	public abstract List<SSOToken> getTokens();
}
