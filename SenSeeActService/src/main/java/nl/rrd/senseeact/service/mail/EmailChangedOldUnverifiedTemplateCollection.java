package nl.rrd.senseeact.service.mail;

import jakarta.servlet.http.HttpServletRequest;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.utils.i18n.I18n;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EmailChangedOldUnverifiedTemplateCollection
		extends EmailTemplateCollection {
	private Map<String,EmailTemplate> templateMap;

	public EmailChangedOldUnverifiedTemplateCollection() {
		templateMap = new HashMap<>();
	}

	@Override
	public EmailTemplate getDefault() {
		return new DefaultEmailChangedOldUnverifiedTemplate();
	}

	@Override
	protected Map<String,EmailTemplate> getTemplateMap() {
		return templateMap;
	}

	private static class DefaultEmailChangedOldUnverifiedTemplate
			extends SenSeeActEmailTemplate {
		@Override
		public String getSubject(HttpServletRequest request, User user) {
			I18n i18n = getStrings(request, user);
			return i18n.get("email_changed_subject");
		}

		@Override
		protected String getHtmlMailContent(HttpServletRequest request,
				User user, Map<String, Object> params) throws IOException {
			return readHtmlContent(request, user,
					"mail_templates/email_changed_old_unverified_mail");
		}
	}
}
