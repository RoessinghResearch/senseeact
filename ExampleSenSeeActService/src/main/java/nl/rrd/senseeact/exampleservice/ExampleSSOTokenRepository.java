package nl.rrd.senseeact.exampleservice;

import nl.rrd.senseeact.service.sso.SSOToken;
import nl.rrd.senseeact.service.sso.SSOTokenRepository;
import nl.rrd.utils.AppComponent;

import java.util.ArrayList;
import java.util.List;

@AppComponent
public class ExampleSSOTokenRepository extends SSOTokenRepository {
	@Override
	public List<SSOToken> getTokens() {
		return new ArrayList<>();
	}
}
