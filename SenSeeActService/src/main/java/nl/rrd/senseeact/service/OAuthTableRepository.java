package nl.rrd.senseeact.service;

import nl.rrd.utils.AppComponent;
import nl.rrd.senseeact.dao.DatabaseTableDef;

import java.util.List;

@AppComponent
public abstract class OAuthTableRepository {
	public abstract List<DatabaseTableDef<?>> getOAuthTables();
}
