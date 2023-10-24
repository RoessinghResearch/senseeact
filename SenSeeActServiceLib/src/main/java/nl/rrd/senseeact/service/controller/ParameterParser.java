package nl.rrd.senseeact.service.controller;

import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.ParseException;

import java.time.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterParser {

	/**
	 * Tries to parse the specified date or time string as a select sample
	 * parameter, depending on the type of sample table. Below are the possible
	 * formats for the date/time string:
	 * 
	 * <p><b>ISO string with date, time and time zone</b></p>
	 * 
	 * <p>If isUtcTable is true, then it returns a {@link ZonedDateTime
	 * ZonedDateTime} object. If isUtcTable is false, it returns a {@link
	 * LocalDateTime LocalDateTime} object, ignoring the time zone.</p>
	 * 
	 * <p><b>ISO string with date and time, without time zone</b></p>
	 * 
	 * <p>It returns a {@link LocalDateTime LocalDateTime} object.</p>
	 * 
	 * <p><b>Long value</b></p>
	 * 
	 * <p>The long value is taken as a unix time and converted to a {@link
	 * ZonedDateTime ZonedDateTime} object in the current time zone. If
	 * isUtcTable is true, it returns the {@link ZonedDateTime ZonedDateTime}
	 * object. Otherwise it returns a {@link LocalDateTime LocalDateTime} object
	 * for time zone UTC.</p>
	 * 
	 * <p><b>SQL date</b></p>
	 * 
	 * <p>This method returns a {@link LocalDateTime LocalDateTime} object
	 * with time 00:00:00.000.</p>
	 * 
	 * <p>If the string does not match one of these formats, it will throw a
	 * {@link ParseException ParseException}.</p>
	 * 
	 * <p>If this method returns a {@link ZonedDateTime ZonedDateTime} object,
	 * it will be matched against field "utcTime" (only available if isUtcTable
	 * is true). If it returns a {@link LocalDateTime LocalDateTime} object, it
	 * will be matched against field "localTime".</p>
	 * 
	 * @param isUtcTable true if the table has field "utcTime", false otherwise
	 * @param dateTimeStr the date/time string
	 * @return a {@link ZonedDateTime ZonedDateTime} or {@link LocalDateTime
	 * LocalDateTime}
	 * @throws ParseException if the date/time string is invalid
	 */
	public Object parseSelectDateTime(boolean isUtcTable, String dateTimeStr)
			throws ParseException {
		int timeSep = dateTimeStr.indexOf('T');
		boolean hasTimeZone = false;
		if (timeSep != -1) {
			String timePart = dateTimeStr.substring(timeSep + 1);
			Pattern tzRegex = Pattern.compile("[Z+-]");
			Matcher m = tzRegex.matcher(timePart);
			hasTimeZone = m.find();
		}
		if (timeSep != -1 && hasTimeZone) {
			try {
				if (isUtcTable) {
					return DateTimeUtils.parseIsoDateTime(dateTimeStr,
							ZonedDateTime.class);
				} else {
					return DateTimeUtils.parseIsoDateTime(dateTimeStr,
							LocalDateTime.class);
				}
			} catch (ParseException ex) {
				throw new ParseException("Invalid date/time value: " +
						dateTimeStr);
			}
		}
		if (timeSep != -1) {
			try {
				return DateTimeUtils.parseLocalIsoDateTime(dateTimeStr,
						LocalDateTime.class);
			} catch (ParseException ex) {
				throw new ParseException("Invalid date/time value: " +
						dateTimeStr);
			}
		}
		ZonedDateTime utcTime;
		try {
			utcTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(
					Long.parseLong(dateTimeStr)), ZoneOffset.UTC);
		} catch (NumberFormatException ex) {
			utcTime = null;
		}
		if (utcTime != null) {
			if (isUtcTable)
				return utcTime;
			else
				return utcTime.toLocalDateTime();
		}
		try {
			LocalDate date = DateTimeUtils.parseDate(dateTimeStr);
			return date.atTime(0, 0, 0);
		} catch (ParseException ex) {
			throw new ParseException("Invalid date/time value: " + dateTimeStr);
		}
	}
}
