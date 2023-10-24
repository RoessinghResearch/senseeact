package nl.rrd.senseeact.service.model;

import nl.rrd.senseeact.client.model.sample.Sample;
import nl.rrd.senseeact.dao.DatabaseObject;
import nl.rrd.senseeact.service.exception.ForbiddenException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class ProjectUserAccess {
	private User user;
	private LocalDate startDate;
	private LocalDate endDate;

	public ProjectUserAccess(User user, LocalDate startDate,
			LocalDate endDate) {
		this.user = user;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	/**
	 * Returns the user.
	 *
	 * @return the user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Returns the start date (inclusive) of the range that can be accessed.
	 * If any start date can be accessed, this method returns null.
	 *
	 * @return the start date (inclusive) or null
	 */
	public LocalDate getStartDate() {
		return startDate;
	}

	/**
	 * Returns the end date (exclusive) of the range that can be accessed.
	 * If any end date can be accessed, this method returns null.
	 *
	 * @return the end date (exclusive) or null
	 */
	public LocalDate getEndDate() {
		return endDate;
	}

	/**
	 * Checks whether the specified database object matches the date range
	 * restriction. If no access is granted, this method throws a
	 * ForbiddenException.
	 *
	 * @param dbObject the database object
	 * @throws ForbiddenException if no access is granted
	 */
	public void checkMatchesRange(DatabaseObject dbObject)
			throws ForbiddenException {
		if (!(dbObject instanceof Sample))
			return;
		Sample sample = (Sample)dbObject;
		LocalDateTime sampleTime = sample.toLocalDateTime();
		try {
			checkMatchesRange(sampleTime, sampleTime.plus(1,
					ChronoUnit.MILLIS));
		} catch (ForbiddenException ex) {
			throw new ForbiddenException("Sample outside granted time range");
		}
	}

	/**
	 * Checks whether the specified request range matches the range
	 * restriction. If no access is granted for the request range, this
	 * method throws a ForbiddenException.
	 *
	 * @param rangeStart the start of the request range (inclusive) or null
	 * @param rangeEnd the end of the request range (exclusive) or null
	 * @throws ForbiddenException if no access is granted for the specified
	 * time range
	 */
	public void checkMatchesRange(LocalDateTime rangeStart,
			LocalDateTime rangeEnd) throws ForbiddenException {
		LocalTime dayStart = LocalTime.of(0, 0, 0);
		if (startDate != null) {
			LocalDateTime restrictStart = startDate.atTime(dayStart);
			if (rangeStart == null || rangeStart.isBefore(restrictStart)) {
				throw new ForbiddenException("Access forbidden for " +
						getLogRangeString(rangeStart, rangeEnd));
			}
		}
		if (endDate != null) {
			LocalDateTime restrictEnd = endDate.atTime(dayStart);
			if (rangeEnd == null || rangeEnd.isAfter(restrictEnd)) {
				throw new ForbiddenException("Access forbidden for " +
						getLogRangeString(rangeStart, rangeEnd));
			}
		}
	}

	private String getLogRangeString(LocalDateTime rangeStart,
			LocalDateTime rangeEnd) {
		if (rangeStart == null && rangeEnd == null) {
			return "unrestricted time range";
		} else if (rangeStart == null) {
			return String.format("time range until %s",
					rangeEnd.format(Sample.LOCAL_TIME_FORMAT));
		} else if (rangeEnd == null) {
			return String.format("time range from %s",
					rangeStart.format(Sample.LOCAL_TIME_FORMAT));
		} else {
			return String.format("time range from %s to %s",
					rangeStart.format(Sample.LOCAL_TIME_FORMAT),
					rangeEnd.format(Sample.LOCAL_TIME_FORMAT));
		}
	}
}
