package nl.rrd.senseeact.service.mail;

import jakarta.servlet.http.HttpServletRequest;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.utils.i18n.I18n;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EmailChangedOldVerifiedTemplateCollection extends EmailTemplateCollection {
	private Map<String,EmailTemplate> templateMap;

	public EmailChangedOldVerifiedTemplateCollection() {
		templateMap = new HashMap<>();
	}

	@Override
	public EmailTemplate getDefault() {
		return new DefaultEmailChangedOldVerifiedTemplate();
	}

	@Override
	protected Map<String,EmailTemplate> getTemplateMap() {
		return templateMap;
	}

	private static class DefaultEmailChangedOldVerifiedTemplate
			extends SenSeeActEmailTemplate {
		@Override
		public String getSubject(HttpServletRequest request, User user) {
			I18n i18n = getStrings(request, user);
			return i18n.get("email_changed_subject");
		}

		@Override
		protected String getHtmlMailContent(HttpServletRequest request,
				User user, Map<String, Object> params) throws IOException {
			String newEmail = (String)params.get("new_email");
			String html = readHtmlContent(request, user,
					"mail_templates/email_changed_old_verified_mail");
			html = html.replaceAll("\\{new_email\\}", newEmail);
			return html;
		}
	}
}
