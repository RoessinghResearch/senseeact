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
import nl.rrd.utils.validation.ValidateNotNull;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
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
	@ValidateNotNull
	private LocalDate startDate;
	@JsonSerialize(using=SqlDateSerializer.class)
	@JsonDeserialize(using=SqlDateDeserializer.class)
	private LocalDate endDate = null;
	private RecurDate recurDate = null;
	@JsonSerialize(using=SqlTimeSerializer.class)
	@JsonDeserialize(using=SqlTimeDeserializer.class)
	@ValidateNotNull
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
		 * Constructs a new instance.
		 *
		 * @param type the repeat type (day, week, month, year)
		 * @param count the count between two instances
		 */
		public RecurDate(RepeatType type, int count) {
			this.type = type;
			this.count = count;
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

		public abstract LocalDate findLatestDateBefore(LocalDate startDate,
				LocalDate date);
		public abstract boolean occursAtDate(LocalDate startDate,
				LocalDate date);
		public abstract LocalDate findNextDateAfter(LocalDate startDate,
				LocalDate date);
	}

	/**
	 * Date recurrence for every x days.
	 */
	@JsonDeserialize(using=JsonDeserializer.None.class)
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class RecurDay extends RecurDate {
		public RecurDay() {
			super(RepeatType.DAY);
		}

		public RecurDay(int count) {
			super(RepeatType.DAY, count);
		}

		@Override
		public LocalDate findLatestDateBefore(LocalDate startDate,
				LocalDate date) {
			Period period = Period.ofDays(getCount());
			int singleCount = (int)ChronoUnit.DAYS.between(startDate,
					date.minusDays(1));
			int recurCount = singleCount / getCount();
			return startDate.plus(period.multipliedBy(recurCount));
		}

		@Override
		public boolean occursAtDate(LocalDate startDate, LocalDate date) {
			Period period = Period.ofDays(getCount());
			int singleCount = (int)ChronoUnit.DAYS.between(startDate, date);
			int recurCount = singleCount / getCount();
			LocalDate scheduleDate = startDate.plus(period.multipliedBy(
					recurCount));
			return scheduleDate.isEqual(date);
		}

		@Override
		public LocalDate findNextDateAfter(LocalDate startDate,
				LocalDate date) {
			Period period = Period.ofDays(getCount());
			int singleCount = (int)ChronoUnit.DAYS.between(startDate,
					date.plus(period));
			int recurCount = singleCount / getCount();
			return startDate.plus(period.multipliedBy(recurCount));
		}
	}

	/**
	 * Date recurrence for every x weeks. Per week you can select the days of
	 * the week when the event should recur. For example: every (1) week at Sat
	 * and Sun.
	 */
	@JsonDeserialize(using=JsonDeserializer.None.class)
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class RecurWeek extends RecurDate {
		private List<Integer> daysOfTheWeek = new ArrayList<>();

		public RecurWeek() {
			super(RepeatType.WEEK);
		}

		public RecurWeek(int count) {
			super(RepeatType.WEEK, count);
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

		@Override
		public LocalDate findLatestDateBefore(LocalDate startDate,
				LocalDate date) {
			Period period = Period.ofWeeks(getCount());
			List<Integer> days = daysOfTheWeek;
			if (days.isEmpty())
				days = List.of(startDate.getDayOfWeek().getValue());
			LocalDate recurWeek = findPreviousRecurWeek(startDate, date);
			LocalDate lastDate = startDate;
			while (true) {
				for (int day : days) {
					LocalDate cmpDate = recurWeek.plusDays(day - 1);
					if (!cmpDate.isBefore(startDate)) {
						if (!cmpDate.isBefore(date))
							return lastDate;
						lastDate = cmpDate;
					}
				}
				recurWeek = recurWeek.plus(period);
			}
		}

		@Override
		public boolean occursAtDate(LocalDate startDate, LocalDate date) {
			Period period = Period.ofWeeks(getCount());
			List<Integer> days = daysOfTheWeek;
			if (days.isEmpty())
				days = List.of(startDate.getDayOfWeek().getValue());
			LocalDate recurWeek = findPreviousRecurWeek(startDate, date);
			while (true) {
				for (int day : days) {
					LocalDate cmpDate = recurWeek.plusDays(day - 1);
					if (cmpDate.isEqual(date))
						return true;
					else if (cmpDate.isAfter(date))
						return false;
				}
				recurWeek = recurWeek.plus(period);
			}
		}

		@Override
		public LocalDate findNextDateAfter(LocalDate startDate,
				LocalDate date) {
			Period period = Period.ofWeeks(getCount());
			List<Integer> days = daysOfTheWeek;
			if (days.isEmpty())
				days = List.of(startDate.getDayOfWeek().getValue());
			LocalDate recurWeek = findPreviousRecurWeek(startDate, date);
			while (true) {
				for (int day : days) {
					LocalDate cmpDate = recurWeek.plusDays(day - 1);
					if (cmpDate.isAfter(date))
						return cmpDate;
				}
				recurWeek = recurWeek.plus(period);
			}
		}

		private LocalDate findPreviousRecurWeek(LocalDate startDate,
				LocalDate date) {
			LocalDate startWeek = startDate.minusDays(
					startDate.getDayOfWeek().getValue() - 1);
			LocalDate lastWeek = date.minusDays(
					date.getDayOfWeek().getValue() + 6);
			if (lastWeek.isBefore(startWeek))
				return startWeek;
			Period period = Period.ofWeeks(getCount());
			int singleCount = (int)ChronoUnit.WEEKS.between(startWeek,
					lastWeek);
			int recurCount = singleCount / getCount();
			return startWeek.plus(period.multipliedBy(recurCount));
		}
	}

	/**
	 * Date recurrence of every x months. You can choose to repeat on the same
	 * day of the week (e.g. every 2nd Tuesday of the month), or on the same
	 * date number (e.g. every 1st of the month).
	 */
	@JsonDeserialize(using=JsonDeserializer.None.class)
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class RecurMonth extends RecurDate {
		private RecurMonthType repeatMonthType =
				RecurMonthType.SAME_DAY_OF_WEEK;

		public RecurMonth() {
			super(RepeatType.MONTH);
		}

		public RecurMonth(int count) {
			super(RepeatType.MONTH, count);
		}

		/**
		 * Returns the repeat month type: same day of the week (default), or
		 * same date number.
		 *
		 * @return the repeat month type
		 */
		public RecurMonthType getRepeatMonthType() {
			return repeatMonthType;
		}

		/**
		 * Sets the repeat month type: same day of the week (default), or same
		 * date number.
		 *
		 * @param repeatMonthType the repeat month type
		 */
		public void setRepeatMonthType(RecurMonthType repeatMonthType) {
			this.repeatMonthType = repeatMonthType;
		}

		@Override
		public LocalDate findLatestDateBefore(LocalDate startDate,
				LocalDate date) {
			Period period = Period.ofMonths(getCount());
			LocalDate recurMonth = findPreviousRecurMonth(startDate, date);
			LocalDate lastDate = startDate;
			while (true) {
				LocalDate cmpDate = getRecurDateInMonth(startDate,
						recurMonth);
				if (!cmpDate.isBefore(startDate)) {
					if (!cmpDate.isBefore(date))
						return lastDate;
					lastDate = cmpDate;
				}
				recurMonth = recurMonth.plus(period);
			}
		}

		@Override
		public boolean occursAtDate(LocalDate startDate, LocalDate date) {
			Period period = Period.ofMonths(getCount());
			LocalDate recurMonth = findPreviousRecurMonth(startDate, date);
			while (true) {
				LocalDate cmpDate = getRecurDateInMonth(startDate,
						recurMonth);
				if (cmpDate.isEqual(date))
					return true;
				else if (cmpDate.isAfter(date))
					return false;
				recurMonth = recurMonth.plus(period);
			}
		}

		@Override
		public LocalDate findNextDateAfter(LocalDate startDate,
				LocalDate date) {
			Period period = Period.ofMonths(getCount());
			LocalDate recurMonth = findPreviousRecurMonth(startDate, date);
			while (true) {
				LocalDate cmpDate = getRecurDateInMonth(startDate,
						recurMonth);
				if (cmpDate.isAfter(date))
					return cmpDate;
				recurMonth = recurMonth.plus(period);
			}
		}

		private LocalDate findPreviousRecurMonth(LocalDate startDate,
				LocalDate date) {
			LocalDate startMonth = startDate.withDayOfMonth(1);
			LocalDate lastMonth = date.withDayOfMonth(1).minusMonths(1);
			if (lastMonth.isBefore(startMonth))
				return startMonth;
			Period period = Period.ofMonths(getCount());
			int singleCount = (int)ChronoUnit.MONTHS.between(startMonth,
					lastMonth);
			int recurCount = singleCount / getCount();
			return startMonth.plus(period.multipliedBy(recurCount));
		}

		private LocalDate getRecurDateInMonth(LocalDate startDate,
				LocalDate month) {
			return switch (repeatMonthType) {
				case SAME_DAY_OF_WEEK ->
					getRecurDateInMonthSameDayOfWeek(startDate, month);
				case SAME_DATE_NUMBER ->
					getRecurDateInMonthSameDateNumber(startDate, month);
			};
		}

		private LocalDate getRecurDateInMonthSameDayOfWeek(LocalDate startDate,
				LocalDate month) {
			LocalDate startMonth = startDate.withDayOfMonth(1);
			int startDay = startDate.getDayOfWeek().getValue();
			LocalDate first = getFirstDateInMonthWithDayOfWeek(month, startDay);
			LocalDate last = getFirstDateInMonthWithDayOfWeek(
					month.plusMonths(1), startDay).minusWeeks(1);
			int num = (int)ChronoUnit.WEEKS.between(startMonth, startDate);
			LocalDate recurDate = first.plusDays(7L * num);
			if (recurDate.isAfter(last))
				return last;
			return recurDate;
		}

		private LocalDate getFirstDateInMonthWithDayOfWeek(LocalDate month,
				int dayOfWeek) {
			int addDays = (7 + dayOfWeek - month.getDayOfWeek().getValue()) % 7;
			return month.plusDays(addDays);
		}

		private LocalDate getRecurDateInMonthSameDateNumber(LocalDate startDate,
				LocalDate month) {
			return month.withDayOfMonth(startDate.getDayOfMonth());
		}
	}

	public enum RecurMonthType {
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
	public static class RecurYear extends RecurDate {
		public RecurYear() {
			super(RepeatType.YEAR);
		}

		public RecurYear(int count) {
			super(RepeatType.YEAR, count);
		}

		@Override
		public LocalDate findLatestDateBefore(LocalDate startDate,
				LocalDate date) {
			Period period = Period.ofYears(getCount());
			int singleCount = (int)ChronoUnit.YEARS.between(startDate,
					date.minusDays(1));
			int recurCount = singleCount / getCount();
			return startDate.plus(period.multipliedBy(recurCount));
		}

		@Override
		public boolean occursAtDate(LocalDate startDate,
				LocalDate date) {
			Period period = Period.ofYears(getCount());
			int singleCount = (int)ChronoUnit.YEARS.between(startDate, date);
			int recurCount = singleCount / getCount();
			LocalDate scheduleDate = startDate.plus(period.multipliedBy(
					recurCount));
			return scheduleDate.isEqual(date);
		}

		@Override
		public LocalDate findNextDateAfter(LocalDate startDate,
				LocalDate date) {
			Period period = Period.ofYears(getCount());
			int singleCount = (int)ChronoUnit.YEARS.between(startDate,
					date.plus(period));
			int recurCount = singleCount / getCount();
			return startDate.plus(period.multipliedBy(recurCount));
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
				case DAY -> RecurDay.class;
				case WEEK -> RecurWeek.class;
				case MONTH -> RecurMonth.class;
				case YEAR ->  RecurYear.class;
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
