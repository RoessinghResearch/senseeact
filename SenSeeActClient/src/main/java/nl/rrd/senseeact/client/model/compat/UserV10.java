package nl.rrd.senseeact.client.model.compat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.senseeact.client.model.Gender;
import nl.rrd.senseeact.client.model.MaritalStatus;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.senseeact.dao.BaseDatabaseObject;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseObject;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.utils.json.SqlDateDeserializer;
import nl.rrd.utils.json.SqlDateSerializer;
import nl.rrd.utils.validation.ValidateEmail;
import nl.rrd.utils.validation.ValidateNotNull;
import nl.rrd.utils.validation.ValidateTimeZone;

import java.time.LocalDate;

public class UserV10 extends BaseDatabaseObject {
	@JsonIgnore
	private String id;

	@DatabaseField(value=DatabaseType.STRING, index=true)
	@ValidateNotNull
	private String userid;

	@DatabaseField(value=DatabaseType.STRING, index=true)
	@ValidateEmail
	@ValidateNotNull
	private String email;

	@DatabaseField(value=DatabaseType.BYTE)
	private boolean emailVerified = false;

	@DatabaseField(value=DatabaseType.STRING)
	@ValidateEmail
	private String emailPendingVerification;

	@DatabaseField(value=DatabaseType.BYTE)
	private boolean hasTemporaryEmail = false;

	@DatabaseField(value=DatabaseType.BYTE)
	private boolean hasTemporaryPassword = false;

	@DatabaseField(value=DatabaseType.STRING)
	private Role role;

	@DatabaseField(value=DatabaseType.BYTE)
	private boolean active = true;

	@DatabaseField(value=DatabaseType.STRING)
	private Gender gender;

	@DatabaseField(value=DatabaseType.STRING)
	private MaritalStatus maritalStatus;

	@DatabaseField(value=DatabaseType.STRING)
	private String title;

	@DatabaseField(value=DatabaseType.STRING)
	private String initials;

	@DatabaseField(value=DatabaseType.STRING)
	private String firstName;

	@DatabaseField(value=DatabaseType.STRING)
	private String officialFirstNames;

	@DatabaseField(value=DatabaseType.STRING)
	private String prefixes;

	@DatabaseField(value=DatabaseType.STRING)
	private String lastName;

	@DatabaseField(value=DatabaseType.STRING)
	private String officialLastNames;

	@DatabaseField(value=DatabaseType.STRING)
	private String fullName;

	@DatabaseField(value=DatabaseType.STRING)
	private String nickName;
	
	@DatabaseField(value=DatabaseType.STRING)
	@ValidateEmail
	private String altEmail;
	
	@DatabaseField(value=DatabaseType.DATE)
	@JsonSerialize(using=SqlDateSerializer.class)
	@JsonDeserialize(using=SqlDateDeserializer.class)
	private LocalDate birthDate;
	
	@DatabaseField(value=DatabaseType.DATE)
	@JsonSerialize(using=SqlDateSerializer.class)
	@JsonDeserialize(using=SqlDateDeserializer.class)
	private LocalDate deceasedDate;
	
	@DatabaseField(value=DatabaseType.STRING)
	private String idNumber;
	
	@DatabaseField(value=DatabaseType.STRING)
	private String landlinePhone;

	@DatabaseField(value=DatabaseType.STRING)
	private String mobilePhone;

	@DatabaseField(value=DatabaseType.STRING)
	private String street;

	@DatabaseField(value=DatabaseType.STRING)
	private String streetNumber;

	@DatabaseField(value=DatabaseType.STRING)
	private String addressExtra;

	@DatabaseField(value=DatabaseType.STRING)
	private String postalCode;

	@DatabaseField(value=DatabaseType.STRING)
	private String town;

	@DatabaseField(value=DatabaseType.STRING)
	private String departmentCode;

	@DatabaseField(value=DatabaseType.TEXT)
	private String extraInfo;

	@DatabaseField(value=DatabaseType.STRING)
	private String localeCode;

	@DatabaseField(value=DatabaseType.STRING)
	private String languageFormality;

	@DatabaseField(value=DatabaseType.STRING)
	@ValidateTimeZone
	private String timeZone;

	@DatabaseField(value=DatabaseType.STRING)
	private String status;

	@Override
	public void copyFrom(DatabaseObject other) {
		if (!(other instanceof UserV10)) {
			super.copyFrom(other);
			return;
		}
		UserV10 otherUser = (UserV10)other;
		id = otherUser.id;
		userid = otherUser.userid;
		email = otherUser.email;
		emailVerified = otherUser.emailVerified;
		emailPendingVerification = otherUser.emailPendingVerification;
		hasTemporaryEmail = otherUser.hasTemporaryEmail;
		hasTemporaryPassword = otherUser.hasTemporaryPassword;
		role = otherUser.role;
		active = otherUser.active;
		gender = otherUser.gender;
		maritalStatus = otherUser.maritalStatus;
		title = otherUser.title;
		initials = otherUser.initials;
		firstName = otherUser.firstName;
		officialFirstNames = otherUser.officialFirstNames;
		prefixes = otherUser.prefixes;
		lastName = otherUser.lastName;
		officialLastNames = otherUser.officialLastNames;
		fullName = otherUser.fullName;
		nickName = otherUser.nickName;
		altEmail = otherUser.altEmail;
		birthDate = otherUser.birthDate;
		deceasedDate = otherUser.deceasedDate;
		idNumber = otherUser.idNumber;
		landlinePhone = otherUser.landlinePhone;
		mobilePhone = otherUser.mobilePhone;
		street = otherUser.street;
		streetNumber = otherUser.streetNumber;
		addressExtra = otherUser.addressExtra;
		postalCode = otherUser.postalCode;
		town = otherUser.town;
		departmentCode = otherUser.departmentCode;
		extraInfo = otherUser.extraInfo;
		localeCode = otherUser.localeCode;
		languageFormality = otherUser.languageFormality;
		timeZone = otherUser.timeZone;
		status = otherUser.status;
	}
	
	/**
	 * Returns the ID. This is not defined on the client side. Users are
	 * identified by their email address.
	 * 
	 * @return the ID
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * Sets the ID. This is not defined on the client side. Users are
	 * identified by their email address.
	 * 
	 * @param id the ID
	 */
	@Override
	public void setId(String id) {
		this.id = id;
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
	 * Returns the email address. This is a required field.
	 * 
	 * @return the email address
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the email address. This is a required field.
	 * 
	 * @param email the email address
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Returns whether the email address has been verified.
	 *
	 * @return true if the email address has been verified, false otherwise
	 */
	public boolean isEmailVerified() {
		return emailVerified;
	}

	/**
	 * Sets whether the email address has been verified.
	 *
	 * @param emailVerified true if the email address has been verified, false
	 * otherwise
	 */
	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	/**
	 * This field is set if a user with a verified email address tries to change
	 * their address. In that case it will be set in this field and an email is
	 * sent to verify this new email address. Only when that is confirmed, this
	 * address will be moved to field "email".
	 *
	 * @return an email address pending verification or null
	 */
	public String getEmailPendingVerification() {
		return emailPendingVerification;
	}

	/**
	 * This field is set if a user with a verified email address tries to change
	 * their address. In that case it will be set in this field and an email is
	 * sent to verify this new email address. Only when that is confirmed, this
	 * address will be moved to field "email".
	 *
	 * @param emailPendingVerification an email address pending verification or
	 * null
	 */
	public void setEmailPendingVerification(String emailPendingVerification) {
		this.emailPendingVerification = emailPendingVerification;
	}

	/**
	 * Returns whether the user has a temporary email address. This is true if
	 * the user signed up as a temporary user and did not change the email
	 * address yet.
	 *
	 * @return true if the user has a temporary email address, false otherwise
	 */
	public boolean isHasTemporaryEmail() {
		return hasTemporaryEmail;
	}

	/**
	 * Sets whether the user has a temporary email address. This is true if the
	 * user signed up as a temporary user and did not change the email address
	 * yet.
	 *
	 * @param hasTemporaryEmail true if the user has a temporary email address,
	 * false otherwise
	 */
	public void setHasTemporaryEmail(boolean hasTemporaryEmail) {
		this.hasTemporaryEmail = hasTemporaryEmail;
	}

	/**
	 * Returns whether the user has a temporary password. This is true if the
	 * user signed up as a temporary user and did not change the password yet.
	 *
	 * @return true if the user has a temporary password, false otherwise
	 */
	public boolean isHasTemporaryPassword() {
		return hasTemporaryPassword;
	}

	/**
	 * Sets whether the user has a temporary password. This is true if the user
	 * signed up as a temporary user and did not change the password yet.
	 *
	 * @param hasTemporaryPassword true if the user has a temporary password,
	 * false otherwise
	 */
	public void setHasTemporaryPassword(boolean hasTemporaryPassword) {
		this.hasTemporaryPassword = hasTemporaryPassword;
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
	 * Returns whether the user is active. An inactive user cannot log in and
	 * is excluded from system services. The default is true.
	 * 
	 * @return true if the user is active, false if the user is inactive
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Sets whether the user is active. An inactive user cannot log in and is
	 * excluded from system services. The default is true.
	 * 
	 * @param active true if the user is active, false if the user is inactive
	 */
	public void setActive(boolean active) {
		this.active = active;
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
	 * Returns the marital status.
	 * 
	 * @return the marital status or null
	 */
	public MaritalStatus getMaritalStatus() {
		return maritalStatus;
	}

	/**
	 * Sets the marital status.
	 * 
	 * @param maritalStatus the marital status or null
	 */
	public void setMaritalStatus(MaritalStatus maritalStatus) {
		this.maritalStatus = maritalStatus;
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
	 * Returns the official first names.
	 * 
	 * @return the official first names or null
	 */
	public String getOfficialFirstNames() {
		return officialFirstNames;
	}

	/**
	 * Sets the official first names.
	 * 
	 * @param officialFirstNames the official first names or null
	 */
	public void setOfficialFirstNames(String officialFirstNames) {
		this.officialFirstNames = officialFirstNames;
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

	/**
	 * Returns the official last names.
	 * 
	 * @return the official last names or null
	 */
	public String getOfficialLastNames() {
		return officialLastNames;
	}

	/**
	 * Sets the official last names.
	 * 
	 * @param officialLastNames the official last names or null
	 */
	public void setOfficialLastNames(String officialLastNames) {
		this.officialLastNames = officialLastNames;
	}

	/**
	 * Returns the full name. This field can be used for applications that do
	 * not separate first name and last name.
	 *
	 * @return the full name or null
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * Sets the full name. This field can be used for applications that do not
	 * separate first name and last name.
	 *
	 * @param fullName the full name or null
	 */
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	/**
	 * Returns the nickname.
	 *
	 * @return the nickname or null
	 */
	public String getNickName() {
		return nickName;
	}

	/**
	 * Sets the nickname.
	 *
	 * @param nickName the nickname or null
	 */
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	/**
	 * Returns the alternative email address.
	 * 
	 * @return the alternative email address or null
	 */
	public String getAltEmail() {
		return altEmail;
	}

	/**
	 * Sets the alternative email address.
	 * 
	 * @param altEmail the alternative email address or null
	 */
	public void setAltEmail(String altEmail) {
		this.altEmail = altEmail;
	}

	/**
	 * Returns the birth date.
	 * 
	 * @return the birth date or null
	 */
	public LocalDate getBirthDate() {
		return birthDate;
	}

	/**
	 * Sets the birth date.
	 * 
	 * @param birthDate the birth date or null
	 */
	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	/**
	 * If the user has deceased, this method returns the date of decease.
	 * 
	 * @return the date of decease or null
	 */
	public LocalDate getDeceasedDate() {
		return deceasedDate;
	}

	/**
	 * If the user has deceased, this method sets the date of decease.
	 * 
	 * @param deceasedDate the date of decease or null
	 */
	public void setDeceasedDate(LocalDate deceasedDate) {
		this.deceasedDate = deceasedDate;
	}

	/**
	 * Returns the identification number. This could be the personal
	 * identification number for government services or in a hospital
	 * administration system.
	 * 
	 * @return the identification number or null
	 */
	public String getIdNumber() {
		return idNumber;
	}

	/**
	 * Sets the identification number. This could be the personal
	 * identification number for government services or in a hospital
	 * administration system.
	 * 
	 * @param idNumber the identification number or null
	 */
	public void setIdNumber(String idNumber) {
		this.idNumber = idNumber;
	}

	/**
	 * Returns the landline phone number. It could include characters such as
	 * hyphens, parentheses and spaces. The format is not validated.
	 * 
	 * @return the landline phone number or null
	 */
	public String getLandlinePhone() {
		return landlinePhone;
	}

	/**
	 * Sets the landline phone number. It could include characters such as
	 * hyphens, parentheses and spaces. The format is not validated.
	 * 
	 * @param landlinePhone the landline phone number or null
	 */
	public void setLandlinePhone(String landlinePhone) {
		this.landlinePhone = landlinePhone;
	}

	/**
	 * Returns the mobile phone number. It could include characters such as
	 * hyphens, parentheses and spaces. The format is not validated.
	 * 
	 * @return the mobile phone number or null
	 */
	public String getMobilePhone() {
		return mobilePhone;
	}

	/**
	 * Sets the mobile phone number. It could include characters such as
	 * hyphens, parentheses and spaces. The format is not validated.
	 * 
	 * @param mobilePhone the mobile phone number or null
	 */
	public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}

	/**
	 * Returns the street name.
	 * 
	 * @return the street name or null
	 */
	public String getStreet() {
		return street;
	}

	/**
	 * Sets the street name.
	 * 
	 * @param street the street name or null
	 */
	public void setStreet(String street) {
		this.street = street;
	}

	/**
	 * Returns the house number in the street.
	 * 
	 * @return the house number or null
	 */
	public String getStreetNumber() {
		return streetNumber;
	}

	/**
	 * Sets the house number in the street.
	 * 
	 * @param streetNumber the house number or null
	 */
	public void setStreetNumber(String streetNumber) {
		this.streetNumber = streetNumber;
	}

	/**
	 * Returns extra address lines.
	 * 
	 * @return extra address lines or null
	 */
	public String getAddressExtra() {
		return addressExtra;
	}

	/**
	 * Sets extra address lines.
	 * 
	 * @param addressExtra extra address lines or null
	 */
	public void setAddressExtra(String addressExtra) {
		this.addressExtra = addressExtra;
	}

	/**
	 * Returns the postal code. The format is not validated.
	 * 
	 * @return the postal code or null
	 */
	public String getPostalCode() {
		return postalCode;
	}

	/**
	 * Sets the postal code. The format is not validated.
	 * 
	 * @param postalCode the postal code or null
	 */
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	/**
	 * Returns the town name.
	 * 
	 * @return the town name or null
	 */
	public String getTown() {
		return town;
	}

	/**
	 * Sets the town name.
	 * 
	 * @param town the town name or null
	 */
	public void setTown(String town) {
		this.town = town;
	}

	/**
	 * Returns a string code for the department.
	 * 
	 * @return the department code or null
	 */
	public String getDepartmentCode() {
		return departmentCode;
	}

	/**
	 * Sets a string code for the department.
	 * 
	 * @param departmentCode the department code or null
	 */
	public void setDepartmentCode(String departmentCode) {
		this.departmentCode = departmentCode;
	}

	/**
	 * Returns any extra information.
	 * 
	 * @return any extra information or null
	 */
	public String getExtraInfo() {
		return extraInfo;
	}

	/**
	 * Sets any extra information.
	 * 
	 * @param extraInfo any extra information or null
	 */
	public void setExtraInfo(String extraInfo) {
		this.extraInfo = extraInfo;
	}

	/**
	 * Returns the locale code. For example en_GB. It should consists of an
	 * ISO 639-1 language code, an underscore, and an ISO 3166-1 alpha-2 country
	 * code.
	 *
	 * @return the locale code or null
	 */
	public String getLocaleCode() {
		return localeCode;
	}

	/**
	 * Sets the locale code. For example en_GB. It should consists of an
	 * ISO 639-1 language code, an underscore, and an ISO 3166-1 alpha-2 country
	 * code.
	 *
	 * @param localeCode the locale code or null
	 */
	public void setLocaleCode(String localeCode) {
		this.localeCode = localeCode;
	}

	/**
	 * Returns the language formality that the user prefers to be addressed
	 * with. This can be "FORMAL", "INFORMAL" or null.
	 *
	 * @return the language formality or null
	 */
	public String getLanguageFormality() {
		return languageFormality;
	}

	/**
	 * Sets the language formality that the user prefers to be addressed with.
	 * This can be "FORMAL", "INFORMAL" or null.
	 *
	 * @param languageFormality the language formality or null
	 */
	public void setLanguageFormality(String languageFormality) {
		this.languageFormality = languageFormality;
	}

	/**
	 * Returns the time zone. This should be a location-based time zone ID from
	 * the tz database. For example Europe/Amsterdam.
	 *
	 * @return the time zone or null
	 */
	public String getTimeZone() {
		return timeZone;
	}

	/**
	 * Sets the time zone. This should be a location-based time zone ID from
	 * the tz database. For example Europe/Amsterdam.
	 *
	 * @param timeZone the time zone or null
	 */
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Returns the status of this user. This field can be used if your
	 * application needs more status information than {@link #isActive()
	 * isActive()}.
	 * 
	 * @return the status or null
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Sets the status of this user. This field can be used if your application
	 * needs more status information than {@link #isActive() isActive()}.
	 * 
	 * @param status the status or null
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Converts a User object to a UserV10 object.
	 *
	 * @param user the User object
	 * @return the UserV10 object
	 */
	public static UserV10 fromUser(User user) {
		UserV10 result = new UserV10();
		result.id = user.getId();
		result.userid = user.getUserid();
		result.email = user.getEmail();
		result.emailVerified = user.isEmailVerified();
		result.emailPendingVerification = user.getEmailPendingVerification();
		result.hasTemporaryEmail = user.isHasTemporaryEmail();
		result.hasTemporaryPassword = user.isHasTemporaryPassword();
		result.role = user.getRole();
		result.active = user.isActive();
		result.gender = user.getGender();
		result.maritalStatus = user.getMaritalStatus();
		result.title = user.getTitle();
		result.initials = user.getInitials();
		result.firstName = user.getFirstName();
		result.officialFirstNames = user.getOfficialFirstNames();
		result.prefixes = user.getPrefixes();
		result.lastName = user.getLastName();
		result.officialLastNames = user.getOfficialLastNames();
		result.fullName = user.getFullName();
		result.nickName = user.getNickName();
		result.altEmail = user.getAltEmail();
		result.birthDate = user.getBirthDate();
		result.deceasedDate = user.getDeceasedDate();
		result.idNumber = user.getIdNumber();
		result.landlinePhone = user.getLandlinePhone();
		result.mobilePhone = user.getMobilePhone();
		result.street = user.getStreet();
		result.streetNumber = user.getStreetNumber();
		result.addressExtra = user.getAddressExtra();
		result.postalCode = user.getPostalCode();
		result.town = user.getTown();
		result.departmentCode = user.getDepartmentCode();
		result.extraInfo = user.getExtraInfo();
		result.localeCode = user.getLocaleCode();
		result.languageFormality = user.getLanguageFormality();
		result.timeZone = user.getTimeZone();
		result.status = user.getStatus();
		return result;
	}

	public User toUser() {
		User result = new User();
		result.setId(id);
		result.setUserid(userid);
		result.setEmail(email);
		result.setEmailVerified(emailVerified);
		result.setEmailPendingVerification(emailPendingVerification);
		result.setHasTemporaryEmail(hasTemporaryEmail);
		result.setHasTemporaryPassword(hasTemporaryPassword);
		result.setRole(role);
		result.setActive(active);
		result.setGender(gender);
		result.setMaritalStatus(maritalStatus);
		result.setTitle(title);
		result.setInitials(initials);
		result.setFirstName(firstName);
		result.setOfficialFirstNames(officialFirstNames);
		result.setPrefixes(prefixes);
		result.setLastName(lastName);
		result.setOfficialLastNames(officialLastNames);
		result.setFullName(fullName);
		result.setNickName(nickName);
		result.setAltEmail(altEmail);
		result.setBirthDate(birthDate);
		result.setDeceasedDate(deceasedDate);
		result.setIdNumber(idNumber);
		result.setLandlinePhone(landlinePhone);
		result.setMobilePhone(mobilePhone);
		result.setStreet(street);
		result.setStreetNumber(streetNumber);
		result.setAddressExtra(addressExtra);
		result.setPostalCode(postalCode);
		result.setTown(town);
		result.setDepartmentCode(departmentCode);
		result.setExtraInfo(extraInfo);
		result.setLocaleCode(localeCode);
		result.setLanguageFormality(languageFormality);
		result.setTimeZone(timeZone);
		result.setStatus(status);
		return result;
	}
}
