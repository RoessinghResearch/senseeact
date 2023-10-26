package nl.rrd.senseeact.exampleservice;

import nl.rrd.utils.AppComponents;
import nl.rrd.senseeact.dao.DatabaseFactory;
import nl.rrd.senseeact.service.ApplicationInit;
import nl.rrd.senseeact.service.Configuration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The main entry point for the SenSeeAct Service as a Spring Boot Application.
 * 
 * @author Dennis Hofs (RRD)
 */
@SpringBootApplication(
		scanBasePackages = {
				"nl.rrd.senseeact.service",
				"nl.rrd.senseeact.exampleservice"
		},
		exclude={ MongoAutoConfiguration.class }
)
@EnableScheduling
public class Application extends SpringBootServletInitializer implements
ApplicationListener<ContextClosedEvent> {
	private ApplicationInit appInit;

	/**
	 * Constructs a new application. It reads service.properties and
	 * initialises the {@link Configuration Configuration} and the {@link
	 * AppComponents AppComponents} with the {@link DatabaseFactory
	 * DatabaseFactory}.
	 * 
	 * @throws Exception if the application can't be initialised
	 */
	public Application() throws Exception {
		appInit = new ExampleApplicationInit();
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		appInit.onApplicationEvent(event);
	}
	
	@Override
	protected SpringApplicationBuilder configure(
			SpringApplicationBuilder builder) {
		return builder.sources(Application.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
