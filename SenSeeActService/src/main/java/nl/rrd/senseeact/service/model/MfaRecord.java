package nl.rrd.senseeact.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.DateTimeFromIsoDateTimeDeserializer;
import nl.rrd.utils.json.IsoDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class defines a multi-factor authentication record.
 *
 * @author Dennis Hofs (d.hofs@rrd.nl)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MfaRecord {
	public static final String TYPE_SMS = "sms";
	public static final String TYPE_TOTP = "totp";

	public static final String KEY_SMS_PHONE_NUMBER = "phoneNumber";
	public static final String KEY_SMS_PARTIAL_PHONE_NUMBER = "partialPhoneNumber";
	public static final String KEY_TOTP_FACTOR_SID = "factorSid";
	public static final String KEY_TOTP_BINDING_URI = "bindingUri";

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
	 * MfaRecord.Status#CREATED CREATED}.
	 *
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Sets the status of this record. The default is {@link
	 * MfaRecord.Status#CREATED CREATED}.
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
	 * Returns the public data associated with this record. This depends on the
	 * status and type. A record with status CREATED is only returned right
	 * after the user created it. When the user requests a list of MFA records,
	 * only the verified records are returned. Therefore "created" records
	 * contain more public data than verified records.
	 *
	 * <p><b>TOTP, status: CREATED</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_TOTP_FACTOR_SID KEY_TOTP_FACTOR_SID}</li>
	 * </ul></p>
	 *
	 * <p><b>TOTP, status: VERIFY_SUCCESS</b></p>
	 *
	 * <p>Empty map</p>
	 *
	 * <p><b>SMS, status: CREATED</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS, status: VERIFY_SUCCESS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_SMS_PARTIAL_PHONE_NUMBER KEY_SMS_PARTIAL_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * @return the public data
	 */
	public Map<String,Object> getPublicData() {
		return publicData;
	}

	/**
	 * Sets the public data associated with this record. This depends on the
	 * status and type. A record with status CREATED is only returned right
	 * after the user created it. When the user requests a list of MFA records,
	 * only the verified records are returned.Therefore "created" records
	 * contain more public data than verified records.
	 *
	 * <p><b>TOTP, status: CREATED</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * </ul></p>
	 *
	 * <p><b>TOTP, status: VERIFY_SUCCESS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS, status: CREATED</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS, status: VERIFY_SUCCESS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_SMS_PARTIAL_PHONE_NUMBER KEY_SMS_PARTIAL_PHONE_NUMBER}</li>
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
	 * user when they request a list of MFA records. The keys depend on the type
	 * of record.
	 *
	 * <p><b>TOTP</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * <li>{@link #KEY_TOTP_FACTOR_SID KEY_TOTP_FACTOR_SID}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
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
	 * <li>{@link #KEY_TOTP_FACTOR_SID KEY_TOTP_FACTOR_SID}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * @param privateData the private data
	 */
	public void setPrivateData(Map<String,Object> privateData) {
		this.privateData = privateData;
	}
}
