package nl.rrd.senseeact.client.model;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.utils.json.JsonObject;

public class Group extends JsonObject {
	private String name;
	private List<ShortUserProfile> members = new ArrayList<>();
	
	public Group() {
	}
	
	public Group(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ShortUserProfile> getMembers() {
		return members;
	}

	public void setMembers(List<ShortUserProfile> members) {
		this.members = members;
	}
	
	public void addMember(ShortUserProfile member) {
		members.add(member);
	}
}
