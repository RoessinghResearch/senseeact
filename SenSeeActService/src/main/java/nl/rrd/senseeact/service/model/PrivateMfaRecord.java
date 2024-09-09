package nl.rrd.senseeact.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.senseeact.client.model.MfaRecord;
import nl.rrd.utils.json.DateTimeFromIsoDateTimeDeserializer;
import nl.rrd.utils.json.IsoDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class defines a multi-factor authentication record. It contains private
 * information that should not be returned to the user. It mirrors {@link
 * MfaRecord MfaRecord}, but does not extend it, to prevent that an instance
 * of this record is returned where an {@link MfaRecord MfaRecord} should be
 * returned.
 *
 * <p>While an {@link MfaRecord MfaRecord} has a "verified" property, this class
 * has a status, that can be CREATED, VERIFY_FAIL or VERIFY_SUCCESS.</p>
 *
 * <p>Status CREATED corresponds to an unverified record. It is returned right
 * after the user creates it, before a verification code has been submitted. It
 * can contain more sensitive public data such as a complete phone number, which
 * the user just entered.</p>
 *
 * <p>Status VERIFY_SUCCESS corresponds to a verified record. It can be returned
 * after the user authenticates with their password, but before the second
 * factor. It is also returned when the user requests a list of their verified
 * records. In this case more sensitive data, such as a complete phone number,
 * is not included in the public data.</p>
 *
 * <p>A record with status VERIFY_FAIL is never returned to the user. These are
 * only preserved in the database to keep track of attempts to add MFA
 * records and enforce a maximum number of attempts within a certain time
 * span.</p>
 *
 * @author Dennis Hofs (RRD)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class PrivateMfaRecord {
	public static class Constants extends MfaRecord.Constants {
		public static final String KEY_TOTP_FACTOR_SID = "factorSid";
	}

	private String id;
	private String type;
	@JsonDeserialize(using=DateTimeFromIsoDateTimeDeserializer.class)
	@JsonSerialize(using=IsoDateTimeSerializer.class)
	private ZonedDateTime created;
	private Status status = Status.CREATED;
	private List<Long> verifyTimes = new ArrayList<>();
	private Map<String,Object> publicData = new LinkedHashMap<>();
	private Map<String,Object> privateData = new LinkedHashMap<>();

	public enum Status {
		CREATED,
		VERIFY_FAIL,
		VERIFY_SUCCESS
	}

	/**
	 * Returns the UUID that identifies this MFA record.
	 *
	 * @return the UUID that identifies this MFA record
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the UUID that identifies this MFA record.
	 *
	 * @param id the UUID that identifies this MFA record
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the MFA type. This should be one of the TYPE_* constants.
	 *
	 * @return the MFA type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the MFA type. This should be one of the TYPE_* constants.
	 *
	 * @param type the MFA type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the system time when this record was created.
	 *
	 * @return the system time when this record was created
	 */
	public ZonedDateTime getCreated() {
		return created;
	}

	/**
	 * Sets the system time when this record was created.
	 *
	 * @param created the system time when this record was created
	 */
	public void setCreated(ZonedDateTime created) {
		this.created = created;
	}

	/**
	 * Returns the status of this record. The default is {@link
	 * PrivateMfaRecord.Status#CREATED CREATED}.
	 *
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Sets the status of this record. The default is {@link
	 * PrivateMfaRecord.Status#CREATED CREATED}.
	 *
	 * @param status the status
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * Returns the Unix timestamps in milliseconds when a verification was
	 * performed (with or without success). This can be used to check the
	 * maximum number of verification attempts.
	 *
	 * @return the verification times
	 */
	public List<Long> getVerifyTimes() {
		return verifyTimes;
	}

	/**
	 * Sets the Unix timestamps in milliseconds when a verification was
	 * performed (with or without success). This can be used to check the
	 * maximum number of verification attempts.
	 *
	 * @param verifyTimes the verification times
	 */
	public void setVerifyTimes(List<Long> verifyTimes) {
		this.verifyTimes = verifyTimes;
	}

	/**
	 * Returns the public data associated with this record. This is the data
	 * that is included in an {@link MfaRecord MfaRecord}. It depends on the
	 * status and type. See more details at the top of this page.
	 *
	 * <p><b>TOTP, status: CREATED</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * </ul></p>
	 *
	 * <p><b>TOTP, status: VERIFY_SUCCESS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS, status: CREATED</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS, status: VERIFY_SUCCESS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_SMS_PARTIAL_PHONE_NUMBER KEY_SMS_PARTIAL_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * @return the public data
	 */
	public Map<String,Object> getPublicData() {
		return publicData;
	}

	/**
	 * Sets the public data associated with this record. This is the data that
	 * is included in an {@link MfaRecord MfaRecord}. It depends on the status
	 * and type. See more details at the top of this page.
	 *
	 * <p><b>TOTP, status: CREATED</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * </ul></p>
	 *
	 * <p><b>TOTP, status: VERIFY_SUCCESS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS, status: CREATED</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS, status: VERIFY_SUCCESS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_SMS_PARTIAL_PHONE_NUMBER KEY_SMS_PARTIAL_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * @param publicData the public data
	 */
	public void setPublicData(Map<String,Object> publicData) {
		this.publicData = publicData;
	}

	/**
	 * Returns the private data associated with this record. That is all data,
	 * including public data and data that is too sensitive to return to the
	 * user. The keys depend on the type of record.
	 *
	 * <p><b>TOTP</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_TOTP_FACTOR_SID KEY_TOTP_FACTOR_SID}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * @return the private data
	 */
	public Map<String,Object> getPrivateData() {
		return privateData;
	}

	/**
	 * Sets the private data associated with this record. That is all data,
	 * including public data and data that is too sensitive to return to the
	 * user when they request a list of MFA records. The keys depend on the type
	 * of record.
	 *
	 * <p><b>TOTP</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_TOTP_FACTOR_SID KEY_TOTP_FACTOR_SID}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link PrivateMfaRecord.Constants#KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * @param privateData the private data
	 */
	public void setPrivateData(Map<String,Object> privateData) {
		this.privateData = privateData;
	}

	public MfaRecord toPublicMfaRecord() {
		MfaRecord result = new MfaRecord();
		result.setId(id);
		result.setType(type);
		result.setCreated(created);
		result.setData(new LinkedHashMap<>(publicData));
		return result;
	}
}
