package nl.rrd.senseeact.service.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.service.controller.ErrorController;

/**
 * Base class for exceptions that result in a HTTP error response. Subclasses
 * should be annotated with {@link ResponseStatus ResponseStatus}. They are
 * handled by {@link ErrorController ErrorController}.
 * 
 * @author Dennis Hofs (RRD)
 */
public abstract class HttpException extends Exception {
	private HttpError error;

	/**
	 * Constructs a new HTTP exception with default error code 0.
	 * 
	 * @param message the error message
	 */
	public HttpException(String message) {
		super(message);
		error = new HttpError(null, message);
	}
	
	/**
	 * Constructs a new HTTP exception.
	 * 
	 * @param code the error code (default null)
	 * @param message the error message
	 */
	public HttpException(String code, String message) {
		super(message);
		error = new HttpError(code, message);
	}
	
	/**
	 * Constructs a new HTTP exception with the specified error.
	 * 
	 * @param error the error
	 */
	public HttpException(HttpError error) {
		super(error.getMessage());
		this.error = error;
	}

	/**
	 * Returns the error details.
	 * 
	 * @return the error details
	 */
	public HttpError getError() {
		return error;
	}
	
	/**
	 * Returns the HTTP exception for the specified HTTP status code.
	 * Unsupported status codes will be mapped to an {@link
	 * InternalServerErrorException InternalServerErrorException}.
	 * 
	 * @param statusCode the HTTP status code
	 * @param error the error details
	 * @return the HTTP exception
	 */
	public static HttpException forStatus(int statusCode, HttpError error) {
		return switch (statusCode) {
		case 400 ->
			new BadRequestException(error);
		case 401 ->
			new UnauthorizedException(error);
		case 403 ->
			new ForbiddenException(error);
		case 404 ->
			new NotFoundException(error);
		case 501 ->
			new NotImplementedException(error);
		default ->
			new InternalServerErrorException(error);
		};
	}
}
