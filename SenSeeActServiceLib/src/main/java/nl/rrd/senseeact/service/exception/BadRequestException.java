package nl.rrd.senseeact.service.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.HttpFieldError;
import nl.rrd.senseeact.service.controller.ErrorController;

/**
 * This exception results in a HTTP response with status 400 Bad Request. The
 * exception message (default "Bad Request") will be written to the response.
 * It is handled by the {@link ErrorController ErrorController}.
 * 
 * @author Dennis Hofs (RRD)
 */
@ResponseStatus(value=HttpStatus.BAD_REQUEST)
public class BadRequestException extends HttpException {

	public BadRequestException() {
		super("Bad Request");
	}

	public BadRequestException(String message) {
		super(message);
	}
	
	public BadRequestException(String code, String message) {
		super(code, message);
	}
	
	public BadRequestException(HttpError error) {
		super(error);
	}
	
	public BadRequestException appendInvalidInput(BadRequestException other) {
		List<HttpFieldError> errors = new ArrayList<>();
		errors.addAll(getError().getFieldErrors());
		errors.addAll(other.getError().getFieldErrors());
		return withInvalidInput(errors);
	}
	
	public BadRequestException appendInvalidInput(
			HttpFieldError... fieldErrors) {
		return appendInvalidInput(Arrays.asList(fieldErrors));
	}
	
	public BadRequestException appendInvalidInput(
			List<HttpFieldError> fieldErrors) {
		List<HttpFieldError> newErrors = new ArrayList<>();
		newErrors.addAll(getError().getFieldErrors());
		newErrors.addAll(fieldErrors);
		return withInvalidInput(newErrors);
	}
	
	public static BadRequestException withInvalidInput(
			HttpFieldError... fieldErrors) {
		return withInvalidInput(Arrays.asList(fieldErrors));
	}

	public static BadRequestException withInvalidInput(
			List<HttpFieldError> fieldErrors) {
		StringBuilder errorMsg = new StringBuilder();
		String newline = System.lineSeparator();
		for (HttpFieldError fieldError : fieldErrors) {
			if (!errorMsg.isEmpty())
				errorMsg.append(newline);
			errorMsg.append(fieldError.getMessage());
		}
		HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
				errorMsg.toString());
		for (HttpFieldError fieldError : fieldErrors) {
			error.addFieldError(fieldError);
		}
		return new BadRequestException(error);
	}
}
