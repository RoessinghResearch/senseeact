package nl.rrd.senseeact.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.senseeact.service.access.ProjectUserAccessControl;
import nl.rrd.senseeact.service.access.ProjectUserAccessControlRepository;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.client.model.AccessMode;
import nl.rrd.senseeact.client.model.ProjectUserAccessRestriction;
import nl.rrd.senseeact.client.model.ProjectUserAccessRule;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.client.project.BaseProject;
import nl.rrd.senseeact.client.project.ProjectRepository;
import nl.rrd.senseeact.dao.*;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

public class User extends nl.rrd.senseeact.client.model.User {

	@DatabaseField(value=DatabaseType.STRING)
	@JsonIgnore
	private String password;
	
	@DatabaseField(value=DatabaseType.STRING)
	@JsonIgnore
	private String salt;
	
	@DatabaseField(value=DatabaseType.INT)
	@JsonIgnore
	private int failedLogins = 0;
	
	@DatabaseField(value=DatabaseType.ISOTIME)
	@JsonIgnore
	private ZonedDateTime accountBlockedUntil = null;

	@DatabaseField(value=DatabaseType.STRING)
	@JsonIgnore
	private String resetPasswordRequestCode = null;
	
	@DatabaseField(value=DatabaseType.ISOTIME)
	@JsonIgnore
	private ZonedDateTime resetPasswordRequestTime = null;

	@DatabaseField(value=DatabaseType.STRING)
	@JsonIgnore
	private String verifyEmailRequestCode = null;

	@DatabaseField(value=DatabaseType.ISOTIME)
	@JsonIgnore
	private ZonedDateTime verifyEmailRequestTime = null;

	@DatabaseField(value=DatabaseType.TEXT, json=true)
	@JsonIgnore
	private String mfa;

	@JsonIgnore
	private List<PrivateMfaRecord> mfaList = new ArrayList<>();

	public User() {
	}

	public User(User other) {
		copyFrom(other);
	}

	@Override
	public void copyFrom(DatabaseObject other) {
		super.copyFrom(other);
		if (!(other instanceof User otherUser))
			return;
		password = otherUser.password;
		salt = otherUser.salt;
		failedLogins = otherUser.failedLogins;
		accountBlockedUntil = otherUser.accountBlockedUntil;
		resetPasswordRequestCode = otherUser.resetPasswordRequestCode;
		resetPasswordRequestTime = otherUser.resetPasswordRequestTime;
		verifyEmailRequestCode = otherUser.verifyEmailRequestCode;
		verifyEmailRequestTime = otherUser.verifyEmailRequestTime;
		try {
			mfaList = JsonMapper.parse(JsonMapper.generate(otherUser.mfaList),
					new TypeReference<>() {});
		} catch (ParseException ex) {
			throw new RuntimeException("Failed to parse JSON code: " +
					ex.getMessage(), ex);
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public int getFailedLogins() {
		return failedLogins;
	}

	public void setFailedLogins(int failedLogins) {
		this.failedLogins = failedLogins;
	}

	public ZonedDateTime getAccountBlockedUntil() {
		return accountBlockedUntil;
	}

	public void setAccountBlockedUntil(ZonedDateTime accountBlockedUntil) {
		this.accountBlockedUntil = accountBlockedUntil;
	}

	public String getResetPasswordRequestCode() {
		return resetPasswordRequestCode;
	}

	public void setResetPasswordRequestCode(String resetPasswordRequestCode) {
		this.resetPasswordRequestCode = resetPasswordRequestCode;
	}

	public ZonedDateTime getResetPasswordRequestTime() {
		return resetPasswordRequestTime;
	}

	public void setResetPasswordRequestTime(
			ZonedDateTime resetPasswordRequestTime) {
		this.resetPasswordRequestTime = resetPasswordRequestTime;
	}

	public String getVerifyEmailRequestCode() {
		return verifyEmailRequestCode;
	}

	public void setVerifyEmailRequestCode(String verifyEmailRequestCode) {
		this.verifyEmailRequestCode = verifyEmailRequestCode;
	}

	public ZonedDateTime getVerifyEmailRequestTime() {
		return verifyEmailRequestTime;
	}

	public void setVerifyEmailRequestTime(ZonedDateTime verifyEmailRequestTime) {
		this.verifyEmailRequestTime = verifyEmailRequestTime;
	}

	/**
	 * Returns the JSON code for the multi-factor authentication list. This
	 * method is used for the DAO. Users can call {@link #getMfaList()
	 * getMfaList()}.
	 *
	 * @return the JSON code for the multi-factor authentication list
	 */
	public String getMfa() {
		return JsonMapper.generate(mfaList);
	}

	/**
	 * Sets the JSON code for the multi-factor authentication list. This method
	 * is used for the DAO. Users can call {@link #setMfaList(List)
	 * setMfaList(List)}.
	 *
	 * @param mfa the JSON code for the multi-factor authentication list
	 * @throws ParseException if the JSON code is invalid
	 */
	public void setMfa(String mfa) throws ParseException {
		mfaList = JsonMapper.parse(mfa, new TypeReference<>() {});
	}

	public List<PrivateMfaRecord> getMfaList() {
		return mfaList;
	}

	public void setMfaList(List<PrivateMfaRecord> mfaList) {
		this.mfaList = mfaList;
	}

	/**
	 * Returns the user ID if the version is 6.0.0 or higher, or otherwise the
	 * email address.
	 *
	 * @param version the protocol version
	 * @return the user ID or email address
	 */
	public String getUserid(ProtocolVersion version) {
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal())
			return getUserid();
		else
			return getEmail();
	}

	/**
	 * Returns the user IDs of all users that are a member of a group where this
	 * user is also a member. This includes inactive members. The user itself is
	 * not included.
	 * 
	 * @param authDb the authentication database
	 * @param groupType the group type or null
	 * @return the user IDs
	 * @throws DatabaseException if a database error occurs
	 */
	public List<String> findGroupUserids(Database authDb,
			Group.Type groupType) throws DatabaseException {
		List<String> groupIds = Group.findGroupIdsForUser(authDb, getUserid(),
				groupType);
		List<String> result = new ArrayList<>();
		List<String> members = Group.findGroupMembers(authDb, groupIds);
		for (String member : members) {
			if (!member.equals(getUserid()) && !result.contains(member))
				result.add(member);
		}
		Collections.sort(result);
		return result;
	}
	
	/**
	 * Returns the codes of the projects that this user can access. If the user
	 * is an admin, that is all projects. Otherwise it checks the {@link
	 * UserProjectTable UserProjectTable}. The projects will be sorted by code.
	 * 
	 * @param authDb the authentication database
	 * @return the project codes
	 * @throws DatabaseException if a database error occurs
	 */
	public List<String> findProjects(Database authDb) throws DatabaseException {
		List<String> projects = new ArrayList<>();
		if (getRole() == Role.ADMIN) {
			ProjectRepository projectRepo = AppComponents.get(
					ProjectRepository.class);
			List<BaseProject> baseProjects = projectRepo.getProjects();
			for (BaseProject project : baseProjects) {
				projects.add(project.getCode());
			}
		} else {
			DatabaseCriteria criteria = new DatabaseCriteria.Equal("user",
					getUserid());
			List<UserProject> userProjects = authDb.select(
					new UserProjectTable(), criteria, 0, null);
			for (UserProject userProject : userProjects) {
				if (!projects.contains(userProject.getProjectCode()))
					projects.add(userProject.getProjectCode());
			}
		}
		Collections.sort(projects);
		return projects;
	}

	/**
	 * Finds the user with the specified user ID or email address and checks
	 * whether the user can be accessed by the user who is currently logged in.
	 * The returned user may be inactive.
	 *
	 * <p>If the protocol version is 6.0.0 later, you should specify a user ID.
	 * Otherwise you should specify an email address.</p>
	 * 
	 * <p>All users can access themselves. Admins can access all users.
	 * Patients can only access themselves. Professionals can access themselves
	 * and all users that occur in the same group as the professional.</p>
	 *
	 * @param version the protocol version
	 * @param subject the user ID or email address of the user to retrieve.
	 * This can be null or an empty string to get the specified "user".
	 * @param authDb the authentication database
	 * @param user the user who is logged in
	 * @return the retrieved user
	 * @throws ForbiddenException if the user doesn't exist or can't be
	 * accessed
	 * @throws DatabaseException if a database error occurs
	 */
	public static User findAccessibleUser(ProtocolVersion version,
			String subject, Database authDb, User user)
			throws ForbiddenException, DatabaseException {
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal())
			return findAccessibleUserByUserid(subject, authDb, user);
		else
			return findAccessibleUserByEmail(subject, authDb, user);
	}

	/**
	 * Finds the user with the specified user ID and checks whether the user can
	 * be accessed by the user who is currently logged in. The returned user may
	 * be inactive.
	 *
	 * <p>All users can access themselves. Admins can access all users.
	 * Patients can only access themselves. Professionals can access themselves
	 * and all users that occur in the same group as the professional.</p>
	 *
	 * @param subject the user ID of the user to retrieve. This can be null or
	 * an empty string to get the specified "user".
	 * @param authDb the authentication database
	 * @param user the user who is logged in
	 * @return the retrieved user
	 * @throws ForbiddenException if the user doesn't exist or can't be
	 * accessed
	 * @throws DatabaseException if a database error occurs
	 */
	public static User findAccessibleUserByUserid(String subject,
			Database authDb, User user) throws ForbiddenException,
			DatabaseException {
		User getUser;
		if (subject == null || subject.isEmpty() ||
				subject.equals(user.getUserid())) {
			getUser = user;
		} else {
			UserCache userCache = UserCache.getInstance();
			getUser = userCache.findByUserid(subject);
		}
		return checkAccessibleUser(subject, getUser, authDb, user);
	}

	/**
	 * Finds the user with the specified email address and checks whether the
	 * user can be accessed by the user who is currently logged in. The returned
	 * user may be inactive.
	 *
	 * <p>All users can access themselves. Admins can access all users.
	 * Patients can only access themselves. Professionals can access themselves
	 * and all users that occur in the same group as the professional.</p>
	 *
	 * @param email the email address of the user to retrieve. This can be null
	 * or an empty string to get the specified "user".
	 * @param authDb the authentication database
	 * @param user the user who is logged in
	 * @return the retrieved user
	 * @throws ForbiddenException if the user doesn't exist or can't be
	 * accessed
	 * @throws DatabaseException if a database error occurs
	 */
	public static User findAccessibleUserByEmail(String email, Database authDb,
			User user) throws ForbiddenException, DatabaseException {
		User getUser;
		if (email == null || email.isEmpty() ||
				email.toLowerCase().equals(user.getEmail())) {
			getUser = user;
		} else {
			UserCache userCache = UserCache.getInstance();
			getUser = userCache.findByEmail(email);
		}
		return checkAccessibleUser(email, getUser, authDb, user);
	}

	/**
	 * Checks whether a user (parameter "user") can access another user
	 * (parameter "getUser"). If both users are the same, the check always
	 * succeeds. Otherwise this method may throw a {@link ForbiddenException
	 * ForbiddenException}. Parameter "subject" is used in the error message.
	 * You may set "getUser" to null. This means that the requested user with
	 * name "subject" was not found and a {@link ForbiddenException
	 * ForbiddenException} will be thrown.
	 *
	 * @param subject the name by which the subject was requested (user ID or
	 * email). This must be specified if "getUser" is not the same as "user".
	 * @param getUser the user that "user" wants to access. If null, this is
	 * interpreted as the requested user not existing.
	 * @param authDb the authentication database
	 * @param user the user that wants to access "getUser".
	 * @return the accessible user ("getUser")
	 * @throws ForbiddenException if "getUser" is null (requested user does not
	 * exist) or if "user" cannot access "getUser"
	 * @throws DatabaseException if a database error occurs
	 */
	public static User checkAccessibleUser(String subject, User getUser,
			Database authDb, User user)
			throws ForbiddenException, DatabaseException {
		if (getUser == null) {
			throw new ForbiddenException(String.format(
					"User %s not found or access forbidden", subject));
		}
		// admins can access all users
		// all users can access themselves
		if (user.getRole() == Role.ADMIN ||
				getUser.getUserid().equals(user.getUserid())) {
			return getUser;
		}
		// patients can only access themselves
		if (user.getRole() == Role.PATIENT) {
			throw new ForbiddenException(String.format(
					"User %s not found or access forbidden", subject));
		}
		if (getUser.getRole() == Role.ADMIN) {
			throw new ForbiddenException(String.format(
					"User %s not found or access forbidden", subject));
		}
		// professionals can access all users in the same group
		Set<String> accessibleUsers = new HashSet<>(
				user.findGroupUserids(authDb, null));
		ProjectUserAccessControlRepository uacRepo = AppComponents.get(
				ProjectUserAccessControlRepository.class);
		Map<String,ProjectUserAccessControl> projectMap =
				uacRepo.getProjectMap();
		if (accessibleUsers.contains(getUser.getUserid()))
			return getUser;
		for (String project : projectMap.keySet()) {
			ProjectUserAccessControl accessControl = projectMap.get(project);
			if (accessControl.isAccessibleUser(authDb, user, getUser)) {
				return getUser;
			}
		}
		throw new ForbiddenException(String.format(
				"User %s not found or access forbidden", subject));
	}

	/**
	 * Returns all users that were added as a subject to the specified user. It
	 * is possible to filter by active status. This method should not be called
	 * for users with role PATIENT.
	 *
	 * @param authDb the authentication database
	 * @param user the user for whom accessible users should be retrieved
	 * @param includeInactive true if inactive subjects should be included,
	 * false if only active subjects should be returned
	 * @return the subjects
	 * @throws DatabaseException if a database error occurs
	 */
	public static List<User> findSubjectUsers(Database authDb, User user,
			boolean includeInactive) throws DatabaseException {
		Set<String> accessUserids = new HashSet<>(user.findGroupUserids(
				authDb, Group.Type.USER_ACCESS));
		UserCache.UserFilter subjectFilter = (subject) -> {
			if (!accessUserids.contains(subject.getUserid()))
				return false;
			if (subject.getRole().ordinal() < user.getRole().ordinal())
				return false;
			if (!includeInactive && !subject.isActive())
				return false;
			return true;
		};
		UserCache cache = UserCache.getInstance();
		return cache.getUsers(subjectFilter, Comparator.comparing(
				User::getEmail));
	}

	/**
	 * Returns all users that occur in the specified project and that can be
	 * accessed by the specified user. It is possible to filter by role and
	 * active status.
	 * 
	 * @param project the project code
	 * @param authDb the authentication database
	 * @param user the user for whom accessible project users should be
	 * retrieved
	 * @param role if set, it only returns users that were added to the project
	 * with the specified role. This can be set to null.
	 * @param includeInactive true if inactive subjects should be included,
	 * false if only active subjects should be returned
	 * @return the user IDs of the subjects
	 * @throws DatabaseException if a database error occurs
	 */
	public static List<User> findProjectUsers(String project, Database authDb,
			User user, Role role, boolean includeInactive)
			throws DatabaseException {
		List<User> allProjectUsers = null;
		if (user.getRole() == Role.ADMIN ||
				user.getRole() == Role.PROFESSIONAL) {
			allProjectUsers = findUsersInProject(authDb, project, role,
					includeInactive);
		}
		if (user.getRole() == Role.ADMIN) {
			return allProjectUsers;
		} else if (user.getRole() == Role.PROFESSIONAL) {
			List<User> result = new ArrayList<>();
			Set<String> allowedUsers = new HashSet<>(user.findGroupUserids(
					authDb, Group.Type.USER_ACCESS));
			allowedUsers.add(user.getUserid());
			ProjectUserAccessControlRepository uacRepo = AppComponents.get(
					ProjectUserAccessControlRepository.class);
			ProjectUserAccessControl projectAccess = uacRepo.get(project);
			if (projectAccess != null) {
				List<User> accessUsers = projectAccess.findAccessibleUsers(
						authDb, user, allProjectUsers);
				for (User accessUser : accessUsers) {
					allowedUsers.add(accessUser.getUserid());
				}
			}
			for (User projectUser : allProjectUsers) {
				if (allowedUsers.contains(projectUser.getUserid()))
					result.add(projectUser);
			}
			return result;
		} else {
			// user is patient
			List<User> result = new ArrayList<>();
			if (isProjectUser(authDb, project, user.getUserid(), role))
				result.add(user);
			return result;
		}
	}

	/**
	 * Returns whether the specified user belongs to the specified project. If
	 * a role is specified, it also checks whether the user was added to the
	 * project with that role.
	 * 
	 * @param authDb the authentication database
	 * @param project the project code
	 * @param user the user ID
	 * @param role the role or null
	 * @return true if the user belongs to the specified project
	 * @throws DatabaseException if a database error occurs
	 */
	public static boolean isProjectUser(Database authDb, String project,
			String user, Role role) throws DatabaseException {
		DatabaseCriteria criteria;
		if (role == null) {
			criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", user),
					new DatabaseCriteria.Equal("projectCode", project)
			);
		} else {
			criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", user),
					new DatabaseCriteria.Equal("projectCode", project),
					new DatabaseCriteria.Equal("asRole", role.toString())
			);
		}
		return authDb.count(new UserProjectTable(), criteria) != 0;
	}

	/**
	 * Returns all users that have been added to the specified project. This
	 * includes inactive users. If you specify a role, it will only return users
	 * that were added to the project with that role. If you only need the user
	 * IDs, then {@link #findUseridsInProject(Database, String, Role)
	 * findUseridsInProject()} is more efficient.
	 * 
	 * @param authDb the authentication database
	 * @param project the project code
	 * @param role the role or null
	 * @return the users
	 * @throws DatabaseException if a database error occurs
	 * @see #findUseridsInProject(Database, String, Role)
	 */
	public static List<User> findUsersInProject(Database authDb, String project,
			Role role) throws DatabaseException {
		return findUsersInProject(authDb, project, role, true);
	}
	
	/**
	 * Returns all users that have been added to the specified project. If you
	 * specify a role, it will only return users that were added to the project
	 * with that role. If you only need the user IDs, then {@link
	 * #findUseridsInProject(Database, String, Role) findUsernamesInProject()}
	 * is more efficient.
	 * 
	 * @param authDb the authentication database
	 * @param project the project code
	 * @param role the role or null
	 * @param includeInactive true if inactive subjects should be included, false
	 * if only active subjects should be returned
	 * @return the users
	 * @throws DatabaseException if a database error occurs
	 * @see #findUseridsInProject(Database, String, Role)
	 */
	public static List<User> findUsersInProject(Database authDb, String project,
			Role role, boolean includeInactive) throws DatabaseException {
		DatabaseCriteria criteria;
		if (role == null) {
			criteria = new DatabaseCriteria.Equal("projectCode", project);
		} else {
			criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("projectCode", project),
					new DatabaseCriteria.Equal("asRole", role.toString())
			);
		}
		List<UserProject> userProjects = authDb.select(new UserProjectTable(),
				criteria, 0, null);
		List<User> result = new ArrayList<>();
		Set<String> userids = new HashSet<>();
		UserCache userCache = UserCache.getInstance();
		for (UserProject userProject : userProjects) {
			String userid = userProject.getUser();
			if (userids.contains(userid))
				continue;
			User user = userCache.findByUserid(userid);
			if (user == null)
				continue;
			if (includeInactive || user.isActive()) {
				result.add(user);
				userids.add(userid);
			}
		}
		return result;
	}
	
	/**
	 * Returns the user IDs of all users that have been added to the specified
	 * project. This includes inactive users. If you specify a role, it will
	 * only return users that were added to the project with that role.
	 * 
	 * @param authDb the authentication database
	 * @param project the project code
	 * @param role the role or null
	 * @return the users
	 * @throws DatabaseException if a database error occurs
	 * @see #findUsersInProject(Database, String, Role)
	 */
	public static List<String> findUseridsInProject(Database authDb,
			String project, Role role) throws DatabaseException {
		DatabaseCriteria criteria;
		if (role == null) {
			criteria = new DatabaseCriteria.Equal("projectCode", project);
		} else {
			criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("projectCode", project),
					new DatabaseCriteria.Equal("asRole", role.toString())
			);
		}
		List<UserProject> userProjects = authDb.select(new UserProjectTable(),
				criteria, 0, null);
		List<String> result = new ArrayList<>();
		for (UserProject userProject : userProjects) {
			String user = userProject.getUser();
			if (!result.contains(user))
				result.add(user);
		}
		return result;
	}

	/**
	 * Finds the user with the specified user ID and returns their public
	 * profile. If the user does not exist, this method returns null.
	 *
	 * @param userid the user ID of the user to find
	 * @return the public profile of the user or null
	 */
	public static Map<String,Object> findPublicProfile(String userid) {
		UserCache userCache = UserCache.getInstance();
		User user = userCache.findByUserid(userid);
		if (user == null)
			return null;
		Map<String,Object> profile = new LinkedHashMap<>();
		profile.put("userid", user.getUserid());
		profile.put("email", user.getEmail());
		return profile;
	}

	/**
	 * Finds the user and accessible date range for the specified user ID or
	 * email address. It checks whether the user who is currently logged has
	 * access for the specified project data request. The returned user may be
	 * inactive.
	 *
	 * <p>If the protocol version is 6.0.0 later, you should specify a user ID.
	 * Otherwise you should specify an email address.</p>
	 *
	 * <p>All users can access themselves. Admins can access all users.
	 * Access is possible if a matching access rule is found. Otherwise patients
	 * can only access themselves. Professionals can access themselves and all
	 * users that occur in the same group as the professional.</p>
	 *
	 * @param version the protocol version
	 * @param subject the user ID or email address of the user to retrieve.
	 * This can be null or an empty string to get the specified "user".
	 * @param project the project code
	 * @param table the table name that is accessed
	 * @param requiredMode the required access mode
	 * @param authDb the authentication database
	 * @param user the user who is logged in
	 * @return the retrieved user and accessible date range
	 * @throws ForbiddenException if the user doesn't exist or can't be
	 * accessed
	 * @throws DatabaseException if a database error occurs
	 */
	public static ProjectUserAccess findAccessibleProjectUser(
			ProtocolVersion version, String subject, String project,
			String table, AccessMode requiredMode, Database authDb, User user)
			throws ForbiddenException, DatabaseException {
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal())
			return findAccessibleProjectUserByUserid(subject, project, table,
					requiredMode, authDb, user);
		else
			return findAccessibleProjectUserByEmail(subject, project, table,
					requiredMode, authDb, user);
	}

	/**
	 * Finds the user and accessible date range for the specified user ID. It
	 * checks whether the user who is currently logged has access for the
	 * specified project data request. The returned user may be inactive.
	 *
	 * <p>All users can access themselves. Admins can access all users.
	 * Access is possible if a matching access rule is found. Otherwise patients
	 * can only access themselves. Professionals can access themselves and all
	 * users that occur in the same group as the professional.</p>
	 *
	 * @param subject the user ID of the user to retrieve. This can be null or
	 * an empty string to get the specified "user".
	 * @param project the project code
	 * @param table the table name that is accessed
	 * @param requiredMode the required access mode
	 * @param authDb the authentication database
	 * @param user the user who is logged in
	 * @return the retrieved user and accessible date range
	 * @throws ForbiddenException if the user doesn't exist or can't be
	 * accessed
	 * @throws DatabaseException if a database error occurs
	 */
	public static ProjectUserAccess findAccessibleProjectUserByUserid(
			String subject, String project, String table,
			AccessMode requiredMode, Database authDb, User user)
			throws ForbiddenException, DatabaseException {
		User getUser;
		if (subject == null || subject.isEmpty() ||
				subject.equals(user.getUserid())) {
			getUser = user;
		} else {
			UserCache userCache = UserCache.getInstance();
			getUser = userCache.findByUserid(subject);
		}
		return checkAccessibleProjectUser(subject, project, table, requiredMode,
				getUser, authDb, user);
	}

	/**
	 * Finds the user and accessible date range for the specified email address.
	 * It checks whether the user who is currently logged has access for the
	 * specified project data request. The returned user may be inactive.
	 *
	 * <p>All users can access themselves. Admins can access all users.
	 * Access is possible if a matching access rule is found. Otherwise patients
	 * can only access themselves. Professionals can access themselves and all
	 * users that occur in the same group as the professional.</p>
	 *
	 * @param email the email address of the user to retrieve. This can be null
	 * or an empty string to get the specified "user".
	 * @param project the project code
	 * @param table the table name that is accessed
	 * @param requiredMode the required access mode
	 * @param authDb the authentication database
	 * @param user the user who is logged in
	 * @return the retrieved user and accessible date range
	 * @throws ForbiddenException if the user doesn't exist or can't be
	 * accessed
	 * @throws DatabaseException if a database error occurs
	 */
	public static ProjectUserAccess findAccessibleProjectUserByEmail(
			String email, String project, String table, AccessMode requiredMode,
			Database authDb, User user) throws ForbiddenException,
			DatabaseException {
		User getUser;
		if (email == null || email.isEmpty() ||
				email.toLowerCase().equals(user.getEmail())) {
			getUser = user;
		} else {
			UserCache userCache = UserCache.getInstance();
			getUser = userCache.findByEmail(email);
		}
		return checkAccessibleProjectUser(email, project, table, requiredMode,
				getUser, authDb, user);
	}

	private static ProjectUserAccess checkAccessibleProjectUser(String subject,
			String project, String table, AccessMode requiredMode, User getUser,
			Database authDb, User user) throws ForbiddenException,
			DatabaseException {
		if (getUser == null) {
			throw new ForbiddenException(String.format(
					"User %s not found or access forbidden", subject));
		}
		// admins can access all users
		// all users can access themselves
		if (user.getRole() == Role.ADMIN ||
				getUser.getUserid().equals(user.getUserid())) {
			return new ProjectUserAccess(getUser, null, null);
		}
		// check project user access
		ProjectUserAccessTable accessTable = new ProjectUserAccessTable();
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("project", project),
				new DatabaseCriteria.Equal("grantee", user.getUserid()),
				new DatabaseCriteria.Equal("subject", getUser.getUserid())
		);
		ProjectUserAccessRecord accessRecord = authDb.selectOne(accessTable,
				criteria, null);
		if (accessRecord != null) {
			LocalDate[] range = findAccessibleProjectUserRange(
					accessRecord.getAccessRuleObject(), project, table,
					requiredMode);
			if (range != null) {
				return new ProjectUserAccess(getUser, range[0], range[1]);
			}
		}
		// no project user access
		// patients can only access themselves
		if (user.getRole() == Role.PATIENT) {
			throw new ForbiddenException(String.format(
					"User %s not found or access forbidden", subject));
		}
		if (getUser.getRole() == Role.ADMIN) {
			throw new ForbiddenException(String.format(
					"User %s not found or access forbidden", subject));
		}
		// professionals can access all users in the same group
		List<String> accessibleUsers = user.findGroupUserids(authDb, null);
		if (accessibleUsers.contains(getUser.getUserid()))
			return new ProjectUserAccess(getUser, null, null);
		ProjectUserAccessControlRepository uacRepo = AppComponents.get(
				ProjectUserAccessControlRepository.class);
		ProjectUserAccessControl accessControl = uacRepo.get(project);
		if (accessControl != null && accessControl.isAccessibleUser(authDb,
				user, getUser)) {
			return new ProjectUserAccess(getUser, null, null);
		}
		throw new ForbiddenException(String.format(
				"User %s not found or access forbidden", subject));
	}

	/**
	 * Checks whether project user access is granted according to the specified
	 * rule and returns the accessible date range.
	 *
	 * @param accessRule the access rule (can be null if no restriction)
	 * @param projectCode the project code
	 * @param table the table name that is accessed
	 * @param requiredMode the required access mode
	 * @return the accessible date range (either date can be if the start date
	 * or end date is not restricted), or null if no access is available
	 */
	private static LocalDate[] findAccessibleProjectUserRange(
			ProjectUserAccessRule accessRule, String projectCode, String table,
			AccessMode requiredMode) {
		LocalDate[] range = new LocalDate[] { null, null };
		if (accessRule.getAccessRestriction() == null) {
			// no access restriction, full access
			return range;
		}
		ProjectRepository projects = AppComponents.get(ProjectRepository.class);
		BaseProject project = projects.findProjectByCode(projectCode);
		Set<String> modules = project.findModulesForTable(table);
		if (modules.isEmpty())
			return null;
		boolean foundMatch = false;
		for (ProjectUserAccessRestriction restrict :
				accessRule.getAccessRestriction()) {
			if (!matchesAccessRestriction(restrict, modules, requiredMode))
				continue;
			if (!foundMatch) {
				foundMatch = true;
				range[0] = restrict.getStart();
				range[1] = restrict.getEnd();
			} else {
				if (restrict.getStart() == null) {
					range[0] = null;
				} else if (range[0] != null &&
						restrict.getStart().isBefore(range[0])) {
					range[0] = restrict.getStart();
				}
				if (restrict.getEnd() == null) {
					range[1] = null;
				} else if (range[1] != null &&
						restrict.getEnd().isAfter(range[1])) {
					range[1] = restrict.getEnd();
				}
			}
		}
		if (foundMatch)
			return range;
		else
			return null;
	}

	/**
	 * Checks whether the specified access restriction matches the specified
	 * request.
	 *
	 * @param restrict the access restriction
	 * @param modules the acceptable modules. At least one module should match.
	 * @param requiredMode the required access mode
	 * @return true if the access restriction matches, false otherwise
	 */
	private static boolean matchesAccessRestriction(
			ProjectUserAccessRestriction restrict, Set<String> modules,
			AccessMode requiredMode) {
		if (!modules.contains(restrict.getModule()))
			return false;
		if (!restrict.getAccessMode().matchesRequest(requiredMode))
			return false;
		return true;
	}
}
