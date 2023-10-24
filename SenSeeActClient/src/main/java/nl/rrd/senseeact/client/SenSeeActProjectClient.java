package nl.rrd.senseeact.client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpClient;
import nl.rrd.utils.http.HttpClientException;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.SenSeeActClientException;

import java.io.IOException;

/**
 * This class can be used to run queries on SenSeeAct project services. It uses
 * the authentication and connection handling of an {@link SenSeeActClient
 * SenSeeActClient}, which you should specify at construction.
 *
 * @author Dennis Hofs (RRD)
 */
public class SenSeeActProjectClient {
	private SenSeeActClient ssaClient;
	private String baseUrl;
	private String project;

	/**
	 * Constructs a new SenSeeAct project client. The specified base URL should
	 * be for project services in general, without a trailing slash. For
	 * example:<br />
	 * https://www.example.com/servlets/senseeactproject
	 *
	 * @param ssaClient the SenSeeAct client
	 * @param baseUrl the base URL for SenSeeAct project services
	 * @param project the project code
	 */
	public SenSeeActProjectClient(SenSeeActClient ssaClient, String baseUrl,
			String project) {
		this.ssaClient = ssaClient;
		this.baseUrl = baseUrl;
		this.project = project;
	}

	/**
	 * Runs an SenSeeAct query. If the query requires authentication, you should
	 * set "authenticate" to true. Then this method will add the authentication
	 * token. For other queries you should set "authenticate" to false, because
	 * it may not be possible to add an authentication token (for example for
	 * a login or signup).
	 *
	 * @param action the action. This is appended to the base URL and should
	 * start with a slash.
	 * @param method the HTTP method (e.g. GET or POST)
	 * @param authenticate true if the query requires authentication, false
	 * otherwise
	 * @param runner the query runner
	 * @param <T> the result type
	 * @return the query result
	 * @throws SenSeeActClientException if the SenSeeAct service returns an
	 * error response
	 * @throws HttpClientException if the server returns an error response (for
	 * example if the server is available, but the SenSeeAct service is not)
	 * @throws ParseException if an error occurs while parsing the response
	 * @throws IOException if an error occurs while communicating with the
	 * server
	 */
	public <T> T runQuery(String action, String method, boolean authenticate,
			SenSeeActQueryRunner<T> runner) throws SenSeeActClientException,
			HttpClientException, ParseException, IOException {
		String url = baseUrl + "/" + project + "/v" +
				SenSeeActClient.PROTOCOL_VERSION + action;
		HttpClient client = ssaClient.getHttpClientForUrl(url, method,
				authenticate);
		try {
			return runner.runQuery(client);
		} catch (HttpClientException httpEx) {
			ObjectMapper mapper = new ObjectMapper();
			HttpError error;
			try {
				error = mapper.readValue(httpEx.getErrorContent(),
						HttpError.class);
			} catch (JsonParseException | JsonMappingException parseEx) {
				error = null;
			}
			if (error == null)
				throw httpEx;
			throw new SenSeeActClientException(httpEx.getStatusCode(),
					httpEx.getStatusMessage(), error);
		} finally {
			ssaClient.closeHttpClient(client);
		}
	}
}
