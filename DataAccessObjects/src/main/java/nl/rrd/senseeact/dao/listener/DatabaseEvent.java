package nl.rrd.senseeact.dao.listener;

import java.util.List;
import java.util.Map;

import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.utils.json.JsonObject;

/**
 * Base class for a database event. It has the following subclasses:
 *
 * <p><ul>
 * <li>{@link Insert Insert} (type {@link Type#INSERT INSERT})</li>
 * <li>{@link Update Update} (type {@link Type#UPDATE UPDATE})</li>
 * <li>{@link Delete Delete} (type {@link Type#DELETE DELETE})</li>
 * </ul></p>
 *
 * @author Dennis Hofs (RRD)
 */
public abstract class DatabaseEvent {

	/**
	 * Possible types of database event. Depending on the type you can cast
	 * a DatabaseEvent to one of the subclasses that are listed at the top of
	 * this page.
	 */
	public enum Type {
		INSERT,
		UPDATE,
		DELETE
	}

	private Type type;
	private String database;
	private String table;

	/**
	 * Constructs a new database event.
	 *
	 * @param type the event type
	 * @param database the database name
	 * @param table the table name
	 */
	protected DatabaseEvent(Type type, String database, String table) {
		this.type = type;
		this.database = database;
		this.table = table;
	}

	/**
	 * Returns the event type. Depending on the type you can cast a
	 * DatabaseEvent to one of the subclasses that are listed at the top of this
	 * page.
	 *
	 * @return the event type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the database name.
	 *
	 * @return the database name
	 */
	public String getDatabase() {
		return database;
	}

	/**
	 * Returns the table name.
	 *
	 * @return the table name
	 */
	public String getTable() {
		return table;
	}

	@Override
	public String toString() {
		return JsonObject.toString(this);
	}

	/**
	 * An insert event.
	 */
	public static class Insert extends DatabaseEvent {
		private List<Map<String,Object>> values;

		/**
		 * Constructs a new insert event.
		 *
		 * @param database the database name
		 * @param table the table name
		 * @param values the objects that were inserted
		 */
		public Insert(String database, String table,
				List<Map<String,Object>> values) {
			super(Type.INSERT, database, table);
			this.values = values;
		}

		/**
		 * Returns the objects that were inserted.
		 *
		 * @return the objects that were inserted
		 */
		public List<Map<String, Object>> getValues() {
			return values;
		}
	}

	/**
	 * An update event.
	 */
	public static class Update extends DatabaseEvent {
		private DatabaseCriteria criteria;
		private Map<String,?> values;

		/**
		 * Constructs a new update event.
		 *
		 * @param database the database name
		 * @param table the table name
		 * @param criteria the criteria to select the objects that were updated
		 * @param values the new values for the updated objects
		 */
		public Update(String database, String table, DatabaseCriteria criteria,
				Map<String,?> values) {
			super(Type.UPDATE, database, table);
			this.criteria = criteria;
			this.values = values;
		}

		/**
		 * Returns the criteria to select the objects that were updated.
		 *
		 * @return the criteria to select the objects that were updated
		 */
		public DatabaseCriteria getCriteria() {
			return criteria;
		}

		/**
		 * Returns the new values for the updated objects.
		 *
		 * @return the new values for the updated objects
		 */
		public Map<String, ?> getValues() {
			return values;
		}
	}

	/**
	 * A delete event.
	 */
	public static class Delete extends DatabaseEvent {
		private DatabaseCriteria criteria;

		/**
		 * Constructs a new delete event.
		 *
		 * @param database the database name
		 * @param table the table name
		 * @param criteria the criteria to select the objects that were deleted
		 */
		public Delete(String database, String table,
				DatabaseCriteria criteria) {
			super(Type.DELETE, database, table);
			this.criteria = criteria;
		}

		/**
		 * Returns the criteria to select the objects that were deleted.
		 *
		 * @return the criteria to select the objects that were deleted
		 */
		public DatabaseCriteria getCriteria() {
			return criteria;
		}
	}
}
