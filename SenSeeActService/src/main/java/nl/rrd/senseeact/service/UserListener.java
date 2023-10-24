package nl.rrd.senseeact.service;

import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.service.model.User;

public interface UserListener {
	void userProfileUpdated(User user, User oldProfile);
	void userRoleChanged(User user, Role oldRole);
	void userActiveChanged(User user);
	void userAddedToProject(User user, String project, Role role);
	void userRemovedFromProject(User user, String project, Role role);
	void userAddedAsSubject(User user, User profUser);
	void userRemovedAsSubject(User user, User profUser);
}
