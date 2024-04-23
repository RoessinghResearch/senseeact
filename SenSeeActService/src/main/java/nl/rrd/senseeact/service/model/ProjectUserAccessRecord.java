package nl.rrd.senseeact.service.model;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;
import nl.rrd.senseeact.client.model.ProjectUserAccessRule;
import nl.rrd.senseeact.dao.BaseDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;

public class ProjectUserAccessRecord extends BaseDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING)
	private String project;
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String grantee;
	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String subject;
	@DatabaseField(value=DatabaseType.TEXT)
	private String accessRule;
	private ProjectUserAccessRule accessRuleObject;

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getGrantee() {
		return grantee;
	}

	public void setGrantee(String grantee) {
		this.grantee = grantee;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * Returns the JSON code for the access rule. This method is used for the
	 * DAO. Users can call {@link #getAccessRuleObject() getAccessRuleObject()}.
	 *
	 * @return the JSON code for the access rule
	 */
	public String getAccessRule() {
		return JsonMapper.generate(accessRuleObject);
	}

	/**
	 * Sets the JSON code for the access rule. This method is used for the DAO.
	 * Users can call {@link #setAccessRuleObject(ProjectUserAccessRule)
	 * setAccessRuleObject()}.
	 *
	 * @param accessRule the JSON code for the access rule
	 * @throws ParseException if the JSON code is invalid
	 */
	public void setAccessRule(String accessRule) throws ParseException {
		accessRuleObject = JsonMapper.parse(accessRule,
				ProjectUserAccessRule.class);
	}

	public ProjectUserAccessRule getAccessRuleObject() {
		return accessRuleObject;
	}

	public void setAccessRuleObject(ProjectUserAccessRule accessRuleObject) {
		this.accessRuleObject = accessRuleObject;
	}
}
