package nl.rrd.senseeact.client;

import java.io.IOException;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpClient;
import nl.rrd.utils.http.HttpClientException;

/**
 * This interface defines how a SenSeeAct query is run on an HTTP client.
 *
 * @param <T> the result type of the query
 */
public interface SenSeeActQueryRunner<T> {

	/**
	 * Runs the query on the specified HTTP client.
	 *
	 * @param client the HTTP client
	 * @return the query result
	 * @throws HttpClientException if the service returns an error response
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * service
	 */
	T runQuery(HttpClient client) throws HttpClientException, ParseException,
			IOException;
}
