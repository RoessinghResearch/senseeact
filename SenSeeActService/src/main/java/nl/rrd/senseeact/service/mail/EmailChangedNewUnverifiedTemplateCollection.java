package nl.rrd.senseeact.service.mail;

import jakarta.servlet.http.HttpServletRequest;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.senseeact.service.Configuration;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.i18n.I18n;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class EmailChangedNewUnverifiedTemplateCollection extends EmailTemplateCollection {
	private Map<String,EmailTemplate> templateMap;

	public EmailChangedNewUnverifiedTemplateCollection() {
		templateMap = new HashMap<>();
	}

	@Override
	public EmailTemplate getDefault() {
		return new DefaultEmailChangedNewTemplate();
	}

	@Override
	protected Map<String,EmailTemplate> getTemplateMap() {
		return templateMap;
	}

	private static class DefaultEmailChangedNewTemplate
			extends SenSeeActEmailTemplate {
		@Override
		public String getSubject(HttpServletRequest request, User user) {
			I18n i18n = getStrings(request, user);
			return i18n.get("email_changed_subject");
		}

		@Override
		protected String getHtmlMailContent(HttpServletRequest request,
				User user, Map<String, Object> params) throws IOException {
			String oldEmail = (String)params.get("old_email");
			String newEmail = (String)params.get("new_email");
			String code = (String)params.get("code");
			Configuration config = AppComponents.get(Configuration.class);
			String url = config.get(Configuration.WEB_URL) +
					"/verify-email?user=" +
					URLEncoder.encode(user.getUserid(), StandardCharsets.UTF_8) +
					"&code=" + code;
			String html = readHtmlContent(request, user,
					"mail_templates/email_changed_new_unverified_mail");
			html = html.replaceAll("\\{old_email\\}", oldEmail)
					.replaceAll("\\{new_email\\}", newEmail)
					.replaceAll("\\{link\\}", url);
			return html;
		}
	}
}
