package nl.rrd.senseeact.client.model.questionnaire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.*;
import nl.rrd.utils.schedule.TimeDuration;
import nl.rrd.utils.validation.MapReader;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class models a date/time schedule for events with a start date/time and
 * possible recurring dates and time. At a minimum, you should specify the start
 * date ({@link #setStartDate(LocalDate) setStartDate()}) and start time ({@link
 * #setStartTime(LocalTime) setStartTime()}). If nothing else is specified, the
 * event only occurs at that date/time. You can set recurring dates and times
 * independently. The time schedule is applied to every recurring date.
 *
 * <p>There are several recurring date options: every x days, weeks, months or
 * years. For weekly recurrences you can also select multiple days of the week.
 * For monthly recurrences you can choose to repeat on the same day of the week
 * (e.g. every 2nd Tuesday of the month), or on the same date number (e.g.
 * every 1st of the month). This is specified with {@link
 * #setRecurDate(RecurDate) setRecurDate()}. You can also specify an end date
 * {@link #setEndDate(LocalDate) setEndDate()} from which the recurring event
 * should no longer occur.</p>
 *
 * <p>The time schedule is applied to every recurring date. At a minimum, the
 * time schedule consists of the start time. If nothing else is specified, the
 * event only occurs at that time, at every recurring date. For example with
 * a date recurrence of every day, and a start time of 10:00, the event will
 * occur every day at 10:00.</p>
 *
 * <p>Time recurrences are specified with {@link #setRecurTime(TimeDuration)
 * setRecurTime()}. This consists of a number and a time unit. For example:
 * 2 hours. You can also set an end time with {@link #setEndTime(LocalTime)
 * setEndTime()}.</p>
 *
 * <p>Complete example:</p>
 *
 * <p><ul>
 * <li>startDate: 6 Jan 2024</li>
 * <li>endDate: 1 Jan 2025</li>
 * <li>recurDate: every week on Sat and Sun</li>
 * <li>startTime: 10:00</li>
 * <li>endTime: 20:01</li>
 * <li>recurTime: every 2 hours</li>
 * </ul></p>
 *
 * <p>The event occurs every week on Sat and Sun, starting at 6 Jan 2024. It
 * no longer occurs at or after 1 Jan 2025, so the last date is Sun 29 Dec 2024.
 * On each of these days, the event occurs every 2 hours starting at 10:00 and
 * no longer occurs at or after 20:01, so the last time is 20:00.</p>
 *
 * @author Dennis Hofs (RRD)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class DateTimeSchedule extends JsonObject {
	@JsonSerialize(using=SqlDateSerializer.class)
	@JsonDeserialize(using=SqlDateDeserializer.class)
	private LocalDate startDate;
	@JsonSerialize(using=SqlDateSerializer.class)
	@JsonDeserialize(using=SqlDateDeserializer.class)
	private LocalDate endDate = null;
	private RecurDate recurDate = null;
	@JsonSerialize(using=SqlTimeSerializer.class)
	@JsonDeserialize(using=SqlTimeDeserializer.class)
	private LocalTime startTime;
	@JsonSerialize(using=SqlTimeSerializer.class)
	@JsonDeserialize(using=SqlTimeDeserializer.class)
	private LocalTime endTime = null;
	@JsonSerialize(using=TimeDuration.Serializer.class)
	@JsonDeserialize(using=TimeDuration.Deserializer.class)
	private TimeDuration recurTime = null;
	@JsonSerialize(using=TimeDuration.Serializer.class)
	@JsonDeserialize(using=TimeDuration.Deserializer.class)
	private TimeDuration duration = null;
	@JsonSerialize(using=TimeDuration.Serializer.class)
	@JsonDeserialize(using=TimeDuration.Deserializer.class)
	private TimeDuration notifyDelay = null;

	/**
	 * Returns the start date. This must always be set.
	 *
	 * @return the start date
	 */
	public LocalDate getStartDate() {
		return startDate;
	}

	/**
	 * Sets the start date. This must always be set.
	 *
	 * @param startDate the start date
	 */
	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	/**
	 * Returns the end date. This is used with recurring dates and indicates the
	 * date at or after which the event should no longer occur.
	 *
	 * @return the end date or null
	 */
	public LocalDate getEndDate() {
		return endDate;
	}

	/**
	 * Sets the end date. This is used with recurring dates and indicates the
	 * date at or after which the event should no longer occur.
	 *
	 * @param endDate the end date or null
	 */
	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	/**
	 * Returns the date recurrence, or null if the event should not recur at
	 * multiple dates.
	 *
	 * @return the date recurrence or null
	 */
	public RecurDate getRecurDate() {
		return recurDate;
	}

	/**
	 * Sets the date recurrence, or null if the event should not recur at
	 * multiple dates.
	 *
	 * @param recurDate the date recurrence or null
	 */
	public void setRecurDate(RecurDate recurDate) {
		this.recurDate = recurDate;
	}

	/**
	 * Returns the start time. This must always be set. The event occurs at this
	 * time at every recurring date.
	 *
	 * @return the start time
	 */
	public LocalTime getStartTime() {
		return startTime;
	}

	/**
	 * Sets the start time. This must always be set. The event occurs at this
	 * time at every recurring date.
	 *
	 * @param startTime the start time
	 */
	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	/**
	 * Returns the end time. This is used with recurring times and indicates the
	 * time at or after which the event should no longer occur. This applies to
	 * every recurring date.
	 *
	 * @return the end time or null
	 */
	public LocalTime getEndTime() {
		return endTime;
	}

	/**
	 * Sets the end time. This is used with recurring times and indicates the
	 * time at or after which the event should no longer occur. This applies to
	 * every recurring date.
	 *
	 * @param endTime the end time or null
	 */
	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	/**
	 * Returns the time recurrence per recurring date, or null if the event
	 * should not recur at multiple times per date. For example if this is set
	 * to 2 hours, it means that the event occurs every 2 hours at every
	 * recurring date. If this is null, the event will occur only once at every
	 * recurring date, namely at the start time.
	 *
	 * @return the time recurrence or null
	 */
	public TimeDuration getRecurTime() {
		return recurTime;
	}

	/**
	 * Sets the time recurrence per recurring date, or null if the event should
	 * not recur at multiple times per date. For example if this is set to 2
	 * hours, it means that the event occurs every 2 hours at every recurring
	 * date. If this is null, the event will occur only once at every recurring
	 * date, namely at the start time.
	 *
	 * @param recurTime the time recurrence or null
	 */
	public void setRecurTime(TimeDuration recurTime) {
		this.recurTime = recurTime;
	}

	/**
	 * Returns the duration of the event. If set to null, the duration is
	 * indefinite and the event may last until the next event occurs.
	 *
	 * @return the duration or null
	 */
	public TimeDuration getDuration() {
		return duration;
	}

	/**
	 * Sets the duration of the event. If set to null (default), the duration is
	 * indefinite and the event may last until the next event occurs.
	 *
	 * @param duration the duration or null
	 */
	public void setDuration(TimeDuration duration) {
		this.duration = duration;
	}

	/**
	 * If the user should receive a notification for the event, this method
	 * returns when the notification should be sent as an amount of time after
	 * the start of the event. If you want to play a notification immediately
	 * at the start time, you can set it to 0 minutes. If set to null (default),
	 * then no notification will be sent.
	 *
	 * @return the delay between the start time and notification time, or null
	 */
	public TimeDuration getNotifyDelay() {
		return notifyDelay;
	}

	/**
	 * If the user should receive a notification for the event, this method sets
	 * when the notification should be sent as an amount of time after the start
	 * of the event. If you want to play a notification immediately at the start
	 * time, you can set it to 0 minutes. If set to null (default), then no
	 * notification will be sent.
	 *
	 * @param notifyDelay the delay between the start time and notification
	 * time, or null
	 */
	public void setNotifyDelay(TimeDuration notifyDelay) {
		this.notifyDelay = notifyDelay;
	}

	/**
	 * The base class for date recurrences. There are subclasses for recurrences
	 * per day, week, month and year.
	 */
	@JsonDeserialize(using=RecurDateDeserializer.class)
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static abstract class RecurDate {
		private RepeatType type;
		private int count;

		/**
		 * Constructs a new instance.
		 *
		 * @param type the repeat type (day, week, month, year)
		 */
		public RecurDate(RepeatType type) {
			this.type = type;
		}

		/**
		 * Returns the repeat type (day, week, month, year).
		 *
		 * @return the repeat type (day, week, month, year)
		 */
		public RepeatType getType() {
			return type;
		}

		/**
		 * Returns the number of date units between two recurrences. For example
		 * if the repeat type is "day" and this number is 2, then the event
		 * recurs every 2 days.
		 *
		 * @return the number of date units between two recurrences
		 */
		public int getCount() {
			return count;
		}

		/**
		 * Sets the number of date units between two recurrences. For example
		 * if the repeat type is "day" and this number is 2, then the event
		 * recurs every 2 days.
		 *
		 * @param count the number of date units between two recurrences
		 */
		public void setCount(int count) {
			this.count = count;
		}
	}

	/**
	 * Date recurrence for every x days.
	 */
	@JsonDeserialize(using=JsonDeserializer.None.class)
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class RepeatDay extends RecurDate {
		public RepeatDay() {
			super(RepeatType.DAY);
		}
	}

	/**
	 * Date recurrence for every x weeks. Per week you can select the days of
	 * the week when the event should recur. For example: every (1) week at Sat
	 * and Sun.
	 */
	@JsonDeserialize(using=JsonDeserializer.None.class)
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class RepeatWeek extends RecurDate {
		private List<Integer> daysOfTheWeek = new ArrayList<>();

		public RepeatWeek() {
			super(RepeatType.WEEK);
		}

		/**
		 * Returns the days of the week when the event should occur in each
		 * recurring week. Monday is 1, Sunday is 7.
		 *
		 * @return the days of the week
		 */
		public List<Integer> getDaysOfTheWeek() {
			return daysOfTheWeek;
		}

		/**
		 * Sets the days of the week when the event should occur in each
		 * recurring week. Monday is 1, Sunday is 7.
		 *
		 * @param daysOfTheWeek the days of the week
		 */
		public void setDaysOfTheWeek(List<Integer> daysOfTheWeek) {
			this.daysOfTheWeek = daysOfTheWeek;
		}
	}

	/**
	 * Date recurrence of every x months. You can choose to repeat on the same
	 * day of the week (e.g. every 2nd Tuesday of the month), or on the same
	 * date number (e.g. every 1st of the month).
	 */
	@JsonDeserialize(using=JsonDeserializer.None.class)
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class RepeatMonth extends RecurDate {
		private RepeatMonthType repeatMonthType =
				RepeatMonthType.SAME_DAY_OF_WEEK;

		public RepeatMonth() {
			super(RepeatType.MONTH);
		}

		/**
		 * Returns the repeat month type: same day of the week (default), or
		 * same date number.
		 *
		 * @return the repeat month type
		 */
		public RepeatMonthType getRepeatMonthType() {
			return repeatMonthType;
		}

		/**
		 * Sets the repeat month type: same day of the week (default), or same
		 * date number.
		 *
		 * @param repeatMonthType the repeat month type
		 */
		public void setRepeatMonthType(RepeatMonthType repeatMonthType) {
			this.repeatMonthType = repeatMonthType;
		}
	}

	public enum RepeatMonthType {
		/**
		 * Recur every x months at the same day of the week. The day of the week
		 * is determined by the start date. For example if the start date is
		 * the 2nd Tuesday of the month, then the event will occur every
		 * recurring month at the 2nd Tuesday of the month.
		 */
		SAME_DAY_OF_WEEK,

		/**
		 * Recur every x months at the same date number. The date number is
		 * determined by the start date. For example if the start date is the
		 * 1st of the month, then the event will occur every recurring month
		 * at the 1st.
		 */
		SAME_DATE_NUMBER
	}

	/**
	 * Date recurrence of every x years.
	 */
	@JsonDeserialize(using=JsonDeserializer.None.class)
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class RepeatYear extends RecurDate {
		public RepeatYear() {
			super(RepeatType.YEAR);
		}
	}

	public enum RepeatType {
		DAY,
		WEEK,
		MONTH,
		YEAR
	}

	public static class RecurDateDeserializer
			extends JsonDeserializer<RecurDate> {
		@Override
		public RecurDate deserialize(JsonParser p,
				DeserializationContext ctxt) throws IOException,
				JacksonException {
			Map<?,?> map = p.readValueAs(Map.class);
			MapReader mapReader = new MapReader(map);
			RepeatType type;
			try {
				type = mapReader.readEnum("type", RepeatType.class);
			} catch (ParseException ex) {
				throw new JsonParseException(p, ex.getMessage(),
						p.currentTokenLocation(), ex);
			}
			Class<? extends RecurDate> typeClass = switch (type) {
				case DAY -> RepeatDay.class;
				case WEEK -> RepeatWeek.class;
				case MONTH -> RepeatMonth.class;
				case YEAR ->  RepeatYear.class;
			};
			try {
				return JsonMapper.convert(map, typeClass);
			} catch (ParseException ex) {
				throw new JsonParseException(p, ex.getMessage(),
						p.currentTokenLocation(), ex);
			}
		}
	}
}
