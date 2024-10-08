package nl.rrd.senseeact.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.DateTimeFromIsoDateTimeDeserializer;
import nl.rrd.utils.json.IsoDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class defines a multi-factor authentication record. It contains only
 * data that can be returned to the user. The record can be verified or
 * unverified (see {@link #isVerified() isVerified()}).
 *
 * <p>An unverified record is returned right after the user creates it, before a
 * verification code has been submitted. It can contain more sensitive data
 * such as a complete phone number, which the user just entered.</p>
 *
 * <p>A verified record can be returned after the user authenticates with their
 * password, but before the second factor. It is also returned when the user
 * requests a list of their verified records. In this case more sensitive data,
 * such as a complete phone number, is not included.</p>
 *
 * @author Dennis Hofs (RRD)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MfaRecord {
	public static class Constants {
		public static final String TYPE_TOTP = "totp";
		public static final String TYPE_SMS = "sms";

		public static final String KEY_TOTP_BINDING_URI = "bindingUri";
		public static final String KEY_SMS_PHONE_NUMBER = "phoneNumber";
		public static final String KEY_SMS_PARTIAL_PHONE_NUMBER = "partialPhoneNumber";
	}

	private String id;
	private String type;
	@JsonDeserialize(using= DateTimeFromIsoDateTimeDeserializer.class)
	@JsonSerialize(using= IsoDateTimeSerializer.class)
	private ZonedDateTime created;
	private boolean verified = false;
	private Map<String,Object> data = new LinkedHashMap<>();

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
	 * Returns the MFA type. This should be one of the TYPE_* constants defined
	 * in {@link MfaRecord.Constants Constants}.
	 *
	 * @return the MFA type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the MFA type. This should be one of the TYPE_* constants defined
	 * in {@link MfaRecord.Constants Constants}.
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
	 * Returns whether this record is verified or unverified. The default is
	 * false. See more details at the top of this page.
	 *
	 * @return true if this record is verified, false if it's unverified
	 */
	public boolean isVerified() {
		return verified;
	}

	/**
	 * Sets whether this record is verified or unverified. The default is false.
	 * See more details at the top of this page.
	 *
	 * @param verified true if this record is verified, false if it's unverified
	 */
	public void setVerified(boolean verified) {
		this.verified = verified;
	}

	/**
	 * Returns the public data associated with this record. This depends on the
	 * type and whether the record is verified or unverified. See more details
	 * about verified and unverified records at the top of this page.
	 *
	 * <p><b>TOTP, unverified</b></p>
	 *
	 * <p><ul>
	 * <li>{@link MfaRecord.Constants#KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * </ul></p>
	 *
	 * <p><b>TOTP, verified</b></p>
	 *
	 * <p>Empty map</p>
	 *
	 * <p><b>SMS, unverified</b></p>
	 *
	 * <p><ul>
	 * <li>{@link MfaRecord.Constants#KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS, verified</b></p>
	 *
	 * <p><ul>
	 * <li>{@link MfaRecord.Constants#KEY_SMS_PARTIAL_PHONE_NUMBER KEY_SMS_PARTIAL_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * @return the public data
	 */
	public Map<String,Object> getData() {
		return data;
	}

	/**
	 * Sets the public data associated with this record. This depends on the
	 * type and whether the record is verified or unverified. See more details
	 * about verified and unverified records at the top of this page.
	 *
	 * <p><b>TOTP, unverified</b></p>
	 *
	 * <p><ul>
	 * <li>{@link MfaRecord.Constants#KEY_TOTP_BINDING_URI KEY_TOTP_BINDING_URI}</li>
	 * </ul></p>
	 *
	 * <p><b>TOTP, verified</b></p>
	 *
	 * <p>Empty map</p>
	 *
	 * <p><b>SMS, unverified</b></p>
	 *
	 * <p><ul>
	 * <li>{@link MfaRecord.Constants#KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS, verified</b></p>
	 *
	 * <p><ul>
	 * <li>{@link MfaRecord.Constants#KEY_SMS_PARTIAL_PHONE_NUMBER KEY_SMS_PARTIAL_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * @param data the public data
	 */
	public void setData(Map<String,Object> data) {
		this.data = data;
	}
}
