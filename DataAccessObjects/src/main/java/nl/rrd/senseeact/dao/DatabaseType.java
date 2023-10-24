package nl.rrd.senseeact.dao;

/**
 * The possible types of database columns. See {@link DatabaseObjectMapper
 * DatabaseObjectMapper} for more details.
 * 
 * @author Dennis Hofs (RRD)
 */
public enum DatabaseType {
	BYTE,
	SHORT,
	INT,
	LONG,
	FLOAT,
	DOUBLE,
	STRING,
	TEXT,
	DATE,
	TIME,
	DATETIME,
	ISOTIME
}
