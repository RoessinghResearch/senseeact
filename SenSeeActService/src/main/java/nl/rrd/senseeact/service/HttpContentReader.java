package nl.rrd.senseeact.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * This class can read the content from a HTTP servlet request.
 * 
 * @author Dennis Hofs (RRD)
 */
public class HttpContentReader {

	/**
	 * Reads the content as a UTF-8 string.
	 * 
	 * @param request the request
	 * @return the content
	 * @throws IOException if a reading error occurs
	 */
	public static String readString(HttpServletRequest request)
			throws IOException {
		try (InputStream input = request.getInputStream()) {
			return FileUtils.readFileString(input);
		}
	}
	
	/**
	 * Reads the content as a JSON object string that's encoded with UTF-8. The
	 * string will then be converted to a map with string keys. If the content
	 * is invalid or empty, it will throw a {@link ParseException
	 * ParseException}.
	 * 
	 * @param request the request
	 * @return the content
	 * @throws ParseException if the content is invalid or empty
	 * @throws IOException if a reading error occurs
	 */
	public static Map<String,?> readJsonParams(HttpServletRequest request)
			throws ParseException, IOException {
		return readJsonParams(request, false);
	}

	/**
	 * Reads the content as a JSON object string that's encoded with UTF-8. The
	 * string will then be converted to a map with string keys. If "optional"
	 * is true, the string may be empty and this method returns null. If the
	 * content is invalid, or if it's empty but "optional" is false, then this
	 * method throws a {@link ParseException ParseException}.
	 * 
	 * @param request the request
	 * @param optional true if the content can be empty, false otherwise
	 * @return the content or null
	 * @throws ParseException if the content is invalid, or if it's empty but
	 * "optional" is false
	 * @throws IOException if a reading error occurs
	 */
	public static Map<String,?> readJsonParams(HttpServletRequest request,
			boolean optional) throws ParseException, IOException {
		String content = readString(request);
		if (content.length() == 0) {
			if (optional)
				return null;
			else
				throw new ParseException("No content");
		}
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(content, new TypeReference<>() {});
		} catch (Exception ex) {
			throw new ParseException("Invalid JSON object: " + ex.getMessage(),
					ex);
		}
	}
}
