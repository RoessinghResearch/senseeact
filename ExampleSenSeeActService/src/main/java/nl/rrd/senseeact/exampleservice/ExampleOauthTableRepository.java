package nl.rrd.senseeact.exampleservice;

import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.senseeact.service.OAuthTableRepository;
import nl.rrd.utils.AppComponent;

import java.util.ArrayList;
import java.util.List;

@AppComponent
public class ExampleOauthTableRepository extends OAuthTableRepository {
	@Override
	public List<DatabaseTableDef<?>> getOAuthTables() {
		return new ArrayList<>();
	}
}
