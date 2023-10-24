package nl.rrd.senseeact.service;

import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.senseeact.service.model.User;

import java.util.List;

public interface ProjectUserAccessControl {
	List<DatabaseTableDef<?>> getTables();
	List<User> findAccessibleUsers(Database authDb, User user,
			List<User> projectUsers) throws DatabaseException;
	boolean isAccessibleUser(Database authDb, User user, User subject)
			throws DatabaseException;
}
