package nl.rrd.senseeact.service.controller.project;

import nl.rrd.senseeact.client.exception.HttpFieldError;
import nl.rrd.senseeact.client.model.RegexUserAccess;
import nl.rrd.senseeact.client.model.RegexUserAccessTable;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.dao.DatabaseSort;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegexUserAccessController {
	private static final Object LOCK = new Object();

	private RegexUserAccessTable table;

	public RegexUserAccessController(RegexUserAccessTable table) {
		this.table = table;
	}

	public List<String> getUserAccess(ProtocolVersion version, Database authDb,
			User user, String subject) throws HttpException, Exception {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		DatabaseCriteria criteria = new DatabaseCriteria.Equal("user",
				subjectUser.getUserid());
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("emailRegex", true)
		};
		List<RegexUserAccess> userAccessList = authDb.select(table, criteria, 0,
				sort);
		List<String> result = new ArrayList<>();
		for (RegexUserAccess userAccess : userAccessList) {
			result.add(userAccess.getEmailRegex());
		}
		return result;
	}

	public Object addUserAccess(ProtocolVersion version, Database authDb,
			User user, String subject, String emailRegex) throws HttpException,
			Exception {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		try {
			Pattern.compile(emailRegex);
		} catch (IllegalArgumentException ex) {
			HttpFieldError error = new HttpFieldError("emailRegex",
					"Invalid regular expression: " + emailRegex);
			throw BadRequestException.withInvalidInput(error);
		}
		synchronized (LOCK) {
			DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", subjectUser.getUserid()),
					new DatabaseCriteria.Equal("emailRegex", emailRegex)
			);
			RegexUserAccess userAccess = authDb.selectOne(table, criteria,
					null);
			if (userAccess != null)
				return null;
			userAccess = new RegexUserAccess(subjectUser.getUserid(),
					emailRegex);
			authDb.insert(table.getName(), userAccess);
			return null;
		}
	}

	public Object removeUserAccess(ProtocolVersion version, Database authDb,
			User user, String subject, String emailRegex) throws HttpException,
			Exception {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		synchronized (LOCK) {
			DatabaseCriteria criteria;
			if (emailRegex == null || emailRegex.isEmpty()) {
				criteria = new DatabaseCriteria.Equal("user",
						subjectUser.getUserid());
 			} else {
				criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", subjectUser.getUserid()),
					new DatabaseCriteria.Equal("emailRegex", emailRegex)
				);
			}
			authDb.delete(table, criteria);
		}
		return null;
	}
}
