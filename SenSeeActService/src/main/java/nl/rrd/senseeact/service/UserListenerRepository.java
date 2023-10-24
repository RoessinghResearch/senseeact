package nl.rrd.senseeact.service;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.service.model.User;

public class UserListenerRepository {
	private static final Object LOCK = new Object();
	private static UserListenerRepository instance = null;
	
	private List<UserListener> listeners = new ArrayList<>();
	
	private UserListenerRepository() {
	}
	
	public static UserListenerRepository getInstance() {
		synchronized (LOCK) {
			if (instance == null)
				instance = new UserListenerRepository();
			return instance;
		}
	}
	
	public List<UserListener> getUserListeners() {
		synchronized (LOCK) {
			return new ArrayList<>(listeners);
		}
	}
	
	public void addUserListener(UserListener listener) {
		synchronized (LOCK) {
			listeners.add(listener);
		}
	}
	
	public void removeUserListener(UserListener listener) {
		synchronized (LOCK) {
			listeners.remove(listener);
		}
	}
	
	public void notifyUserProfileUpdated(User user, User oldProfile) {
		List<UserListener> listeners;
		synchronized (LOCK) {
			listeners = new ArrayList<>(this.listeners);
		}
		for (UserListener listener : listeners) {
			listener.userProfileUpdated(user, oldProfile);
		}
	}

	public void notifyUserRoleChanged(User user, Role oldRole) {
		List<UserListener> listeners;
		synchronized (LOCK) {
			listeners = new ArrayList<>(this.listeners);
		}
		for (UserListener listener : listeners) {
			listener.userRoleChanged(user, oldRole);
		}
	}
	
	public void notifyUserActiveChanged(User user) {
		List<UserListener> listeners;
		synchronized (LOCK) {
			listeners = new ArrayList<>(this.listeners);
		}
		for (UserListener listener : listeners) {
			listener.userActiveChanged(user);
		}
	}

	public void notifyUserAddedToProject(User user, String project, Role role) {
		List<UserListener> listeners;
		synchronized (LOCK) {
			listeners = new ArrayList<>(this.listeners);
		}
		for (UserListener listener : listeners) {
			listener.userAddedToProject(user, project, role);
		}
	}

	public void notifyUserRemovedFromProject(User user, String project,
			Role role) {
		List<UserListener> listeners;
		synchronized (LOCK) {
			listeners = new ArrayList<>(this.listeners);
		}
		for (UserListener listener : listeners) {
			listener.userRemovedFromProject(user, project, role);
		}
	}

	public void notifyUserAddedAsSubject(User user, User profUser) {
		List<UserListener> listeners;
		synchronized (LOCK) {
			listeners = new ArrayList<>(this.listeners);
		}
		for (UserListener listener : listeners) {
			listener.userAddedAsSubject(user, profUser);
		}
	}

	public void notifyUserRemovedAsSubject(User user, User profUser) {
		List<UserListener> listeners;
		synchronized (LOCK) {
			listeners = new ArrayList<>(this.listeners);
		}
		for (UserListener listener : listeners) {
			listener.userRemovedAsSubject(user, profUser);
		}
	}
}
