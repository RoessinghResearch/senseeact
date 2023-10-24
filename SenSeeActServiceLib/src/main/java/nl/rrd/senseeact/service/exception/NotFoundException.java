package nl.rrd.senseeact.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.service.controller.ErrorController;

/**
 * This exception results in a HTTP response with status 404 Not Found. The
 * exception message (default "Not Found") will be written to the response.
 * It is handled by the {@link ErrorController ErrorController}.
 * 
 * @author Dennis Hofs (RRD)
 */
@ResponseStatus(value=HttpStatus.NOT_FOUND)
public class NotFoundException extends HttpException {
	private static final long serialVersionUID = 1L;

	public NotFoundException() {
		super("Not Found");
	}

	public NotFoundException(String message) {
		super(message);
	}
	
	public NotFoundException(String code, String message) {
		super(code, message);
	}
	
	public NotFoundException(HttpError error) {
		super(error);
	}
}
