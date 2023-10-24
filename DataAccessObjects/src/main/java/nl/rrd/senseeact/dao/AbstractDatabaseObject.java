package nl.rrd.senseeact.dao;

import java.util.Map;

import nl.rrd.utils.DataFormatter;
import nl.rrd.utils.beans.PropertyReader;
import nl.rrd.utils.beans.PropertyWriter;
import nl.rrd.utils.exception.ParseException;

/**
 * Base implementation of {@link DatabaseObject DatabaseObject}. It implements
 * getId() and setId() and provides a toString() that returns the values of all
 * database fields and a hashCode() and equals() that work on the map that is
 * generated from this object using a {@link DatabaseObjectMapper
 * DatabaseObjectMapper}.
 *
 * <p>If you add a "user" field, it will have a special meaning when database
 * actions are saved for synchronisation with a remote database. It means that
 * the object belongs to the specified user and can be used if only the data for
 * one user should be synchronised.</p>
 * 
 * <p>Set methods may throw a {@link ParseException ParseException}. In that
 * case the parameter value will be treated as illegal and the {@link
 * DatabaseObjectMapper DatabaseObjectMapper} will throw a {@link
 * DatabaseFieldException DatabaseFieldException}.</p>
 *
 * @author Dennis Hofs (RRD)
 */
public abstract class AbstractDatabaseObject implements DatabaseObject,
		Cloneable {
	private String id;
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return toString(false);
	}

	/**
	 * Copies the values from another database object into this object.
	 *
	 * @param other the other database object
	 */
	public void copyFrom(DatabaseObject other) {
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		Map<String,Object> map = mapper.objectToMap(other, false);
		other = mapper.mapToObject(map, other.getClass(), false);
		for (String prop : map.keySet()) {
			Object value = PropertyReader.readProperty(other, prop);
			PropertyWriter.writeProperty(this, prop, value);
		}
	}
	
	/**
	 * Returns a string representation of this object. If "human" is true, the
	 * returned string will have a friendly formatting, possibly spanning
	 * multiple lines.
	 * 
	 * @param human true for friendly formatting, false for single-line
	 * formatting
	 * @return the string
	 */
	public String toString(boolean human) {
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		Map<String,Object> map = mapper.objectToMap(this, true);
		DataFormatter formatter = new DataFormatter();
		return getClass().getSimpleName() + " " + formatter.format(map, human);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		Map<String,Object> map = mapper.objectToMap(this, false);
		result = prime * result + map.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractDatabaseObject other = (AbstractDatabaseObject)obj;
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		Map<String,Object> map = mapper.objectToMap(this, false);
		Map<String,Object> otherMap = mapper.objectToMap(other, false);
		return map.equals(otherMap);
	}
	
	/**
	 * Compares the value fields of this database object with another database
	 * object. The "id" field is ignored.
	 * 
	 * @param obj the other object
	 * @return true if both objects have the same values, false otherwise
	 */
	public boolean equalValues(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractDatabaseObject other = (AbstractDatabaseObject)obj;
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		Map<String,Object> map = mapper.objectToMap(this, false);
		map.remove("id");
		Map<String,Object> otherMap = mapper.objectToMap(other, false);
		otherMap.remove("id");
		return map.equals(otherMap);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		return mapper.mapToObject(mapper.objectToMap(this, false), getClass(),
				false);
	}
}
