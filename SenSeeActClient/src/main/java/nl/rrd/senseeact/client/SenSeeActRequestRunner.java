package nl.rrd.senseeact.client;

import nl.rrd.utils.http.HttpClient2;
import nl.rrd.utils.http.HttpClientException;
import nl.rrd.utils.http.HttpResponse;

import java.io.IOException;

public interface SenSeeActRequestRunner {

	/**
	 * Runs the request on the specified HTTP client and returns the response.
	 *
	 * @param client the HTTP client
	 * @return the HTTP response
	 * @throws HttpClientException if the service returns an error response
	 * @throws IOException if an error occurs while communicating with the
	 * service
	 */
	HttpResponse run(HttpClient2 client) throws HttpClientException,
			IOException;
}
