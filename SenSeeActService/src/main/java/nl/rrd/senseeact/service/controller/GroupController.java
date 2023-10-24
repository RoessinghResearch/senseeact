package nl.rrd.senseeact.service.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.HttpFieldError;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.client.model.ShortUserProfile;
import nl.rrd.senseeact.client.model.compat.GroupV0;
import nl.rrd.senseeact.client.model.compat.GroupV1;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.dao.DatabaseSort;
import nl.rrd.senseeact.service.HttpContentReader;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.QueryRunner;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.exception.NotFoundException;
import nl.rrd.senseeact.service.model.*;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.validation.MapReader;
import nl.rrd.utils.validation.TypeConversion;
import nl.rrd.utils.validation.Validation;
import nl.rrd.utils.validation.ValidationException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v{version}/group")
public class GroupController {
	public static final Object GROUP_LOCK = new Object();
	
	@RequestMapping(value="/", method=RequestMethod.POST)
	public void createGroup(
			final HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="name")
			final String name) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, user) ->
				doCreateGroup(version, authDb, user, name, request),
				versionName, request, response);
	}
	
	@RequestMapping(value="/", method=RequestMethod.GET)
	public Object getGroup(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="name")
			final String name,
			@RequestParam(value="includeInactiveMembers", required=false, defaultValue="true")
			final String includeInactiveMembers) throws HttpException, Exception {
		return QueryRunner.runAuthQuery(
				(version, authDb, user) ->
				doGetGroup(version, authDb, user, name, includeInactiveMembers),
				versionName, request, response);
	}
	
	@RequestMapping(value="/list", method=RequestMethod.GET)
	public List<String> getGroupList(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="domain", required=false, defaultValue="")
			final String domain) throws HttpException, Exception {
		return QueryRunner.runAuthQuery(
				(version, authDb, user) -> doGetGroupList(authDb, user, domain),
				versionName, request, response);
	}
	
	@RequestMapping(value="/", method=RequestMethod.DELETE)
	public void deleteGroup(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="name")
			final String name) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, user) -> doDeleteGroup(authDb, user, name),
				versionName, request, response);
	}
	
	@RequestMapping(value="/member", method=RequestMethod.POST)
	public void addMember(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="group")
			final String group,
			@RequestParam(value="member")
			final String member) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, user) ->
				doAddMember(version, authDb, user, group, member),
				versionName, request, response);
	}
	
	@RequestMapping(value="/member", method=RequestMethod.DELETE)
	public void deleteMember(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="group")
			final String group,
			@RequestParam(value="member")
			final String member) throws HttpException, Exception {
		QueryRunner.runAuthQuery(
				(version, authDb, user) ->
				doDeleteMember(version, authDb, user, group, member),
				versionName, request, response);
	}
	
	private Object doCreateGroup(ProtocolVersion version, Database authDb,
			User user, String name, HttpServletRequest request)
			throws HttpException, Exception {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		Map<String,?> inputMap = HttpContentReader.readJsonParams(request,
				true);
		List<String> uniqueMemberNames;
		if (inputMap == null) {
			uniqueMemberNames = new ArrayList<>();
		} else {
			MapReader mapReader = new MapReader(inputMap);
			uniqueMemberNames = new ArrayList<>();
			List<String> members = mapReader.readJson("members",
					new TypeReference<>() {});
			for (String member : members) {
				if (!uniqueMemberNames.contains(member))
					uniqueMemberNames.add(member);
			}
		}
		UserCache userCache = UserCache.getInstance();
		List<String> invalidMembers = new ArrayList<>();
		List<User> memberUsers = new ArrayList<>();
		for (String member : uniqueMemberNames) {
			User memberUser = userCache.find(version, member);
			if (memberUser == null)
				invalidMembers.add(member);
			else
				memberUsers.add(memberUser);
		}
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		if (!invalidMembers.isEmpty()) {
			StringBuilder invalidMembersStr = new StringBuilder();
			for (String invalidMember : invalidMembers) {
				if (invalidMembersStr.length() > 0)
					invalidMembersStr.append(", ");
				invalidMembersStr.append(invalidMember);
			}
			fieldErrors.add(new HttpFieldError("members",
					"Invalid group members: " + invalidMembersStr));
		}
		try {
			Validation.validateEmail(name);
		} catch (ValidationException ex) {
			fieldErrors.add(new HttpFieldError("name",
					"Group name not formatted as an email address: " + name));
		}
		if (!fieldErrors.isEmpty())
			throw BadRequestException.withInvalidInput(fieldErrors);
		synchronized (GROUP_LOCK) {
			Group group = Group.findByName(authDb, name);
			if (group != null) {
				String errorMsg = "Group with name \"" + name +
						"\" already exists";
				HttpError error = new HttpError(ErrorCode.GROUP_ALREADY_EXISTS,
						errorMsg);
				error.addFieldError(new HttpFieldError("name", errorMsg));
				throw new ForbiddenException(error);
			}
			group = new Group();
			group.setName(name);
			authDb.insert(GroupTable.NAME, group);
			if (!memberUsers.isEmpty()) {
				List<GroupMember> groupMembers = new ArrayList<>();
				for (User member : memberUsers) {
					GroupMember groupMember = new GroupMember();
					groupMember.setGroupId(group.getId());
					groupMember.setUser(member.getUserid());
					groupMembers.add(groupMember);
				}
				authDb.insert(GroupMemberTable.NAME, groupMembers);
			}
		}
		return null;
	}
	
	private Object doGetGroup(ProtocolVersion version, Database authDb,
			User user, String name, String includeInactiveMembersStr)
			throws HttpException, Exception {
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		try {
			Validation.validateEmail(name);
		} catch (ValidationException ex) {
			fieldErrors.add(new HttpFieldError("name",
					"Group name not formatted as an email address: " + name));
		}
		boolean includeInactiveMembers = false;
		try {
			includeInactiveMembers = TypeConversion.getBoolean(
					includeInactiveMembersStr);
		} catch (ParseException ex) {
			fieldErrors.add(new HttpFieldError("includeInactiveMembers",
					ex.getMessage()));
		}
		if (!fieldErrors.isEmpty())
			throw BadRequestException.withInvalidInput(fieldErrors);
		synchronized (GROUP_LOCK) {
			Group group = Group.findByName(authDb, name);
			if (user.getRole() == Role.ADMIN) {
				if (group == null) {
					String message = String.format(
							"Group with name \"%s\" not found", name);
					HttpError error = new HttpError(message);
					error.addFieldError(new HttpFieldError("name", message));
					throw new NotFoundException(error);
				}
			} else {
				List<String> groupIds = null;
				if (group != null) {
					groupIds = Group.findGroupIdsForUser(authDb,
							user.getUserid());
				}
				if (groupIds == null || !groupIds.contains(group.getId())) {
					throw new ForbiddenException(String.format(
							"Group with name \"%s\" not found or access denied",
							name));
				}
			}
			List<User> members = group.findGroupMemberUsers(authDb,
					includeInactiveMembers);
			members.removeIf(member ->
					member.getRole().ordinal() < user.getRole().ordinal());
			nl.rrd.senseeact.client.model.Group resultGroup =
					new nl.rrd.senseeact.client.model.Group(name);
			for (User member : members) {
				resultGroup.addMember(new ShortUserProfile(member));
			}
			Object result;
			if (version.ordinal() >= ProtocolVersion.V6_0_3.ordinal())
				result = resultGroup;
			else if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal())
				result = GroupV1.fromGroup(resultGroup, members);
			else
				result = GroupV0.fromGroup(resultGroup, members);
			return result;
		}
	}
	
	private List<String> doGetGroupList(Database authDb, User user,
			String domain) throws HttpException, Exception {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		GroupTable table = new GroupTable();
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("name", true)
		};
		List<Group> groups = authDb.select(table, null, 0, sort);
		List<String> result = new ArrayList<>();
		for (Group group : groups) {
			if (group.getGroupType() != Group.Type.MULTI_USER)
				continue;
			String name = group.getName();
			if (domain != null && domain.length() > 0) {
				String groupDomain = name.substring(name.indexOf('@') + 1);
				if (!groupDomain.equals(domain))
					continue;
			}
			result.add(group.getName());
		}
		return result;
	}
	
	private Object doDeleteGroup(Database authDb, User user, String name)
			throws HttpException, Exception {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		try {
			Validation.validateEmail(name);
		} catch (ValidationException ex) {
			throw BadRequestException.withInvalidInput(new HttpFieldError(
					"name", "Group name not formatted as an email address: " +
					name));
		}
		deleteGroup(authDb, name);
		return null;
	}
	
	public static void deleteGroup(Database authDb, String name)
			throws HttpException, DatabaseException {
		synchronized (GROUP_LOCK) {
			Group group = Group.findByName(authDb, name);
			if (group == null) {
				HttpFieldError fieldError = new HttpFieldError("name",
						"Group with name \"" + name + "\" not found");
				throw BadRequestException.withInvalidInput(fieldError);
			}
			DatabaseCriteria criteria = new DatabaseCriteria.Equal(
					"groupId", group.getId());
			authDb.delete(new GroupMemberTable(), criteria);
			authDb.delete(GroupTable.NAME, group);
		}
	}
	
	private Object doAddMember(ProtocolVersion version, Database authDb,
			User user, String groupName, String memberId) throws HttpException,
			Exception {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		try {
			Validation.validateEmail(groupName);
		} catch (ValidationException ex) {
			throw BadRequestException.withInvalidInput(new HttpFieldError(
					"group", "Group name not formatted as an email address: " +
					groupName));
		}
		synchronized (GROUP_LOCK) {
			List<HttpFieldError> fieldErrors = new ArrayList<>();
			Group group = Group.findByName(authDb, groupName);
			if (group == null) {
				fieldErrors.add(new HttpFieldError("group",
						"Group with name \"" + groupName + "\" not found"));
			}
			UserCache userCache = UserCache.getInstance();
			User member = userCache.find(version, memberId);
			if (member == null) {
				fieldErrors.add(new HttpFieldError("member",
						"User \"" + memberId + "\" not found"));
			}
			if (!fieldErrors.isEmpty())
				throw BadRequestException.withInvalidInput(fieldErrors);
			DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("groupId", group.getId()),
					new DatabaseCriteria.Equal("user", member.getUserid())
			);
			GroupMember groupMember = authDb.selectOne(new GroupMemberTable(),
					criteria, null);
			if (groupMember != null)
				return null;
			groupMember = new GroupMember();
			groupMember.setGroupId(group.getId());
			groupMember.setUser(member.getUserid());
			authDb.insert(GroupMemberTable.NAME, groupMember);
		}
		return null;
	}
	
	private Object doDeleteMember(ProtocolVersion version, Database authDb,
			User user, String groupName, String memberId) throws HttpException,
			Exception {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		try {
			Validation.validateEmail(groupName);
		} catch (ValidationException ex) {
			throw BadRequestException.withInvalidInput(new HttpFieldError(
					"group", "Group name not formatted as an email address: " +
					groupName));
		}
		UserCache userCache = UserCache.getInstance();
		User member = userCache.find(version, memberId);
		if (member == null) {
			throw BadRequestException.withInvalidInput(new HttpFieldError(
					"member", "User \"" + member + "\" not found"));
		}
		deleteMember(authDb, groupName, member);
		return null;
	}

	public static void deleteMember(Database authDb, String groupName,
			User member) throws HttpException, DatabaseException {
		synchronized (GROUP_LOCK) {
			List<HttpFieldError> fieldErrors = new ArrayList<>();
			Group group = Group.findByName(authDb, groupName);
			if (group == null) {
				fieldErrors.add(new HttpFieldError("group",
						"Group with name \"" + groupName + "\" not found"));
			}
			if (!fieldErrors.isEmpty())
				throw BadRequestException.withInvalidInput(fieldErrors);
			DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("groupId", group.getId()),
					new DatabaseCriteria.Equal("user", member.getUserid())
			);
			authDb.delete(new GroupMemberTable(), criteria);
		}
	}
}
