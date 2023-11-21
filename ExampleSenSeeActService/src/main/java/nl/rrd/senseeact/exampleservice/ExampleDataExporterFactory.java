package nl.rrd.senseeact.exampleservice;

import nl.rrd.senseeact.client.SenSeeActClient;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.senseeact.service.export.DataExportListener;
import nl.rrd.senseeact.service.export.DataExporter;
import nl.rrd.senseeact.service.export.DataExporterFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExampleDataExporterFactory implements DataExporterFactory {
	@Override
	public List<String> getProjectCodes() {
		return new ArrayList<>();
	}

	@Override
	public DataExporter create(String project, String id, User user,
			SenSeeActClient client, File dir, DataExportListener listener) {
		return null;
	}
}
