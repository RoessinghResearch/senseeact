package nl.rrd.senseeact.dao.sync;

/**
 * This exception is thrown when the {@link DatabaseActionMerger
 * DatabaseActionMerger} fails to merge a list of database actions.
 * 
 * @author Dennis Hofs (RRD)
 */
public class MergeException extends Exception {
	private static final long serialVersionUID = 1L;

	public MergeException(String message) {
		super(message);
	}

	public MergeException(String message, Throwable cause) {
		super(message, cause);
	}
}
