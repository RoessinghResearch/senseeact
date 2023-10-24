package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import nl.rrd.utils.json.JsonObject;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ProjectUserAccessRule extends JsonObject {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Map<String,Object> grantee = null;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Map<String,Object> subject = null;
	private List<ProjectUserAccessRestriction> accessRestriction = null;

	/**
	 * Returns the public profile of the grantee. This is only set when you
	 * query for the access rules of a specified subject. The result is a map
	 * with selected fields from the {@link User User} object. It should include
	 * at least the fields "userid" and "emailVerified".
	 *
	 * @return the grantee or null
	 */
	public Map<String, Object> getGrantee() {
		return grantee;
	}

	/**
	 * Sets the public profile of the grantee. This should only be set at a
	 * query for the access rules of a specified subject. It is a map with
	 * selected fields from the {@link User User} object. It should include at
	 * least the fields "userid" and "emailVerified".
	 *
	 * @param grantee the grantee or null
	 */
	public void setGrantee(Map<String, Object> grantee) {
		this.grantee = grantee;
	}

	/**
	 * Returns the public profile of the subject. This is only set when you
	 * query for the access rules of a specified grantee. The result is a map
	 * with selected fields from the {@link User User} object. It should include
	 * at least the field "userid".
	 *
	 * @return the subject or null
	 */
	public Map<String, Object> getSubject() {
		return subject;
	}

	/**
	 * Sets the public profile of the subject. This should only be set at a
	 * query for the access rules of a specified grantee. It is is a map with
	 * selected fields from the {@link User User} object. It should include
	 * at least the field "userid".
	 *
	 * @param subject the subject or null
	 */
	public void setSubject(Map<String, Object> subject) {
		this.subject = subject;
	}

	/**
	 * Returns the access restriction. If the grantee has full access to the
	 * subject's data, then this method returns null.
	 *
	 * @return the access restriction or null
	 */
	public List<ProjectUserAccessRestriction> getAccessRestriction() {
		return accessRestriction;
	}

	/**
	 * Sets the access restriction. If the grantee has full access to the
	 * subject's data, then this should be null.
	 *
	 * @param accessRestriction the access restriction or null
	 */
	public void setAccessRestriction(
			List<ProjectUserAccessRestriction> accessRestriction) {
		this.accessRestriction = accessRestriction;
	}
}
