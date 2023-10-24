package nl.rrd.senseeact.client.model.notification;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import nl.rrd.senseeact.client.model.sample.UTCSample;
import nl.rrd.senseeact.dao.DatabaseField;
import nl.rrd.senseeact.dao.DatabaseType;
import nl.rrd.utils.json.LocalDateTimeDeserializer;
import nl.rrd.utils.json.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * This class models a notification instance that should be given in an app.
 * It uses only local times. The sample time should be the start time of the
 * notification. Because the sample time requires a time zone, this class uses
 * UTC as a default, but note that this has no meaning and should be ignored.
 *
 * <p>The notification remains active from the start time until "ended" is set
 * to true or the end time is reached. To check whether the notification is
 * active, you can call {@link #isActive(ZonedDateTime) isActive()}.</p>
 *
 * <p>If an end time is set, then "ended" should be set to true when the end
 * time is reached. This makes it more efficient to get all active notifications
 * with a simple filter on the indexed field "ended", but you should still call
 * {@link #isActive(ZonedDateTime) isActive()} to see if the notification is
 * really active or ended.</p>
 *
 * @author Dennis Hofs (RRD)
 */
public class AppNotification extends UTCSample {
	public static final int PRIORITY_NONE = 0;
	public static final int PRIORITY_LOW = 100;
	public static final int PRIORITY_MEDIUM = 200;
	public static final int PRIORITY_HIGH = 300;

	@DatabaseField(value=DatabaseType.STRING, index=true)
	private String notificationClassId;

	@DatabaseField(value=DatabaseType.TEXT)
	private String data = null;

	@DatabaseField(value=DatabaseType.DATETIME)
	@JsonSerialize(using=LocalDateTimeSerializer.class)
	@JsonDeserialize(using=LocalDateTimeDeserializer.class)
	private LocalDateTime endTime = null;

	@DatabaseField(value=DatabaseType.DATETIME)
	@JsonSerialize(using=LocalDateTimeSerializer.class)
	@JsonDeserialize(using=LocalDateTimeDeserializer.class)
	private LocalDateTime alarmTime = null;

	@DatabaseField(value=DatabaseType.BYTE, index=true)
	private boolean ended = false;

	@DatabaseField(value=DatabaseType.BYTE)
	private boolean alarmTriggered = false;

	@DatabaseField(value=DatabaseType.INT)
	private int priority = PRIORITY_NONE;

	/**
	 * Constructs a new empty notification. This is used for DataAccessObjects
	 * and JSON serialization. Users should not call this.
	 */
	public AppNotification() {
	}

	/**
	 * Constructs a new notification at the specified time.
	 *
	 * @param user the user (user ID)
	 * @param startTime the start time
	 * @param notificationClassId the notification class ID
	 */
	public AppNotification(String user, LocalDateTime startTime,
			String notificationClassId) {
		super(user, startTime.atZone(ZoneOffset.UTC));
		this.notificationClassId = notificationClassId;
	}

	/**
	 * Returns the notification class ID. The ID should map to a class that
	 * defines the notification icon and text and what should be done when the
	 * alarm or notification is clicked. The notification class may use {@link
	 * #getData() getData()} for that.
	 *
	 * @return the notification class ID
	 */
	public String getNotificationClassId() {
		return notificationClassId;
	}

	/**
	 * Sets the notification class ID. The ID should map to a class that defines
	 * the notification icon and text and what should be done when the alarm or
	 * notification is clicked. The notification class may use {@link #getData()
	 * getData()} for that.
	 *
	 * @param notificationClassId the notification class ID
	 */
	public void setNotificationClassId(String notificationClassId) {
		this.notificationClassId = notificationClassId;
	}

	/**
	 * Returns data that defines the notification icon and text and what should
	 * be done when the alarm or notification is clicked. The format of the
	 * data depends on the notification class (see {@link
	 * #getNotificationClassId() getNotificationClassId()}.
	 *
	 * @return the data
	 */
	public String getData() {
		return data;
	}

	/**
	 * Sets data that defines the notification icon and text and what should be
	 * done when the alarm or notification is clicked. The format of the data
	 * depends on the notification class (see {@link #getNotificationClassId()
	 * getNotificationClassId()}.
	 *
	 * @param data the data
	 */
	public void setData(String data) {
		this.data = data;
	}

	/**
	 * Returns the time when the notification ends. If there is no predefined
	 * end time, this method returns null (default).
	 *
	 * @return the end time or null
	 */
	public LocalDateTime getEndTime() {
		return endTime;
	}

	/**
	 * Sets the time when the notification should end. If there is no predefined
	 * end time, you can set this to null (default).
	 *
	 * @param endTime the end time or null
	 */
	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	/**
	 * Returns the time when an alarm should be given for this notification.
	 * This should trigger a notification in the status bar of the device and
	 * it may trigger a sound and vibration depending on the device settings.
	 * If no alarm should be given, this method returns null (default).
	 *
	 * @return the alarm time or null
	 */
	public LocalDateTime getAlarmTime() {
		return alarmTime;
	}

	/**
	 * Sets the time when an alarm should be given for this notification. This
	 * should trigger a notification in the status bar of the device and it may
	 * trigger a sound and vibration depending on the device settings. If no
	 * alarm should be given, you can set this to null (default).
	 *
	 * @param alarmTime the alarm time or null
	 */
	public void setAlarmTime(LocalDateTime alarmTime) {
		this.alarmTime = alarmTime;
	}

	/**
	 * Returns whether the notification has been set as ended. To check whether
	 * the notification is really active or ended, you should call {@link
	 * #isActive(ZonedDateTime) isActive()}.
	 *
	 * <p>This field can be set manually at any time or it can be set after the
	 * end time has been reached. See the top of this page for more details.</p>
	 *
	 * @return true if the notification has been set as ended, false otherwise
	 */
	public boolean isEnded() {
		return ended;
	}

	/**
	 * Sets the notification state as ended. This field can be set manually at
	 * any time or it can be set after the end time has been reached. See the
	 * top of this page for more details.
	 *
	 * @param ended true if the notification state should be set as ended,
	 * false otherwise
	 */
	public void setEnded(boolean ended) {
		this.ended = ended;
	}

	/**
	 * Returns whether the alarm has been triggered. If this method returns
	 * false and the alarm time has been reached, then the alarm should be
	 * triggered now and this field should be set to true. It should trigger a
	 * notification in the status bar of the device and it may trigger a sound
	 * and vibration depending on the device settings.
	 *
	 * @return true if the alarm has been triggered, false otherwise
	 */
	public boolean isAlarmTriggered() {
		return alarmTriggered;
	}

	/**
	 * Sets whether the alarm has been triggered. The alarm should be triggered
	 * if this field is false and the alarm time has been reached. It should
	 * set this field to true and trigger a notification in the status bar of
	 * the device and it may trigger a sound and vibration depending on the
	 * device settings.
	 *
	 * @param alarmTriggered true if the alarm has been triggered, false
	 * otherwise
	 */
	public void setAlarmTriggered(boolean alarmTriggered) {
		this.alarmTriggered = alarmTriggered;
	}

	/**
	 * Returns the priority. You can use one of the PRIORITY_* constants defined
	 * in this class or any other number. The default is {@link #PRIORITY_NONE
	 * PRIORITY_NONE}. Priority values:
	 *
	 * <p><ul>
	 * <li>{@link #PRIORITY_NONE PRIORITY_NONE}: 0</li>
	 * <li>{@link #PRIORITY_LOW PRIORITY_LOW}: 100</li>
	 * <li>{@link #PRIORITY_MEDIUM PRIORITY_MEDIUM}: 200</li>
	 * <li>{@link #PRIORITY_HIGH PRIORITY_HIGH}: 300</li>
	 * </ul></p>
	 *
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Sets the priority. You can use one of the PRIORITY_* constants defined in
	 * this class or any other number. The default is {@link #PRIORITY_NONE
	 * PRIORITY_NONE}. Priority values:
	 *
	 * <p><ul>
	 * <li>{@link #PRIORITY_NONE PRIORITY_NONE}: 0</li>
	 * <li>{@link #PRIORITY_LOW PRIORITY_LOW}: 100</li>
	 * <li>{@link #PRIORITY_MEDIUM PRIORITY_MEDIUM}: 200</li>
	 * <li>{@link #PRIORITY_HIGH PRIORITY_HIGH}: 300</li>
	 * </ul></p>
	 *
	 * @param priority the priority
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Returns whether this notification is currently active. It checks the
	 * start time and end time and the field "ended".
	 *
	 * @param now the current time
	 * @return true if the notification is active, false otherwise
	 */
	public boolean isActive(ZonedDateTime now) {
		if (ended)
			return false;
		LocalDateTime nowLocal = now.toLocalDateTime();
		if (toDateTime().toLocalDateTime().isAfter(nowLocal))
			return false;
		if (endTime != null && !now.toLocalDateTime().isBefore(endTime))
			return false;
		return true;
	}
}
