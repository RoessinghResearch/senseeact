package nl.rrd.senseeact.service.mail;

public class EmailConfiguration {
	private String host;
	private String username;
	private String password;
	private boolean smtpTls;
	private String from;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isSmtpTls() {
		return smtpTls;
	}

	public void setSmtpTls(boolean smtpTls) {
		this.smtpTls = smtpTls;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public static EmailConfiguration parse(
			String host,
			String username,
			String password,
			String smtpTls,
			String from) {
		EmailConfiguration config = new EmailConfiguration();
		config.host = host == null ? "" : host.trim();
		if (config.host.isEmpty())
			throw new RuntimeException("Mail host not configured");
		config.username = username == null ? "" : username.trim();
		config.password = password == null ? "" : password.trim();
		smtpTls = smtpTls == null ? "" : smtpTls.trim();
		config.smtpTls = smtpTls.equalsIgnoreCase("true") ||
				smtpTls.equals("1");
		config.from = from;
		return config;
	}
}
