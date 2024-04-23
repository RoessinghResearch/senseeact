package nl.rrd.senseeact.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.service.controller.ErrorController;

/**
 * This exception results in a HTTP response with status 500 Internal Server
 * Error. The exception message (default "Internal Server Error") will be
 * written to the response. It is handled by the {@link ErrorController
 * ErrorController}.
 * 
 * @author Dennis Hofs (RRD)
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerErrorException extends HttpException {
	public InternalServerErrorException() {
		super("Internal Server Error");
	}

	public InternalServerErrorException(String message) {
		super(message);
	}
	
	public InternalServerErrorException(String code, String message) {
		super(code, message);
	}
	
	public InternalServerErrorException(HttpError error) {
		super(error);
	}
}
