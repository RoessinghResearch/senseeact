package nl.rrd.senseeact.client.model.compat;

import nl.rrd.utils.json.JsonObject;
import nl.rrd.senseeact.client.model.Group;
import nl.rrd.senseeact.client.model.User;

import java.util.ArrayList;
import java.util.List;

public class GroupV1 extends JsonObject {
	private String name;
	private List<ShortUserProfileV1> members = new ArrayList<>();

	public GroupV1() {
	}

	public GroupV1(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ShortUserProfileV1> getMembers() {
		return members;
	}

	public void setMembers(List<ShortUserProfileV1> members) {
		this.members = members;
	}
	
	public void addMember(ShortUserProfileV1 member) {
		members.add(member);
	}

	public static GroupV1 fromGroup(Group group, List<? extends User> members) {
		GroupV1 result = new GroupV1();
		result.name = group.getName();
		for (User member : members) {
			result.members.add(ShortUserProfileV1.fromUser(member));
		}
		return result;
	}
}
