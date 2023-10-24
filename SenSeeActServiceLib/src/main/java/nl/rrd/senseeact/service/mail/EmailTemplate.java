package nl.rrd.senseeact.service.mail;

import jakarta.servlet.http.HttpServletRequest;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.utils.http.AcceptLanguageParser;
import nl.rrd.utils.i18n.I18n;
import nl.rrd.utils.i18n.I18nLoader;
import nl.rrd.utils.i18n.I18nResourceFinder;
import nl.rrd.utils.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class defines an email template. It returns the "from" address, subject,
 * HTML content and possible inline files for the email. The texts can be
 * translated according to the user preference. The methods receive the HTTP
 * request and user profile for this. The HTTP request may define an
 * Accept-Language header. The user profile may contain a preferred locale. This
 * class contains some helper methods to get the preferred language and
 * translated content.
 *
 * @author Dennis Hofs (RRD)
 */
public abstract class EmailTemplate {
	private EmailConfiguration config;

	public EmailTemplate(EmailConfiguration config) {
		this.config = config;
	}

	/**
	 * Returns the "from" address for the email. For example
	 * "John Doe &lt;john@example.com&gt;". The HTTP request and user can be
	 * used to determine the language. This can be the Accept-Language header
	 * in the request, or the preferred locale in the user profile.
	 *
	 * <p>The default implementation reads the mailFrom property from the
	 * configuration.</p>
	 *
	 * @param request the HTTP request
	 * @param user the user
	 * @return the "from" address
	 */
	public String getFrom(HttpServletRequest request, User user) {
		return config.getFrom();
	}

	/**
	 * Returns the subject of the email. The HTTP request and user can be used
	 * to determine the language. This can be the Accept-Language header in the
	 * request, or the preferred locale in the user profile.
	 *
	 * @param request the HTTP request
	 * @param user the user
	 * @return the subject
	 */
	public abstract String getSubject(HttpServletRequest request, User user);

	/**
	 * Returns the HTML content for the email. The HTTP request and user can be
	 * used to determine the language. This can be the Accept-Language header in
	 * the request, or the preferred locale in the user profile.
	 *
	 * <p>The parameters depend on the type of email. They define the dynamic
	 * parts of the email content.</p>
	 *
	 * @param request the HTTP request
	 * @param user the user
	 * @param params the parameters
	 * @return the HTML content
	 */
	public abstract String getHtmlContent(HttpServletRequest request, User user,
			Map<String,Object> params) throws IOException;

	/**
	 * Returns the inline files that should be added to the email, such as
	 * pictures. Inside the HTML you can refer to them using URL
	 * "cid:[contentId]". The URLs could be obtained from resources or local
	 * files. The HTTP request and user can be used to determine the language.
	 * This can be the Accept-Language header in the request, or the preferred
	 * locale in the user profile.
	 *
	 * <p>The default implementation returns an empty list.</p>
	 *
	 * @param request the HTTP request
	 * @param user the user
	 * @return the inline files
	 */
	public List<InlineFile> getInlineFiles(HttpServletRequest request,
			User user) {
		return new ArrayList<>();
	}

	protected List<Locale> getPreferredLocales(HttpServletRequest request,
			User user) {
		if (user.getLocaleCode() != null)
			return List.of(user.toLocale());
		String acceptLanguage = request.getHeader("Accept-Language");
		List<Locale> locales = new ArrayList<>();
		if (acceptLanguage != null)
			locales.addAll(AcceptLanguageParser.parse(acceptLanguage));
		locales.add(Locale.getDefault());
		return locales;
	}

	protected I18n getStrings(HttpServletRequest request, User user) {
		List<Locale> locales = getPreferredLocales(request, user);
		return I18nLoader.getInstance().getI18n("strings", locales, true,
				null);
	}

	protected String readHtmlContent(HttpServletRequest request, User user,
			String resource) throws IOException {
		List<Locale> locales = getPreferredLocales(request, user);
		I18nResourceFinder finder = new I18nResourceFinder(resource);
		finder.setExtension("html");
		finder.setUserLocales(locales);
		if (!finder.find()) {
			throw new RuntimeException("Can't find resource \"" + resource +
					"\"");
		}
		try (InputStream input = finder.openStream()) {
			return FileUtils.readFileString(input);
		}
	}

	public static class InlineFile {
		private String contentId;
		private URL url;

		public InlineFile(String contentId, URL url) {
			this.contentId = contentId;
			this.url = url;
		}

		public String getContentId() {
			return contentId;
		}

		public URL getUrl() {
			return url;
		}
	}
}
