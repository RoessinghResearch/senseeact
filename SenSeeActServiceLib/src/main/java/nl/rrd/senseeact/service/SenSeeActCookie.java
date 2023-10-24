package nl.rrd.senseeact.service;

import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpDate;
import nl.rrd.utils.http.HttpURL;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

public class SenSeeActCookie {
	/**
	 * Sets cookie "authToken". The cookie will be valid for all URLs within the
	 * host of the base URL. The cookie will expire at the specified time. If
	 * no expiration time is set, it will expire when the session ends.
	 *
	 * @param baseUrl the base URL of the web service
	 * @param response the HTTP response where the cookie should be set
	 * @param token the authentication token
	 * @param expires the expiration time or null
	 */
	public static void setAuthTokenCookie(String baseUrl,
			HttpServletResponse response, String token, ZonedDateTime expires) {
		setCookie("authToken", token, baseUrl, response, expires);
	}

	/**
	 * Sets the specified cookie. The cookie will be valid for all URLs within
	 * the host of the base URL. The cookie will expire at the specified time.
	 * If no expiration time is set, it will expire when the session ends.
	 *
	 * @param name the cookie name
	 * @param value the cookie value
	 * @param baseUrl the base URL of the web service
	 * @param response the HTTP response where the cookie should be set
	 * @param expires the expiration time or null
	 */
	public static void setCookie(String name, String value, String baseUrl,
			HttpServletResponse response, ZonedDateTime expires) {
		try {
			HttpURL httpUrl = HttpURL.parse(baseUrl);
			String domain = httpUrl.getHost();
			value = name + "=" + URLEncoder.encode(value,
					StandardCharsets.UTF_8) +
				"; Domain=" + domain +
				"; Path=/";
			if (expires != null)
				value += "; Expires=" + HttpDate.generate(expires);
			value += "; SameSite=Strict; HttpOnly";
			response.setHeader("Set-Cookie", value);
		} catch (ParseException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

	/**
	 * Clears cookie "authToken". It sets the cookie to an empty string and
	 * the cookie will expire when the session ends.
	 *
	 * @param baseUrl the base URL of the web service
	 * @param response the HTTP response where the cookie should be cleared
	 */
	public static void clearAuthTokenCookie(String baseUrl,
			HttpServletResponse response) {
		clearCookie("authToken", baseUrl, response);
	}

	/**
	 * Clears the specified cookie. It sets the cookie to an empty string and
	 * the cookie will expire when the session ends.
	 *
	 * @param name the cookie name
	 * @param baseUrl the base URL of the web service
	 * @param response the HTTP response where the cookie should be cleared
	 */
	public static void clearCookie(String name, String baseUrl,
			HttpServletResponse response) {
		try {
			HttpURL httpUrl = HttpURL.parse(baseUrl);
			String domain = httpUrl.getHost();
			response.setHeader("Set-Cookie",
				name + "=" +
				"; Domain=" + domain +
				"; Path=/" +
				"; SameSite=Strict" +
				"; HttpOnly");
		} catch (ParseException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}
}
