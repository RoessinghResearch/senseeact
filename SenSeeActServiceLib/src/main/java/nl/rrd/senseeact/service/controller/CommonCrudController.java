package nl.rrd.senseeact.service.controller;

import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.senseeact.client.model.sample.LocalTimeSample;
import nl.rrd.senseeact.client.model.sample.Sample;
import nl.rrd.senseeact.client.model.sample.UTCSample;
import nl.rrd.senseeact.dao.DatabaseObject;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.HttpException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class CommonCrudController {
	/**
	 * Validates the time fields of a record written by a user. If the data
	 * class of the table is a {@link Sample Sample}, it ensures that
	 * "localTime" is set. If the data class is a {@link UTCSample UTCSample},
	 * it also ensures that "utcTime" and "timezone" are set. For any of these
	 * fields that were set, this method validates the values.
	 *
	 * @param subject the subject user
	 * @param record the record
	 * @param map the original data map contained field utcTime
	 * @throws HttpException if the sample has invalid values
	 */
	public static void validateWriteRecordTime(User subject,
			DatabaseObject record, Map<?,?> map) throws HttpException {
		ZoneId subjectTz = subject.toTimeZone();
		ZonedDateTime now = DateTimeUtils.nowMs(subjectTz);
		boolean hasUtcTime = map.containsKey("utcTime");
		if (record instanceof LocalTimeSample) {
			LocalTimeSample sample = (LocalTimeSample)record;
			if (sample.getLocalTime() != null) {
				DateTimeFormatter parser = Sample.LOCAL_TIME_FORMAT;
				try {
					parser.parse(sample.getLocalTime(), LocalDateTime::from);
				} catch (DateTimeParseException ex) {
					String msg = "Invalid localTime: " + sample.getLocalTime();
					HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
							msg);
					throw new BadRequestException(error);
				}
			} else {
				sample.updateLocalDateTime(now.toLocalDateTime());
			}
		} else if (record instanceof UTCSample) {
			UTCSample sample = (UTCSample)record;
			ZoneId tz = subjectTz;
			if (sample.getTimezone() != null) {
				try {
					tz = ZoneId.of(sample.getTimezone());
				} catch (DateTimeException ex) {
					String msg = String.format("Unknown timezone %s in sample",
							sample.getTimezone()) + ": " + sample;
					HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
					throw new BadRequestException(error);
				}
			}
			if (sample.getLocalTime() != null && hasUtcTime) {
				ZonedDateTime time = ZonedDateTime.ofInstant(
						Instant.ofEpochMilli(sample.getUtcTime()), tz);
				String timeStr = time.format(Sample.LOCAL_TIME_FORMAT);
				if (!timeStr.equals(sample.getLocalTime())) {
					String msg = String.format(
							"utcTime %d (%s in timezone %s) does not match localTime %s",
							sample.getUtcTime(), timeStr,
							sample.getTimezone(), sample.getLocalTime());
					HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
							msg);
					throw new BadRequestException(error);
				}
				sample.updateDateTime(time);
			} else if (sample.getLocalTime() != null) {
				DateTimeFormatter parser = Sample.LOCAL_TIME_FORMAT;
				LocalDateTime localTime;
				try {
					localTime = parser.parse(sample.getLocalTime(),
							LocalDateTime::from);
				} catch (DateTimeParseException ex) {
					String msg = "Invalid localTime: " + sample.getLocalTime();
					HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
							msg);
					throw new BadRequestException(error);
				}
				ZonedDateTime utcTime;
				try {
					utcTime = DateTimeUtils.tryLocalToZonedDateTime(localTime,
							tz);
				} catch (IllegalArgumentException ex) {
					String msg = String.format(
							"Can't get local time %s in timezone %s",
							sample.getLocalTime(), sample.getTimezone());
					HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
							msg);
					throw new BadRequestException(error);
				}
				sample.updateDateTime(utcTime);
			} else if (hasUtcTime) {
				ZonedDateTime time = ZonedDateTime.ofInstant(
						Instant.ofEpochMilli(sample.getUtcTime()), tz);
				sample.updateDateTime(time);
			} else {
				sample.updateDateTime(now);
			}
		}
	}
}
