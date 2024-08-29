package nl.rrd.senseeact.service;

import nl.rrd.senseeact.service.mail.EmailConfiguration;
import nl.rrd.utils.AppComponent;

/**
 * Configuration of the SenSeeAct Service. This is initialised from resource
 * service.properties and deployment.properties. Known property keys are defined
 * as constants in this class.
 * 
 * @author Dennis Hofs (RRD)
 */
@AppComponent
public class Configuration extends BaseConfiguration {
	public static final String BASE_URL = "baseUrl";

	public static final String MYSQL_HOST = "mysqlHost";
	public static final String MYSQL_PORT = "mysqlPort";
	public static final String MYSQL_ROOT_PASSWORD = "mysqlRootPassword";

	public static final String DB_NAME_PREFIX = "dbNamePrefix";
	public static final String JWT_SECRET_KEY = "jwtSecretKey";
	public static final String SECRET_SALT = "secretSalt";
	
	public static final String ADMIN_EMAIL = "adminEmail";
	public static final String ADMIN_PASSWORD = "adminPassword";
	
	public static final String MAIL_HOST = "mailHost";
	public static final String MAIL_USERNAME = "mailUsername";
	public static final String MAIL_PASSWORD = "mailPassword";
	public static final String MAIL_SMTP_TLS = "mailSmtpTls";
	public static final String MAIL_FROM = "mailFrom";

	public static final String TWILIO_ACCOUNT_SID = "twilioAccountSid";
	public static final String TWILIO_AUTH_TOKEN = "twilioAuthToken";
	public static final String TWILIO_VERIFY_SERVICE_SID = "twilioVerifyServiceSid";
	
	public static final String WEB_URL = "webUrl";

	@Override
	public String getBaseUrl() {
		return get(BASE_URL);
	}

	public EmailConfiguration toEmailConfig() {
		return EmailConfiguration.parse(
				get(MAIL_HOST),
				get(MAIL_USERNAME),
				get(MAIL_PASSWORD),
				get(MAIL_SMTP_TLS),
				get(MAIL_FROM)
		);
	}
}
