package nl.rrd.senseeact.dao.sync;

/**
 * This exception is thrown when illegal input is provided.
 *
 * @author Dennis Hofs (RRD)
 */
public class IllegalInputException extends Exception {
	private static final long serialVersionUID = 4677697805244195518L;

	public IllegalInputException(String message) {
		super(message);
	}

	public IllegalInputException(String message, Throwable cause) {
		super(message, cause);
	}
}
