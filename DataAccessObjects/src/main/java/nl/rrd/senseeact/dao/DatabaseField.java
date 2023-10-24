package nl.rrd.senseeact.dao;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be added to a field in a {@link DatabaseObject
 * DatabaseObject} to indicate that it can be read from or written to the
 * database. The value specifies the database type.
 * 
 * <p>See {@link DatabaseObjectMapper DatabaseObjectMapper} for information
 * about the mapping between Java types and database types.</p>
 * 
 * @author Dennis Hofs (RRD)
 */
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.FIELD)
@Documented
public @interface DatabaseField {
	
	/**
	 * The database type of this field.
	 * 
	 * @return the database type of this field
	 */
	DatabaseType value();

	/**
	 * True if the database should create an index on this field to make select
	 * queries run faster. The default is false. Compound indices can be
	 * defined in {@link DatabaseTableDef DatabaseTableDef}.
	 * 
	 * @return true if the database should create an index on this field, false
	 * otherwise
	 */
	boolean index() default false;

	/**
	 * True if the database type of element type is a {@link
	 * DatabaseType#STRING STRING} or {@link DatabaseType#TEXT TEXT} that
	 * contains a JSON string. In the database it is stored as a string, but the
	 * API returns it as JSON code.
	 *
	 * @return true if the database field contains a JSON string, false
	 * otherwise (default)
	 */
	boolean json() default false;
}
