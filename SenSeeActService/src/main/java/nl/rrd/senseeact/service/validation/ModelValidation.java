package nl.rrd.senseeact.service.validation;

import java.util.List;
import java.util.Map;

import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.HttpFieldError;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.utils.validation.ObjectValidation;

/**
 * This class can validate a data model that was received as input from a HTTP
 * request. It uses {@link ObjectValidation ObjectValidation}, so you can
 * annotate fields with nl.rrd.utils.validation.Validate*.
 * 
 * @author Dennis Hofs (RRD)
 */
public class ModelValidation {
	
	/**
	 * Validates the specified model. If the validation fails it throws a
	 * {@link BadRequestException BadRequestException} with a {@link HttpError
	 * HttpError}. The error has code {@link ErrorCode#INVALID_INPUT
	 * INVALID_INPUT} and it contains details about the properties with errors.
	 * 
	 * @param model the model
	 * @throws BadRequestException if the validation failed
	 */
	public static void validate(Object model)
			throws BadRequestException {
		Map<String,List<String>> result = ObjectValidation.validate(model);
		if (result.isEmpty())
			return;
		String message = ObjectValidation.getErrorMessage(result);
		HttpError httpError = new HttpError(message);
		httpError.setCode(ErrorCode.INVALID_INPUT);
		for (String prop : result.keySet()) {
			List<String> errors = result.get(prop);
			for (String error : errors) {
				httpError.addFieldError(new HttpFieldError(prop, error));
			}
		}
		throw new BadRequestException(httpError);
	}
}
