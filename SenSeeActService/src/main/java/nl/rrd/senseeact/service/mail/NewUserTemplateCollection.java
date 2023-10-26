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

public class NewUserTemplateCollection extends EmailTemplateCollection {
	private Map<String,EmailTemplate> templateMap;

	public NewUserTemplateCollection() {
		templateMap = new HashMap<>();
	}

	@Override
	public EmailTemplate getDefault() {
		return new DefaultNewUserTemplate();
	}

	@Override
	protected Map<String,EmailTemplate> getTemplateMap() {
		return templateMap;
	}

	private static class DefaultNewUserTemplate extends SenSeeActEmailTemplate {
		@Override
		public String getSubject(HttpServletRequest request, User user) {
			I18n i18n = getStrings(request, user);
			return i18n.get("new_user_subject");
		}

		@Override
		protected String getHtmlMailContent(HttpServletRequest request,
				User user, Map<String, Object> params) throws IOException {
			String code = (String)params.get("code");
			Configuration config = AppComponents.get(Configuration.class);
			String url = config.get(Configuration.WEB_URL) +
					"/verify-email?user=" +
					URLEncoder.encode(user.getUserid(), StandardCharsets.UTF_8) +
					"&code=" + code;
			String html = readHtmlContent(request, user,
					"mail_templates/new_user_mail");
			html = html.replaceAll("\\{link\\}", url);
			return html;
		}
	}
}
