package nl.rrd.senseeact.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.json.DateTimeFromIsoDateTimeDeserializer;
import nl.rrd.utils.json.IsoDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
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
	public static final String KEY_TOTP_FACTOR_SID = "factorSid";

	private String id;
	private String type;
	@JsonDeserialize(using=DateTimeFromIsoDateTimeDeserializer.class)
	@JsonSerialize(using=IsoDateTimeSerializer.class)
	private ZonedDateTime created;
	private boolean paired;
	private Map<String,Object> attemptPairData = new LinkedHashMap<>();
	private Map<String,Object> publicPairData = new LinkedHashMap<>();
	private Map<String,Object> privatePairData = new LinkedHashMap<>();

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
	 * Returns the system time when this record was created. That is when the
	 * user started the pair attempt.
	 *
	 * @return the system time when this record was created
	 */
	public ZonedDateTime getCreated() {
		return created;
	}

	/**
	 * Sets the system time when this record was created. That is when the user
	 * started the pair attempt.
	 *
	 * @param created the system time when this record was created
	 */
	public void setCreated(ZonedDateTime created) {
		this.created = created;
	}

	/**
	 * Returns true if the pairing has been completed, false otherwise.
	 *
	 * @return true if the pairing hass been completed, false otherwise
	 */
	public boolean isPaired() {
		return paired;
	}

	/**
	 * Sets the paired status. It should be true if the pairing has been
	 * completed, false otherwise.
	 *
	 * @param paired true if the pairing hass been completed, false otherwise
	 */
	public void setPaired(boolean paired) {
		this.paired = paired;
	}

	/**
	 * Returns data related to the pairing attempt. This is only defined if
	 * {@link #isPaired() isPaired()} is false. Otherwise it should be an empty
	 * map. The keys depend on the type.
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
	 * @return the data related to the pairing attempt
	 */
	public Map<String,Object> getAttemptPairData() {
		return attemptPairData;
	}

	/**
	 * Sets data related to the pairing attempt. This is only defined if {@link
	 * #isPaired() isPaired()} is false. Otherwise it should be an empty map.
	 * The keys depend on the type.
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
	 * @param attemptPairData the data related to the pairing attempt
	 */
	public void setAttemptPairData(Map<String,Object> attemptPairData) {
		this.attemptPairData = attemptPairData;
	}

	/**
	 * Returns the public pair data. That is data that can be shown to the user.
	 * This is only defined if {@link #isPaired() isPaired()} is true. Otherwise
	 * it should be an empty map. The keys depend on the type.
	 *
	 * <p><b>TOTP</b></p>
	 *
	 * <p>Empty map</p>
	 *
	 * <p><b>SMS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * @return the public pair data
	 */
	public Map<String,Object> getPublicPairData() {
		return publicPairData;
	}

	/**
	 * Sets the public pair data. That is data that can be shown to the user.
	 * This is only defined if {@link #isPaired() isPaired()} is true. Otherwise
	 * it should be an empty map. The keys depend on the type.
	 *
	 * <p><b>TOTP</b></p>
	 *
	 * <p>Empty map</p>
	 *
	 * <p><b>SMS</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_SMS_PHONE_NUMBER KEY_SMS_PHONE_NUMBER}</li>
	 * </ul></p>
	 *
	 * @param publicPairData the public pair data
	 */
	public void setPublicPairData(Map<String,Object> publicPairData) {
		this.publicPairData = publicPairData;
	}

	/**
	 * Returns the private pair data. That is data that should not be shown to
	 * the user. This is only defined if {@link #isPaired() isPaired()} is true.
	 * Otherwise it should be an empty map. The keys depend on the type.
	 *
	 * <p><b>TOTP</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_TOTP_FACTOR_SID KEY_TOTP_FACTOR_SID}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS</b></p>
	 *
	 * <p>Empty map</p>
	 *
	 * @return the private pair data
	 */
	public Map<String,Object> getPrivatePairData() {
		return privatePairData;
	}

	/**
	 * Sets the private pair data. That is data that should not be shown to the
	 * user. This is only defined if {@link #isPaired() isPaired()} is true.
	 * Otherwise it should be an empty map. The keys depend on the type.
	 *
	 * <p><b>TOTP</b></p>
	 *
	 * <p><ul>
	 * <li>{@link #KEY_TOTP_FACTOR_SID KEY_TOTP_FACTOR_SID}</li>
	 * </ul></p>
	 *
	 * <p><b>SMS</b></p>
	 *
	 * <p>Empty map</p>
	 *
	 * @param privatePairData the private pair data
	 */
	public void setPrivatePairData(Map<String,Object> privatePairData) {
		this.privatePairData = privatePairData;
	}
}
