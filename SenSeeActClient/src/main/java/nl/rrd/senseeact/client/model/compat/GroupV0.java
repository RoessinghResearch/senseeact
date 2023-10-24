package nl.rrd.senseeact.client.model.compat;

import nl.rrd.utils.json.JsonObject;
import nl.rrd.senseeact.client.model.Group;
import nl.rrd.senseeact.client.model.User;

import java.util.ArrayList;
import java.util.List;

public class GroupV0 extends JsonObject {
	private String name;
	private List<ShortUserProfileV0> members = new ArrayList<>();

	public GroupV0() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ShortUserProfileV0> getMembers() {
		return members;
	}

	public void setMembers(List<ShortUserProfileV0> members) {
		this.members = members;
	}
	
	public void addMember(ShortUserProfileV0 member) {
		members.add(member);
	}

	public static GroupV0 fromGroup(Group group, List<? extends User> members) {
		GroupV0 result = new GroupV0();
		result.name = group.getName();
		for (User member : members) {
			result.members.add(ShortUserProfileV0.fromUser(member));
		}
		return result;
	}
}
