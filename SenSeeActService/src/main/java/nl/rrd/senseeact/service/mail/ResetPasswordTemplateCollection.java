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

public class ResetPasswordTemplateCollection extends EmailTemplateCollection {
	private Map<String,EmailTemplate> templateMap;

	public ResetPasswordTemplateCollection() {
		templateMap = new HashMap<>();
	}

	@Override
	public EmailTemplate getDefault() {
		return new DefaultResetPasswordTemplate();
	}

	@Override
	protected Map<String,EmailTemplate> getTemplateMap() {
		return templateMap;
	}

	private static class DefaultResetPasswordTemplate extends SenSeeActEmailTemplate {
		@Override
		public String getSubject(HttpServletRequest request, User user) {
			I18n i18n = getStrings(request, user);
			return i18n.get("reset_password_subject");
		}

		@Override
		protected String getHtmlMailContent(HttpServletRequest request,
				User user, Map<String, Object> params) throws IOException {
			String code = (String)params.get("code");
			Configuration config = AppComponents.get(Configuration.class);
			String html = readHtmlContent(request, user,
					"mail_templates/reset_password_mail");
			String url = config.get(Configuration.WEB_URL) +
					"/reset-password?email=" +
					URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8) +
					"&code=" + code;
			html = html.replaceAll("\\{link\\}", url);
			return html;
		}
	}
}
