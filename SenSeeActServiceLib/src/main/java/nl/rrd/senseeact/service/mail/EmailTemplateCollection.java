package nl.rrd.senseeact.service.mail;

import nl.rrd.utils.AppComponent;

import java.util.Map;

@AppComponent
public abstract class EmailTemplateCollection {
	public EmailTemplate find(String name)
			throws IllegalArgumentException {
		Map<String, EmailTemplate> map = getTemplateMap();
		EmailTemplate template = map.get(name);
		if (template == null) {
			throw new IllegalArgumentException(
					"Unknown reset password template: " + name);
		}
		return template;
	}

	public abstract EmailTemplate getDefault();

	protected abstract Map<String,EmailTemplate> getTemplateMap();
}
