package nl.rrd.senseeact.exampleservice;

import nl.rrd.senseeact.client.MobileAppRepository;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.DatabaseFactory;
import nl.rrd.senseeact.exampleclient.ExampleMobileAppRepository;
import nl.rrd.senseeact.exampleclient.project.ExampleProjectRepository;
import nl.rrd.senseeact.service.ApplicationInit;
import nl.rrd.senseeact.service.Configuration;
import nl.rrd.senseeact.service.OAuthTableRepository;
import nl.rrd.senseeact.service.ProjectUserAccessControlRepository;
import nl.rrd.senseeact.service.export.DataExporterFactory;
import nl.rrd.senseeact.service.mail.EmailTemplateRepository;
import nl.rrd.senseeact.service.sso.SSOTokenRepository;
import nl.rrd.utils.exception.ParseException;

public class ExampleApplicationInit extends ApplicationInit {
	public ExampleApplicationInit() throws Exception {
	}

	@Override
	protected Configuration createConfiguration() {
		return ExampleConfiguration.getInstance();
	}

	@Override
	protected DatabaseFactory createDatabaseFactory() throws ParseException {
		return createMySQLDatabaseFactory();
	}

	@Override
	protected OAuthTableRepository createOAuthTableRepository() {
		return new ExampleOauthTableRepository();
	}

	@Override
	protected SSOTokenRepository createSSOTokenRepository() {
		return new ExampleSSOTokenRepository();
	}

	@Override
	protected EmailTemplateRepository
	createResetPasswordTemplateRepository() {
		return new ExampleEmailTemplateRepository();
	}

	@Override
	protected ProjectRepository createProjectRepository() {
		return new ExampleProjectRepository();
	}

	@Override
	protected ProjectUserAccessControlRepository
	createProjectUserAccessControlRepository() {
		return new ExampleProjectUserAccessControlRepository();
	}

	@Override
	protected MobileAppRepository createMobileAppRepository() {
		return new ExampleMobileAppRepository();
	}

	@Override
	protected DataExporterFactory createDataExporterFactory() {
		return new ExampleDataExporterFactory();
	}
}
