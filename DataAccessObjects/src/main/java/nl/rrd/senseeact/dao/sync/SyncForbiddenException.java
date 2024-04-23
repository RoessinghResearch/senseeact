package nl.rrd.senseeact.dao.sync;

/**
 * This exception is thrown when you try to synchronise database actions from a
 * remote database that are not allowed.
 *
 * @author Dennis Hofs (RRD)
 */
public class SyncForbiddenException extends Exception {
	public SyncForbiddenException(String message) {
		super(message);
	}

	public SyncForbiddenException(String message, Throwable cause) {
		super(message, cause);
	}
}
