package nl.rrd.senseeact.service.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.HttpFieldError;
import nl.rrd.senseeact.client.model.PermissionRecord;
import nl.rrd.senseeact.client.model.ProjectDataModule;
import nl.rrd.senseeact.client.model.ProjectUserAccessRule;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.*;
import nl.rrd.senseeact.service.PermissionManager;
import nl.rrd.senseeact.service.PermissionRepository;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.UserListenerRepository;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.exception.NotFoundException;
import nl.rrd.senseeact.service.model.*;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.io.FileUtils;
import nl.rrd.utils.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccessControllerExecution {
	public List<ProjectDataModule> getModuleList(String projectCode) {
		ProjectRepository projects = AppComponents.get(ProjectRepository.class);
		BaseProject project = projects.findProjectByCode(projectCode);
		Map<String,List<DatabaseTableDef<?>>> modules =
				project.getModuleTables();
		List<ProjectDataModule> result = new ArrayList<>();
		for (String key : modules.keySet()) {
			ProjectDataModule module = new ProjectDataModule(key);
			List<DatabaseTableDef<?>> tables = modules.get(key);
			for (DatabaseTableDef<?> table : tables) {
				module.addTable(table.getName());
			}
			result.add(module);
		}
		return result;
	}

	public List<ProjectUserAccessRule> getGranteeList(
			ProtocolVersion version, Database authDb, User user, String project,
			String subject) throws HttpException, Exception {
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		ProjectUserAccessTable table = new ProjectUserAccessTable();
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("project", project),
				new DatabaseCriteria.Equal("subject", subjectUser.getUserid())
		);
		List<ProjectUserAccessRecord> records = authDb.select(table, criteria,
				0, null);
		List<ProjectUserAccessRule> rules = new ArrayList<>();
		for (ProjectUserAccessRecord record : records) {
			Map<String,Object> grantee = User.findPublicProfile(
					record.getGrantee());
			if (grantee == null)
				continue;
			grantee.put("emailVerified", false);
			ProjectUserAccessRule rule = record.getAccessRuleObject();
			rule.setGrantee(grantee);
			rules.add(rule);
		}
		return rules;
	}

	public List<ProjectUserAccessRule> getProjectSubjectList(
			ProtocolVersion version, Database authDb, User user, String project,
			String grantee) throws HttpException, Exception {
		User granteeUser = User.findAccessibleUser(version, grantee, authDb,
				user);
		ProjectUserAccessTable table = new ProjectUserAccessTable();
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("project", project),
				new DatabaseCriteria.Equal("grantee", granteeUser.getUserid())
		);
		List<ProjectUserAccessRecord> records = authDb.select(table, criteria,
				0, null);
		List<ProjectUserAccessRule> rules = new ArrayList<>();
		for (ProjectUserAccessRecord record : records) {
			Map<String,Object> subject = User.findPublicProfile(
					record.getSubject());
			if (subject == null)
				continue;
			ProjectUserAccessRule rule = record.getAccessRuleObject();
			rule.setSubject(subject);
			rules.add(rule);
		}
		return rules;
	}

	public Object setAccessRule(ProtocolVersion version, Database authDb,
			User user, String project, String granteeEmail, String subject,
			HttpServletRequest request) throws HttpException, Exception {
		String bodyString;
		try (InputStream input = request.getInputStream()) {
			bodyString = FileUtils.readFileString(input).trim();
		}
		if (bodyString.isEmpty()) {
			throw new BadRequestException(new HttpError(ErrorCode.INVALID_INPUT,
					"No access rule specified"));
		}
		ProjectUserAccessRule rule;
		try {
			rule = JsonMapper.parse(bodyString, ProjectUserAccessRule.class);
		} catch (ParseException ex) {
			throw new BadRequestException(new HttpError(ErrorCode.INVALID_INPUT,
					"Invalid JSON content: " + ex.getMessage()));
		}
		if (rule == null) {
			throw new BadRequestException(new HttpError(ErrorCode.INVALID_INPUT,
					"No access rule specified"));
		}
		if (rule.getAccessRestriction() != null &&
				rule.getAccessRestriction().isEmpty()) {
			throw new BadRequestException(new HttpError(ErrorCode.INVALID_INPUT,
					"Access restriction is empty"));
		}
		rule.setGrantee(null);
		rule.setSubject(null);
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		UserCache userCache = UserCache.getInstance();
		User granteeUser = userCache.findByEmail(granteeEmail);
		if (granteeUser == null) {
			throw new NotFoundException(ErrorCode.USER_NOT_FOUND,
					"Grantee " + granteeEmail + " not found");
		}
		if (granteeUser.getUserid().equals(subjectUser.getUserid())) {
			throw new BadRequestException(new HttpError(ErrorCode.INVALID_INPUT,
					"Grantee and subject cannot be the same user"));
		}
		ProjectUserAccessTable table = new ProjectUserAccessTable();
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("project", project),
				new DatabaseCriteria.Equal("grantee", granteeUser.getUserid()),
				new DatabaseCriteria.Equal("subject", subjectUser.getUserid())
		);
		ProjectUserAccessRecord record = authDb.selectOne(table, criteria,
				null);
		if (record == null) {
			record = new ProjectUserAccessRecord();
			record.setProject(project);
			record.setGrantee(granteeUser.getUserid());
			record.setSubject(subjectUser.getUserid());
			record.setAccessRuleObject(rule);
			authDb.insert(table.getName(), record);
		} else {
			record.setAccessRuleObject(rule);
			authDb.update(table.getName(), record);
		}
		return null;
	}

	public Object deleteAccessRule(ProtocolVersion version, Database authDb,
			User user, String project, String grantee, String subject)
			throws HttpException, Exception {
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("project", project),
				new DatabaseCriteria.Equal("grantee", grantee),
				new DatabaseCriteria.Equal("subject", subjectUser.getUserid())
		);
		authDb.delete(new ProjectUserAccessTable(), criteria);
		return null;
	}

	public List<DatabaseObject> getSubjectList(ProtocolVersion version,
			Database authDb, User user, String forUserid,
			String includeInactiveStr) throws HttpException, Exception {
		if (user.getRole() == Role.PATIENT)
			throw new ForbiddenException();
		UserController.GetSubjectListInput input =
				UserController.getSubjectListInput(version, authDb, user,
				forUserid, null, includeInactiveStr);
		List<User> subjects = User.findSubjectUsers(authDb, input.getForUser(),
				input.isIncludeInactive());
		return UserController.getCompatUserList(version, subjects);
	}

	public Object addSubject(ProtocolVersion version, Database authDb,
			User currUser, String addToUser, String subject)
			throws HttpException, Exception {
		if (currUser.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		ValidateSubjectResult validateSubject = validateSubjectParams(version,
				addToUser, subject);
		String addToUserId = validateSubject.profUser.getUserid();
		String subjectId = validateSubject.subjectUser.getUserid();
		synchronized (GroupController.GROUP_LOCK) {
			if (subjectExists(authDb, addToUserId, subjectId)) {
				return null;
			}
			Group group = new Group();
			group.setName(addToUserId + "|" + subjectId);
			authDb.insert(GroupTable.NAME, group);
			List<GroupMember> members = new ArrayList<>();
			GroupMember member = new GroupMember();
			member.setGroupId(group.getId());
			member.setUser(addToUserId);
			members.add(member);
			member = new GroupMember();
			member.setGroupId(group.getId());
			member.setUser(subjectId);
			members.add(member);
			authDb.insert(GroupMemberTable.NAME, members);
			UserListenerRepository.getInstance().notifyUserAddedAsSubject(
					validateSubject.subjectUser, validateSubject.profUser);
		}
		return null;
	}

	/**
	 * Validates whether addToUser exists and is a professional, and whether
	 * subject exists and is a patient.
	 *
	 * @param version the protocol version
	 * @param addToUserId the user ID or email address of addToUser
	 * @param subjectId the user ID or email address of subject
	 * @return addToUser and subject
	 * @throws HttpException if the request is invalid or not allowed
	 */
	private ValidateSubjectResult validateSubjectParams(ProtocolVersion version,
			String addToUserId, String subjectId) throws HttpException {
		UserCache userCache = UserCache.getInstance();
		User addToUser = userCache.find(version, addToUserId);
		User subject = userCache.find(version, subjectId);
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		if (addToUser == null) {
			fieldErrors.add(new HttpFieldError("user", String.format(
					"User \"%s\" not found", addToUserId)));
		} else if (addToUser.getRole() != Role.PROFESSIONAL) {
			fieldErrors.add(new HttpFieldError("user", String.format(
					"User \"%s\" does not have role %s", addToUserId,
					Role.PROFESSIONAL)));
		}
		if (subject == null) {
			fieldErrors.add(new HttpFieldError("subject", String.format(
					"User \"%s\" not found", subjectId)));
		} else if (subject.getRole() != Role.PATIENT) {
			fieldErrors.add(new HttpFieldError("subject", String.format(
					"User \"%s\" does not have role %s", subjectId,
					Role.PATIENT)));
		}
		if (!fieldErrors.isEmpty())
			throw BadRequestException.withInvalidInput(fieldErrors);
		ValidateSubjectResult result = new ValidateSubjectResult();
		result.profUser = addToUser;
		result.subjectUser = subject;
		return result;
	}

	private static class ValidateSubjectResult {
		public User profUser;
		public User subjectUser;
	}

	/**
	 * Returns whether the specified addToUser already has the specified
	 * subject.
	 *
	 * @param authDb the authentication database
	 * @param addToUser the user ID of addToUser
	 * @param subject the user ID of the subject
	 * @return true if the subject already exists, false otherwise
	 * @throws DatabaseException if a database error occurs
	 */
	private boolean subjectExists(Database authDb, String addToUser,
			String subject) throws DatabaseException {
		List<Group> groups = Group.findGroupsForUser(authDb, subject,
				Group.Type.USER_ACCESS);
		for (Group group : groups) {
			List<String> members = group.findGroupMembers(authDb);
			if (members.contains(addToUser))
				return true;
		}
		return false;
	}

	public Object removeSubject(ProtocolVersion version, Database authDb,
			User currUser, String removeFromUser, String subject)
			throws HttpException, Exception {
		if (currUser.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		ValidateSubjectResult validateSubject = validateSubjectParams(version,
				removeFromUser, subject);
		String removeFromUserId = validateSubject.profUser.getUserid();
		String subjectId = validateSubject.subjectUser.getUserid();
		synchronized (GroupController.GROUP_LOCK) {
			List<Group> groups = Group.findGroupsForUser(authDb, subjectId,
					Group.Type.USER_ACCESS);
			for (Group group : groups) {
				List<String> members = group.findGroupMembers(authDb);
				if (members.contains(removeFromUserId)) {
					deleteSubjectGroup(authDb, group.getId(), validateSubject);
				}
			}
		}
		return null;
	}

	private void deleteSubjectGroup(Database authDb, String groupId,
			ValidateSubjectResult validateSubject) throws DatabaseException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal(
				"groupId", groupId);
		authDb.delete(new GroupMemberTable(), criteria);
		criteria = new DatabaseCriteria.Equal("id", groupId);
		authDb.delete(new GroupTable(), criteria);
		UserListenerRepository.getInstance().notifyUserRemovedAsSubject(
				validateSubject.subjectUser, validateSubject.profUser);
	}

	public List<Map<?,?>> getPermissions(ProtocolVersion version,
			Database authDb, User user, String subject) throws HttpException,
			DatabaseException {
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		PermissionManager permMgr = PermissionManager.getInstance();
		List<PermissionRecord> records = permMgr.getPermissions(authDb,
				subjectUser.getUserid());
		DatabaseObjectMapper mapper = new DatabaseObjectMapper();
		List<Map<?,?>> result = new ArrayList<>();
		for (PermissionRecord record : records) {
			result.add(mapper.objectToMap(record, true));
		}
		return result;
	}

	public Object grantPermission(ProtocolVersion version, Database authDb,
			User user, HttpServletRequest request, String subject,
			String permission) throws HttpException, DatabaseException,
			IOException {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		validatePermission(permission);
		Map<String,Object> params = readPermissionParams(request);
		PermissionManager permMgr = PermissionManager.getInstance();
		permMgr.grant(authDb, subjectUser.getUserid(), permission, params);
		return null;
	}

	public Object revokePermission(ProtocolVersion version, Database authDb,
			User user, HttpServletRequest request, String subject,
			String permission) throws HttpException, DatabaseException,
			IOException {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		validatePermission(permission);
		Map<String,Object> params = readPermissionParams(request);
		PermissionManager permMgr = PermissionManager.getInstance();
		permMgr.revoke(authDb, subjectUser.getUserid(), permission, params);
		return null;
	}

	public Object revokePermissionAll(ProtocolVersion version, Database authDb,
			User user, String subject, String permission) throws HttpException,
			DatabaseException {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		validatePermission(permission);
		PermissionManager permMgr = PermissionManager.getInstance();
		permMgr.revokeAll(authDb, subjectUser.getUserid(), permission);
		return null;
	}

	private void validatePermission(String permission)
			throws BadRequestException {
		PermissionRepository permRepo = AppComponents.get(
				PermissionRepository.class);
		if (!permRepo.getPermissionNames().contains(permission)) {
			HttpFieldError error = new HttpFieldError("permission",
					"Unknown permission: " + permission);
			throw BadRequestException.withInvalidInput(error);
		}
	}

	private Map<String,Object> readPermissionParams(HttpServletRequest request)
			throws BadRequestException, IOException {
		String json;
		try (InputStream input = request.getInputStream()) {
			json = FileUtils.readFileString(input);
		}
		if (json.trim().isEmpty())
			return null;
		try {
			return JsonMapper.parse(json, new TypeReference<>() {});
		} catch (ParseException ex) {
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
					"Invalid permission parameters: " + ex.getMessage());
			throw new BadRequestException(error);
		}
	}
}
