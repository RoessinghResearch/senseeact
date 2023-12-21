package nl.rrd.senseeact.service.access;

import nl.rrd.senseeact.client.model.RegexUserAccess;
import nl.rrd.senseeact.client.model.RegexUserAccessTable;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.dao.DatabaseTableDef;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.utils.exception.DatabaseException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegexUserAccessControl implements ProjectUserAccessControl {
	private RegexUserAccessTable table;

	public RegexUserAccessControl(RegexUserAccessTable table) {
		this.table = table;
	}

	@Override
	public List<DatabaseTableDef<?>> getTables() {
		List<DatabaseTableDef<?>> tables = new ArrayList<>();
		tables.add(table);
		return tables;
	}

	@Override
	public List<User> findAccessibleUsers(Database authDb, User user,
			List<User> projectUsers) throws DatabaseException {
		List<Pattern> patterns = findRegexAccessibleUserPatterns(authDb, user);
		List<User> result = new ArrayList<>();
		for (User projectUser : projectUsers) {
			if (emailMatchesPattern(projectUser.getEmail(), patterns)) {
				result.add(projectUser);
			}
		}
		return result;
	}

	public List<Pattern> findRegexAccessibleUserPatterns(Database authDb,
			User user) throws DatabaseException {
		List<Pattern> result = new ArrayList<>();
		DatabaseCriteria criteria = new DatabaseCriteria.Equal(
				"user", user.getUserid());
		List<RegexUserAccess> accessList = authDb.select(table, criteria, 0,
				null);
		if (accessList.isEmpty())
			return result;
		for (RegexUserAccess access : accessList) {
			result.add(Pattern.compile(access.getEmailRegex()));
		}
		return result;
	}

	public boolean emailMatchesPattern(String email, List<Pattern> regexList) {
		for (Pattern regex : regexList) {
			if (regex.matcher(email).matches())
				return true;
		}
		return false;
	}

	@Override
	public boolean isAccessibleUser(Database authDb, User user, User subject)
			throws DatabaseException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal(
				"user", user.getUserid());
		List<RegexUserAccess> accessList = authDb.select(table, criteria, 0,
				null);
		for (RegexUserAccess access : accessList) {
			if (subject.getEmail().matches(access.getEmailRegex()))
				return true;
		}
		return false;
	}
}
