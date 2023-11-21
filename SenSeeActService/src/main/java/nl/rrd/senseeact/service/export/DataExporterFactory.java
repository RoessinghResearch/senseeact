package nl.rrd.senseeact.service.export;

import nl.rrd.senseeact.client.SenSeeActClient;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.utils.AppComponent;

import java.io.File;
import java.util.List;

@AppComponent
public interface DataExporterFactory {
	List<String> getProjectCodes();
	DataExporter create(String project, String id, User user,
			SenSeeActClient client, File dir, DataExportListener listener);
}
