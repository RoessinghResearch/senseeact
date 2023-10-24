package nl.rrd.senseeact.client.model;

import nl.rrd.utils.json.JsonObject;

/**
 * Short version of {@link User User}.
 * 
 * @author Dennis Hofs (RRD)
 */
public class ShortUserProfile extends JsonObject {
	private String userid;
	private Role role;
	private Gender gender;
	private String title;
	private String initials;
	private String firstName;
	private String prefixes;
	private String lastName;
	
	/**
	 * Constructs a new empty instance. This is used for JSON serialization.
	 * Users should not call this.
	 */
	public ShortUserProfile() {
	}
	
	/**
	 * Constructs a new short user profile from the specified User object.
	 * 
	 * @param user the User object
	 */
	public ShortUserProfile(User user) {
		this.userid = user.getUserid();
		this.role = user.getRole();
		this.gender = user.getGender();
		this.title = user.getTitle();
		this.initials = user.getInitials();
		this.firstName = user.getFirstName();
		this.prefixes = user.getPrefixes();
		this.lastName = user.getLastName();
	}

	/**
	 * Returns the user ID. This is a required field. The user ID can be a UUID
	 * or an email address (for legacy users). This field identifies the user
	 * throughout the database.
	 *
	 * @return the user ID
	 */
	public String getUserid() {
		return userid;
	}

	/**
	 * Sets the user ID. This is a required field. The user ID can be a UUID or
	 * an email address (for legacy users). This field identifies the user
	 * throughout the database.
	 *
	 * @param userid the user ID
	 */
	public void setUserid(String userid) {
		this.userid = userid;
	}

	/**
	 * Returns the role. This is a required field.
	 * 
	 * @return the role
	 */
	public Role getRole() {
		return role;
	}

	/**
	 * Sets the role. This is a required field.
	 * 
	 * @param role the role
	 */
	public void setRole(Role role) {
		this.role = role;
	}

	/**
	 * Returns the gender.
	 * 
	 * @return the gender or null
	 */
	public Gender getGender() {
		return gender;
	}

	/**
	 * Sets the gender.
	 * 
	 * @param gender the gender or null
	 */
	public void setGender(Gender gender) {
		this.gender = gender;
	}

	/**
	 * Returns the title.
	 * 
	 * @return the title or null
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 * 
	 * @param title the title or null
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the initials of the first names formatted as A.B.C.
	 * 
	 * @return the initials or null
	 */
	public String getInitials() {
		return initials;
	}

	/**
	 * Sets the initials of the first names formatted as A.B.C.
	 * 
	 * @param initials the initials or null
	 */
	public void setInitials(String initials) {
		this.initials = initials;
	}

	/**
	 * Returns the first name. This should be the familiar first name used to
	 * address the person in friendly language.
	 * 
	 * @return the first name or null
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * Sets the first name. This should be the familiar first name used to
	 * address the person in friendly language.
	 * 
	 * @param firstName the first name or null
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * Returns the prefixes for the last name. Languages such as Dutch have
	 * prefixes for last names that should be ignored when sorting.
	 * 
	 * @return the prefixes or null
	 */
	public String getPrefixes() {
		return prefixes;
	}

	/**
	 * Sets the prefixes for the last name. Languages such as Dutch have
	 * prefixes for last names that should be ignored when sorting.
	 * 
	 * @param prefixes the prefixes or null
	 */
	public void setPrefixes(String prefixes) {
		this.prefixes = prefixes;
	}

	/**
	 * Returns the last name. This should be the familiar last name used to
	 * address the person in friendly language.
	 * 
	 * @return the last name or null
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * Sets the last name. This should be the familiar last name used to
	 * address the person in friendly language.
	 * 
	 * @param lastName the last name or null
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}
