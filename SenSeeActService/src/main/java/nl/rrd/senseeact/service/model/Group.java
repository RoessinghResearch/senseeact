package nl.rrd.senseeact.service.model;

import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.validation.Validation;
import nl.rrd.utils.validation.ValidationException;
import nl.rrd.senseeact.dao.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class models a user group. SenSeeAct has two types of groups:
 * 
 * <p><ul>
 * <li>Multi-user group: The names of these groups are formatted as an email
 * address: "group@example.com". These groups are created with API call
 * POST /group/.</li>
 * 
 * <li>User access group: This group defines that a professional user has
 * access to a patient user. The names of these groups are formatted as
 * "profuserid|patientuserid". They are created with API call
 * POST /access/subject.</li>
 * </ul></p>
 * 
 * @author Dennis Hofs (RRD)
 */
public class Group extends AbstractDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING)
	private String name;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Returns the group type (multi-user or user access). See the top of this
	 * page for more info.
	 * 
	 * @return the group type
	 */
	public Type getGroupType() {
		if (name.contains("|"))
			return Type.USER_ACCESS;
		try {
			Validation.validateEmail(name);
			return Type.MULTI_USER;
		} catch (ValidationException ex) {
			return Type.USER_ACCESS;
		}
	}
	
	/**
	 * Finds the group with the specified name. If there is no such group, this
	 * method returns null.
	 * 
	 * @param authDb the authentication database
	 * @param name the group name
	 * @return the group or null
	 * @throws DatabaseException if a database error occurs
	 */
	public static Group findByName(Database authDb, String name)
			throws DatabaseException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal("name", name);
		return authDb.selectOne(new GroupTable(), criteria, null);
	}
	
	/**
	 * Returns the groups where the specified user is a member. You can
	 * optionally filter the result by type of group. If you only need the group
	 * IDs and no filtering, it's faster to call {@link
	 * #findGroupIdsForUser(Database, String) findGroupIdsForUser()}.
	 * 
	 * @param authDb the authentication database
	 * @param user the user ID of the user
	 * @param groupType the desired type of groups or null
	 * @return the groups
	 * @throws DatabaseException if a database error occurs
	 */
	public static List<Group> findGroupsForUser(Database authDb, String user,
			Type groupType) throws DatabaseException {
		List<String> groupIds = findGroupIdsForUser(authDb, user);
		List<Group> result = new ArrayList<>();
		for (String groupId : groupIds) {
			DatabaseCriteria criteria = new DatabaseCriteria.Equal(
					"id", groupId);
			Group group = authDb.selectOne(new GroupTable(), criteria, null);
			if (groupType == null || group.getGroupType() == groupType)
				result.add(group);
		}
		result.sort(new GroupNameComparator());
		return result;
	}
	
	/**
	 * Returns the IDs of the groups where the specified user is a member. You
	 * can optionally filter the result by type of group. This method is faster
	 * if you don't specify a group type. Otherwise it does the same as {@link
	 * #findGroupsForUser(Database, String, Type) findGroupsForUser()}.
	 * 
	 * @param authDb the authentication database
	 * @param user the user ID of the user
	 * @param groupType the desired type of groups or null
	 * @return the group IDs
	 * @throws DatabaseException if a database error occurs
	 */
	public static List<String> findGroupIdsForUser(Database authDb, String user,
			Type groupType) throws DatabaseException {
		if (groupType == null)
			return findGroupIdsForUser(authDb, user);
		List<Group> groups = findGroupsForUser(authDb, user, groupType);
		List<String> result = new ArrayList<>();
		for (Group group : groups) {
			result.add(group.getId());
		}
		return result;
	}
	
	/**
	 * Returns the IDs of all groups where the specified user is a member.
	 * 
	 * @param authDb the authentication database
	 * @param user the user ID of the user
	 * @return the group IDs
	 * @throws DatabaseException if a database error occurs
	 */
	public static List<String> findGroupIdsForUser(Database authDb, String user)
			throws DatabaseException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal("user", user);
		List<GroupMember> memberships = authDb.select(new GroupMemberTable(),
				criteria, 0, null);
		List<String> result = new ArrayList<>();
		for (GroupMember membership : memberships) {
			if (!result.contains(membership.getGroupId()))
				result.add(membership.getGroupId());
		}
		return result;
	}
	
	/**
	 * Returns the user IDs of the users who are a member of this group.
	 * 
	 * @param authDb the authentication database
	 * @return the group members
	 * @throws DatabaseException if a database error occurs
	 */
	public List<String> findGroupMembers(Database authDb)
			throws DatabaseException {
		return findGroupMembers(authDb, getId());
	}
	
	/**
	 * Returns the User objects of the users who are a member of this group.
	 * If you only need the user IDs of the group members, then {@link
	 * #findGroupMembers(Database) findGroupMembers()} is faster.
	 * 
	 * @param authDb the authentication database
	 * @param includeInactive true if inactive users should be included, false
	 * if only active users should be returned
	 * @return the group members
	 * @throws DatabaseException if a database error occurs
	 */
	public List<User> findGroupMemberUsers(Database authDb,
			boolean includeInactive) throws DatabaseException {
		return findGroupMemberUsers(authDb, getId(), includeInactive);
	}
	
	/**
	 * Returns the user IDs of the users who are a member of the specified
	 * group.
	 * 
	 * @param authDb the authentication database
	 * @param groupId the group ID
	 * @return the group members
	 * @throws DatabaseException if a database error occurs
	 */
	public static List<String> findGroupMembers(Database authDb,
			String groupId) throws DatabaseException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal(
				"groupId", groupId);
		List<String> result = new ArrayList<>();
		List<GroupMember> members = authDb.select(new GroupMemberTable(),
				criteria, 0, null);
		for (GroupMember member : members) {
			result.add(member.getUser());
		}
		return result;
	}
	
	/**
	 * Returns the User objects of the users who are a member of the specified
	 * group. If you only need the user IDs of the group members, then {@link
	 * #findGroupMembers(Database, String) findGroupMembers()} is faster.
	 * 
	 * @param authDb the authentication database
	 * @param groupId the group ID
	 * @param includeInactive true if inactive users should be included, false
	 * if only active users should be returned
	 * @return the group members
	 * @throws DatabaseException if a database error occurs
	 */
	public static List<User> findGroupMemberUsers(Database authDb,
			String groupId, boolean includeInactive) throws DatabaseException {
		List<String> userids = findGroupMembers(authDb, groupId);
		List<User> result = new ArrayList<>();
		UserCache userCache = UserCache.getInstance();
		for (String userid : userids) {
			User user = userCache.findByUserid(userid);
			if (user != null && (includeInactive || user.isActive()))
				result.add(user);
		}
		return result;
	}
	
	// get members of GROUP_MEMBER_BATCH groups at a time
	private static final int GROUP_MEMBER_BATCH = 100;
	
	/**
	 * Returns the user IDs of all members of the specified groups.
	 * 
	 * @param authDb the authentication database
	 * @param groupIds the group IDs
	 * @return the group members
	 * @throws DatabaseException if a database error occurs
	 */
	public static List<String> findGroupMembers(Database authDb,
			List<String> groupIds) throws DatabaseException {
		List<String> result = new ArrayList<>();
		groupIds = new ArrayList<>(groupIds);
		while (!groupIds.isEmpty()) {
			List<String> groupBatch = getNextGroupBatch(groupIds);
			List<DatabaseCriteria> orCriteria = new ArrayList<>();
			for (String groupId : groupBatch) {
				orCriteria.add(new DatabaseCriteria.Equal("groupId", groupId));
			}
			DatabaseCriteria[] orArray = orCriteria.toArray(
					new DatabaseCriteria[0]);
			DatabaseCriteria criteria = new DatabaseCriteria.Or(orArray);
			List<GroupMember> members = authDb.select(new GroupMemberTable(),
					criteria, 0, null);
			for (GroupMember member : members) {
				if (!result.contains(member.getUser()))
					result.add(member.getUser());
			}
		}
		Collections.sort(result);
		return result;
	}
	
	/**
	 * Gets the next batch of groups from the specified list. It returns a
	 * batch with a maximum size of GROUP_MEMBER_BATCH. The returned groups
	 * are removed from the specified list.
	 * 
	 * @param groupIds the group IDs from which the next batch is taken
	 * @return the next batch
	 */
	private static List<String> getNextGroupBatch(List<String> groupIds) {
		int count = groupIds.size();
		if (count > GROUP_MEMBER_BATCH)
			count = GROUP_MEMBER_BATCH;
		List<String> result = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			result.add(groupIds.remove(0));
		}
		return result;
	}
	
	private static class GroupNameComparator implements Comparator<Group> {
		@Override
		public int compare(Group o1, Group o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	/**
	 * The types of groups. See the top of this page for more info.
	 */
	public enum Type {
		MULTI_USER,
		USER_ACCESS
	}
}
