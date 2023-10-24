package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ListUser {
	private String userid;
	private String email;
	private Role role;
	private boolean active;

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public static ListUser fromUser(User user) {
		ListUser result = new ListUser();
		result.userid = user.getUserid();
		result.email = user.getEmail();
		result.role = user.getRole();
		result.active = user.isActive();
		return result;
	}
}
