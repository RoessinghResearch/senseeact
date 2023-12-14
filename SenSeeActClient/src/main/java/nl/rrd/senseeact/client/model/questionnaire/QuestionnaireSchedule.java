package nl.rrd.senseeact.client.model.questionnaire;

import nl.rrd.utils.datetime.DateTimeUtils;
import org.xml.sax.Attributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.schedule.DateDuration;
import nl.rrd.utils.schedule.TimeDuration;
import nl.rrd.utils.schedule.TimeUnit;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXHandler;

/**
 * This class defines a schedule for a questionnaire. When you create a
 * schedule, as a minimum you should call {@link #setQuestionnaireId(String)
 * setQuestionnaireId()}, {@link #setStartDate(LocalDate) setStartDate()} and
 * {@link #setStartTime(LocalTime) setStartTime()}.
 *
 * @author Dennis Hofs (RRD)
 */
public class QuestionnaireSchedule {
	private String questionnaireId;
	private LocalDate startDate;
	private LocalDate endDate = null;
	private DateDuration recurDate = null;
	private LocalTime startTime;
	private LocalTime endTime = null;
	private TimeDuration recurTime = null;
	private TimeDuration duration = null;
	private TimeDuration notifyDelay = null;
	private boolean canScheduleFirstImmediately = true;

	/**
	 * Returns the questionnaire ID.
	 *
	 * @return the questionnaire ID
	 */
	public String getQuestionnaireId() {
		return questionnaireId;
	}

	/**
	 * Sets the questionnaire ID.
	 *
	 * @param questionnaireId the questionnaire ID
	 */
	public void setQuestionnaireId(String questionnaireId) {
		this.questionnaireId = questionnaireId;
	}

	/**
	 * Returns the start date when the questionnaire should be scheduled.
	 *
	 * @return the start date
	 */
	public LocalDate getStartDate() {
		return startDate;
	}

	/**
	 * Sets the start date when the questionnaire should be scheduled.
	 *
	 * @param startDate the start date
	 */
	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	/**
	 * Returns the end date, if any. If set, the questionnaire will only be
	 * scheduled before this date, and not at or after this date. This is mainly
	 * useful in combination with {@link #getRecurDate() getRecurDate()}.
	 *
	 * @return the end date or null (default)
	 */
	public LocalDate getEndDate() {
		return endDate;
	}

	/**
	 * Sets the end date, if any. If set, the questionnaire will only be
	 * scheduled before this date, and not at or after this date. This is mainly
	 * useful in combination with {@link #setRecurDate(DateDuration)
	 * setRecurDate()}.
	 *
	 * @param endDate the end date or null (default)
	 */
	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	/**
	 * Returns the recur interval of days when the questionnaire should be
	 * repeated. If set, the questionnaire is scheduled at the start date and
	 * every interval after that, until the end date. If the recur interval is
	 * null, it will not be repeated after the start date.
	 *
	 * @return the recur date interval or null (default)
	 */
	public DateDuration getRecurDate() {
		return recurDate;
	}

	/**
	 * Sets the recur interval of days when the questionnaire should be
	 * repeated. If set, the questionnaire is scheduled at the start date and
	 * every interval after that, until the end date. If the recur interval is
	 * null, it will not be repeated after the start date.
	 *
	 * @param recurDate the recur date interval or null (default)
	 */
	public void setRecurDate(DateDuration recurDate) {
		this.recurDate = recurDate;
	}

	/**
	 * Returns the start time when the questionnaire should be scheduled on each
	 * scheduled date.
	 *
	 * @return the start time
	 */
	public LocalTime getStartTime() {
		return startTime;
	}

	/**
	 * Sets the start time when the questionnaire should be scheduled on each
	 * scheduled date.
	 *
	 * @param startTime the start time
	 */
	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	/**
	 * Returns the end time, if any. If set, the questionnaire will only be
	 * scheduled before this time, and not at or after this time. This is mainly
	 * useful in combination with {@link #getRecurTime() getRecurTime()}.
	 *
	 * @return the end time or null (default)
	 */
	public LocalTime getEndTime() {
		return endTime;
	}

	/**
	 * Returns the end time, if any. If set, the questionnaire will only be
	 * scheduled before this time, and not at or after this time. This is mainly
	 * useful in combination with {@link #setRecurTime(TimeDuration)
	 * setRecurTime(TimeDuration)}.
	 *
	 * @param endTime the end time or null (default)
	 */
	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	/**
	 * Returns the recur time interval when the questionnaire should be repeated
	 * within a scheduled date. If set, the questionnaire is scheduled at the
	 * start time and every interval after that, until the end time. If the
	 * recur interval is null, it will not be repeated within a scheduled date.
	 *
	 * @return the recur time interval or null (default)
	 */
	public TimeDuration getRecurTime() {
		return recurTime;
	}

	/**
	 * Sets the recur time interval when the questionnaire should be repeated
	 * within a scheduled date. If set, the questionnaire is scheduled at the
	 * start time and every interval after that, until the end time. If the
	 * recur interval is null, it will not be repeated within a scheduled date.
	 *
	 * @param recurTime the recur time interval or null (default)
	 */
	public void setRecurTime(TimeDuration recurTime) {
		this.recurTime = recurTime;
	}

	/**
	 * Returns the duration that the questionnaire should be available after the
	 * scheduled start. If the duration is null, the questionnaire will be
	 * available until the next scheduled occurrence of the same questionnaire.
	 *
	 * @return the duration or null (default)
	 */
	public TimeDuration getDuration() {
		return duration;
	}

	/**
	 * Sets the duration that the questionnaire should be available after the
	 * scheduled start. If the duration is null, the questionnaire will be
	 * available until the next scheduled occurrence of the same questionnaire.
	 *
	 * @param duration the duration or null (default)
	 */
	public void setDuration(TimeDuration duration) {
		this.duration = duration;
	}

	/**
	 * Returns the time when a notification should be played after the scheduled
	 * start of the questionnaire. If this is null, then no notification will
	 * be played at all. If set, the notification delay must be shorter than
	 * {@link #getDuration() getDuration()}.
	 *
	 * @return the notification delay or null (default)
	 */
	public TimeDuration getNotifyDelay() {
		return notifyDelay;
	}

	/**
	 * Sets the time when a notification should be played after the scheduled
	 * start of the questionnaire. If this is null, then no notification will
	 * be played at all. If set, the notification delay must be shorter than
	 * {@link #setDuration(TimeDuration) setDuration()}.
	 *
	 * @param notifyDelay the notification delay or null (default)
	 */
	public void setNotifyDelay(TimeDuration notifyDelay) {
		this.notifyDelay = notifyDelay;
	}

	/**
	 * Returns whether the first instance of this questionnaire can be scheduled
	 * immediately when the user first logs in, even if the start time is before
	 * the current time (it should still end after the current time). If not,
	 * the app should wait until the first start time is reached. The default
	 * is true.
	 *
	 * @return true if the first questionnaire can be scheduled immediately
	 * (default), false if it should wait until the first start time is reached
	 */
	public boolean isCanScheduleFirstImmediately() {
		return canScheduleFirstImmediately;
	}

	/**
	 * Sets whether the first instance of this questionnaire can be scheduled
	 * immediately when the user first logs in, even if the start time is before
	 * the current time (it should still end after the current time). If not,
	 * the app should wait until the first start time is reached. The default is
	 * true.
	 *
	 * @param canScheduleFirstImmediately true if the first questionnaire can be
	 * scheduled immediately (default), false if it should wait until the first
	 * start time is reached
	 */
	public void setCanScheduleFirstImmediately(
			boolean canScheduleFirstImmediately) {
		this.canScheduleFirstImmediately = canScheduleFirstImmediately;
	}

	public static SimpleSAXHandler<QuestionnaireSchedule> getXMLHandler() {
		return new XMLHandler();
	}

	private static class XMLHandler extends
			AbstractSimpleSAXHandler<QuestionnaireSchedule> {
		private QuestionnaireSchedule schedule = new QuestionnaireSchedule();
		private int rootLevel = -1;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (rootLevel == -1)
				rootLevel = parents.size();
			if (parents.size() == rootLevel) {
				parseSchedule(atts);
			} else {
				throw new ParseException("Unexpected element: " + name);
			}
		}

		private void parseSchedule(Attributes atts) throws ParseException {
			schedule.questionnaireId = readAttribute(atts, "questionnaire",
					1, null);
			String s = readAttribute(atts, "start_date");
			schedule.startDate = parseDate("start_date", s);
			s = atts.getValue("end_date");
			if (s != null)
				schedule.endDate = parseDate("end_date", s);
			s = atts.getValue("recur_date");
			if (s != null)
				schedule.recurDate = DateDuration.parse(s, null, null);
			s = readAttribute(atts, "start_time");
			schedule.startTime = parseTime("start_time", s);
			s = atts.getValue("end_time");
			if (s != null)
				schedule.endTime = parseTime("end_time", s);
			s = atts.getValue("recur_time");
			if (s != null) {
				schedule.recurTime = TimeDuration.parse(s, TimeUnit.SECOND,
						null);
			}
			s = atts.getValue("duration");
			if (s != null) {
				schedule.duration = TimeDuration.parse(s, TimeUnit.SECOND,
						null);
			}
			s = atts.getValue("notify_delay");
			if (s != null) {
				schedule.notifyDelay = TimeDuration.parse(s, TimeUnit.SECOND,
						null);
			}
			if (atts.getValue("can_schedule_first_immediately") != null) {
				schedule.canScheduleFirstImmediately = readBooleanAttribute(
						atts, "can_schedule_first_immediately");
			}
		}

		private LocalDate parseDate(String attr, String value)
				throws ParseException {
			DateTimeFormatter parser = DateTimeFormatter.ofPattern(
					"yyyy-MM-dd");
			try {
				return parser.parse(value, LocalDate::from);
			} catch (IllegalArgumentException ex) {
				throw new ParseException("Invalid value for attribute \"" +
						attr + "\": " + value, ex);
			}
		}

		private LocalTime parseTime(String attr, String value)
				throws ParseException {
			if (!value.matches("[0-9]+:[0-9]{2}(:[0-9]{2})?")) {
				throw new ParseException("Invalid value for attribute \"" +
						attr + "\": " + value);
			}
			try {
				return DateTimeUtils.parseIsoTime(value);
			} catch (ParseException ex) {
				throw new ParseException("Invalid value for attribute \"" +
						attr + "\": " + value, ex);
			}
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
		}

		@Override
		public void characters(String ch, List<String> parents)
				throws ParseException {
			if (!ch.trim().isEmpty())
				throw new ParseException("Unexpected text content");
		}

		@Override
		public QuestionnaireSchedule getObject() {
			return schedule;
		}
	}
}
