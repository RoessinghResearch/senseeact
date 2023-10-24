package nl.rrd.senseeact.service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.utils.AppComponents;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This controller handles all errors in the application. It looks at the
 * request attributes for a status code, error message and exception. It
 * distinguishes the following cases:
 * 
 * <p><ul>
 * <li>The request has no exception. It sets the status code and returns the
 * error message or an empty string.</li>
 * <li>The exception has a {@link ResponseStatus ResponseStatus} annotation,
 * such as a {@link HttpException HttpException}. It sets the status code and
 * returns the exception message.</li>
 * <li>The exception is a {@link ServletException ServletException}, which is
 * usually the result of Spring validation. It sets the status code and
 * returns the error message.</li>
 * <li>Any other exception. It logs the exception, sets the status code to
 * 500 Internal Server Error and returns "Internal Server Error". This means
 * that any details about the exception, which may contain sensitive
 * information, are hidden for the client.</li>
 * </ul></p>
 * 
 * @author Dennis Hofs (RRD)
 */
@RestController
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
	private static final String LOGTAG = ErrorController.class.getSimpleName();

	/**
	 * The error endpoint is known to be called in the following cases:
	 *
	 * <p><ol>
	 * <li>An endpoint throws an exception with a ResponseStatus annotation,
	 * for example a HttpException.</li>
	 * <li>An endpoint receives invalid input for a Spring annotated parameter,
	 * in particular JSON input that cannot be parsed.</li>
	 * <li>An endpoint throws an unhandled exception, for example an unexpected
	 * RuntimeException.</li>
	 * </ol></p>
	 *
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @return the error text to send to the client
	 */
	@RequestMapping("/error")
	public Object error(HttpServletRequest request,
			HttpServletResponse response) {
		Logger logger = AppComponents.getLogger(LOGTAG);

		// If an exception is thrown with a response status, then the statusCode
		// is set to that status. Otherwise (for example an unexpected
		// RuntimeException) it is set to 500.

		// The status code is set as:
		// (1) status code of the ResponseStatus annotation
		// (2) 400
		// (3) 500
		int statusCode = (Integer)request.getAttribute(
				"jakarta.servlet.error.status_code");

		// The exception object is set as:
		// (1) Exception object with ResponseStatus annotation
		// (2) HttpMessageNotReadableException with JSON exception as cause
		// (3) null
		Object obj = request.getAttribute(
				"org.springframework.web.servlet.DispatcherServlet.EXCEPTION");

		Throwable exception = null;
		if (obj instanceof Throwable)
			exception = (Throwable)obj;

		// The error message is set as:
		// (1) null
		// (2) null
		// (3) exception message
		String message = (String)request.getAttribute(
				"jakarta.servlet.error.message");

		if (message == null)
			message = "";
		if (exception == null) {
			logger.error("Error " + statusCode + ": " + message);
			response.setStatus(statusCode);
			return "Unknown error";
		}

		// At this point we have known cases (1) and (2) or an unknown case

		// The response status is set as:
		// (1) ResponseStatus annotation of the exception
		// (2) null
		ResponseStatus status = exception.getClass().getAnnotation(
				ResponseStatus.class);
		if (status != null && exception instanceof HttpException httpEx) {
			response.setStatus(status.value().value());
			return httpEx.getError();
		} else if (status != null) {
			response.setStatus(status.value().value());
			return exception.getMessage();
		} else if (exception instanceof ServletException) {
			response.setStatus(statusCode);
			return message;
		}

		// At this point we have known case (2) or an unknown case

		Throwable cause = null;
		if (exception instanceof HttpMessageNotReadableException) {
			cause = exception.getCause();
		}
		if (cause instanceof JsonProcessingException) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return cause.getMessage();
		} else {
			String stackTrace;
			StringWriter stringWriter = new StringWriter();
			try (PrintWriter writer = new PrintWriter(stringWriter)) {
				exception.printStackTrace(writer);
				stackTrace = stringWriter.getBuffer().toString();
			}
			logger.error(
					"Internal Server Error detected in error controller: " +
					exception.getMessage() + ": " + stackTrace, exception);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return "Internal Server Error";
		}
	}
}
