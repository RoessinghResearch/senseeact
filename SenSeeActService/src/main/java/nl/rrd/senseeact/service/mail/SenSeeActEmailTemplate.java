package nl.rrd.senseeact.service.mail;

import jakarta.servlet.http.HttpServletRequest;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.senseeact.service.Configuration;
import nl.rrd.utils.AppComponents;

import java.io.IOException;
import java.util.Map;

public abstract class SenSeeActEmailTemplate extends EmailTemplate {
	public SenSeeActEmailTemplate() {
		super(AppComponents.get(Configuration.class).toEmailConfig());
	}

	@Override
	public String getHtmlContent(HttpServletRequest request, User user,
			Map<String, Object> params) throws IOException {
		Configuration config = AppComponents.get(Configuration.class);
		String webBase = config.get(Configuration.WEB_URL);
		String html = readHtmlContent(request, user,
				"mail_templates/senseeact_mail_template");
		String contentHtml = getHtmlMailContent(request, user, params);
		html = html.replaceAll("\\{content\\}", contentHtml);
		html = html.replaceAll("\\{web-base\\}", webBase);
		return html;
	}

	protected abstract String getHtmlMailContent(HttpServletRequest request,
			User user, Map<String, Object> params) throws IOException;
}
