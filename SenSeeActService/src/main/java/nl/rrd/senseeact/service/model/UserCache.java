package nl.rrd.senseeact.service.model;

import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.service.ProtocolVersion;

import java.util.*;

public class UserCache {
	private static final Object LOCK = new Object();

	private static UserCache instance = null;

	private List<User> users = new ArrayList<>();
	private Map<String,User> useridMap = new LinkedHashMap<>();
	private Map<String,User> emailMap = new LinkedHashMap<>();
	private Map<String,List<User>> emailLocalMap = new LinkedHashMap<>();

	private UserCache(Database authDb) throws DatabaseException {
		List<User> users = authDb.select(new UserTable(), null, 0, null);
		for (User user : users) {
			addUser(user);
		}
	}

	public static UserCache createInstance(Database authDb)
			throws DatabaseException {
		synchronized (LOCK) {
			if (instance != null)
				return instance;
			instance = new UserCache(authDb);
			return instance;
		}
	}

	public static UserCache getInstance() {
		return instance;
	}

	/**
	 * Finds the user with the specified user ID or email address. If the
	 * protocol version is 6.0.0 later, you should specify a user ID. Otherwise
	 * you should specify an email address.
	 *
	 * <p>If the user does not exist, this method returns null. The returned
	 * user may be inactive. Note that a user may not be allowed to access
	 * another user. See also {@link
	 * User#findAccessibleUser(ProtocolVersion, String, Database, User)
	 * User.findAccessibleUser()}.
	 *
	 * @param version the protocol version
	 * @param user the user ID or email address
	 * @return the user or null
	 */
	public User find(ProtocolVersion version, String user) {
		if (version.ordinal() >= ProtocolVersion.V6_0_0.ordinal())
			return findByUserid(user);
		else
			return findByEmail(user);
	}

	/**
	 * Finds the user with the specified user ID. If the user does not exist,
	 * this method returns null. The returned user may be inactive. Note that a
	 * user may not be allowed to access another user. See also {@link
	 * User#findAccessibleUser(ProtocolVersion, String, Database, User)
	 * User.findAccessibleUser()}.
	 *
	 * @param userid the user ID of the user to find
	 * @return the user or null
	 */
	public User findByUserid(String userid) {
		synchronized (LOCK) {
			User user = useridMap.get(userid);
			if (user == null)
				return null;
			else
				return new User(user);
		}
	}

	/**
	 * Finds the user with the specified email address. If the user does not
	 * exist, this method returns null. The returned user may be inactive. Note
	 * that a user may not be allowed to access another user. See also {@link
	 * User#findAccessibleUser(ProtocolVersion, String, Database, User)
	 * User.findAccessibleUser()}.
	 *
	 * @param email the email address of the user to find
	 * @return the user or null
	 */
	public User findByEmail(String email) {
		synchronized (LOCK) {
			User user = emailMap.get(email.toLowerCase());
			if (user == null)
				return null;
			else
				return new User(user);
		}
	}

	/**
	 * Returns whether the specified email address already exists, either in the
	 * "email" field or the "emailPendingVerification" field of a user.
	 *
	 * @param email the email address
	 * @return true if the email address already exists, false otherwise
	 */
	public boolean emailExists(String email) {
		synchronized (LOCK) {
			String emailLower = email.toLowerCase();
			for (User user : users) {
				if (emailLower.equals(user.getEmail()) ||
						emailLower.equals(user.getEmailPendingVerification())) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Finds the users with the specified local part of the email address. It
	 * can return zero or more users. It includes inactive users.
	 *
	 * @param emailLocal the local part of the email address
	 * @return the users
	 */
	public List<User> findByEmailLocal(String emailLocal) {
		synchronized (LOCK) {
			List<User> users = emailLocalMap.get(emailLocal.toLowerCase());
			List<User> result = new ArrayList<>();
			if (users == null)
				return result;
			for (User user : users) {
				result.add(new User(user));
			}
			return result;
		}
	}

	public List<User> getUsers(UserFilter filter, Comparator<User> sort) {
		List<User> result = new ArrayList<>();
		synchronized (LOCK) {
			for (User user : users) {
				if (filter == null || filter.matches(user))
					result.add(new User(user));
			}
		}
		if (sort != null)
			result.sort(sort);
		return result;
	}

	public void createUser(Database authDb, User user)
			throws DatabaseException {
		authDb.insert(UserTable.NAME, user);
		addUser(new User(user));
	}

	public void updateUser(Database authDb, User user)
			throws DatabaseException {
		authDb.update(UserTable.NAME, user);
		updateUser(new User(user));
	}

	public void deleteUser(Database authDb, String userid)
			throws DatabaseException {
		DatabaseCriteria criteria = new DatabaseCriteria.Equal("userid",
				userid);
		authDb.delete(new UserTable(), criteria);
		deleteUser(userid);
	}

	public int getCount() {
		synchronized (LOCK) {
			return users.size();
		}
	}

	private void addUser(User user) {
		synchronized (LOCK) {
			users.add(user);
			useridMap.put(user.getUserid(), user);
			String email = user.getEmail();
			int sep = email.indexOf('@');
			String emailLocal = email.substring(0, sep);
			emailMap.put(email, user);
			List<User> emailLocalUsers = emailLocalMap.get(emailLocal);
			if (emailLocalUsers == null) {
				emailLocalUsers = new ArrayList<>();
				emailLocalMap.put(emailLocal, emailLocalUsers);
			}
			emailLocalUsers.add(user);
		}
	}

	private void updateUser(User user) {
		synchronized (LOCK) {
			User prevUser = useridMap.get(user.getUserid());
			int index = users.indexOf(prevUser);
			users.remove(index);
			users.add(index, user);
			useridMap.put(user.getUserid(), user);
			String prevEmail = prevUser.getEmail();
			int sep = prevEmail.indexOf('@');
			String prevEmailLocal = prevEmail.substring(0, sep);
			String email = user.getEmail();
			sep = email.indexOf('@');
			String emailLocal = email.substring(0, sep);
			if (!email.equals(prevEmail))
				emailMap.remove(prevEmail);
			emailMap.put(email, user);
			if (emailLocal.equals(prevEmailLocal)) {
				List<User> emailLocalUsers = emailLocalMap.get(emailLocal);
				index = emailLocalUsers.indexOf(prevUser);
				emailLocalUsers.remove(index);
				emailLocalUsers.add(index, user);
			} else {
				List<User> prevEmailLocalUsers = emailLocalMap.get(
						prevEmailLocal);
				prevEmailLocalUsers.remove(prevUser);
				if (prevEmailLocalUsers.isEmpty())
					emailLocalMap.remove(prevEmailLocal);
				List<User> emailLocalUsers = emailLocalMap.computeIfAbsent(
						emailLocal, key -> new ArrayList<>());
				emailLocalUsers.add(user);
			}
		}
	}

	private void deleteUser(String userid) {
		synchronized (LOCK) {
			User user = useridMap.get(userid);
			if (user == null)
				return;
			users.remove(user);
			useridMap.remove(userid);
			String email = user.getEmail();
			int sep = email.indexOf('@');
			String emailLocal = email.substring(0, sep);
			emailMap.remove(email);
			List<User> emailLocalUsers = emailLocalMap.get(emailLocal);
			emailLocalUsers.remove(user);
			if (emailLocalUsers.isEmpty())
				emailLocalMap.remove(emailLocal);
		}
	}

	public interface UserFilter {
		boolean matches(User user);
	}
}
