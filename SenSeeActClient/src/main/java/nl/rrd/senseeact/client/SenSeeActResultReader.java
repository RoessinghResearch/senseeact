package nl.rrd.senseeact.client;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpClientException;
import nl.rrd.utils.http.HttpResponse;

import java.io.IOException;

public interface SenSeeActResultReader<T> {

	/**
	 * Reads the request result from an HTTP response.
	 *
	 * @param httpResponse the HTTP response
	 * @return the result
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if a reading error occurs
	 */
	T read(HttpResponse httpResponse) throws HttpClientException,
			ParseException, IOException;
}
