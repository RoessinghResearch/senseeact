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
	private static final long serialVersionUID = 1L;
	
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
		for (HttpFieldError error : getError().getFieldErrors()) {
			errors.add(error);
		}
		for (HttpFieldError error : other.getError().getFieldErrors()) {
			errors.add(error);
		}
		return withInvalidInput(errors);
	}
	
	public BadRequestException appendInvalidInput(
			HttpFieldError... fieldErrors) {
		return appendInvalidInput(Arrays.asList(fieldErrors));
	}
	
	public BadRequestException appendInvalidInput(
			List<HttpFieldError> fieldErrors) {
		List<HttpFieldError> newErrors = new ArrayList<>();
		for (HttpFieldError error : getError().getFieldErrors()) {
			newErrors.add(error);
		}
		for (HttpFieldError error : fieldErrors) {
			newErrors.add(error);
		}
		return withInvalidInput(newErrors);
	}
	
	public static BadRequestException withInvalidInput(
			HttpFieldError... fieldErrors) {
		return withInvalidInput(Arrays.asList(fieldErrors));
	}

	public static BadRequestException withInvalidInput(
			List<HttpFieldError> fieldErrors) {
		StringBuffer errorMsg = new StringBuffer();
		String newline = System.getProperty("line.separator");
		for (HttpFieldError fieldError : fieldErrors) {
			if (errorMsg.length() > 0)
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
