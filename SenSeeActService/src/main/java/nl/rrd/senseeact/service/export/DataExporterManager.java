package nl.rrd.senseeact.service.export;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataExporterManager {
	private List<DataExporter> exporters = new ArrayList<>();

	@PostConstruct
	public void init() {
	}

	@PreDestroy
	public void destroy() {
	}
}
