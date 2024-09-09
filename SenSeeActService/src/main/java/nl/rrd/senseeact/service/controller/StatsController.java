package nl.rrd.senseeact.service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.ParseException;
import io.swagger.v3.oas.annotations.Parameter;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.HttpFieldError;
import nl.rrd.senseeact.client.model.NullableResponse;
import nl.rrd.senseeact.client.model.Role;
import nl.rrd.senseeact.client.model.SystemStat;
import nl.rrd.senseeact.client.model.SystemStatTable;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.dao.DatabaseSort;
import nl.rrd.senseeact.service.QueryRunner;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.ForbiddenException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v{version}/stats")
public class StatsController {
	
	@RequestMapping(value="/{statName}/latest", method=RequestMethod.GET)
	public NullableResponse<SystemStat> getLatestSystemStat(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("statName")
			String statName) throws HttpException, Exception {
		SystemStat stat = QueryRunner.runAuthQuery(
				(version, authDb, user, authDetails) ->
				doGetSystemStatLatest(authDb, user, statName),
				versionName, request, response);
		return new NullableResponse<>(stat);
	}

	@RequestMapping(value="/{statName}", method=RequestMethod.GET)
	public List<SystemStat> getSystemStats(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("statName")
			String statName,
			@RequestParam(value="start", required=false, defaultValue="")
			String start,
			@RequestParam(value="end", required=false, defaultValue="")
			String end) throws HttpException, Exception {
		return QueryRunner.runAuthQuery(
				(version, authDb, user, authDetails) ->
				doGetSystemStats(authDb, user, statName, start, end),
				versionName, request, response);
	}
	
	private SystemStat doGetSystemStatLatest(Database authDb, User user,
			String statName) throws HttpException, DatabaseException {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		DatabaseCriteria criteria = new DatabaseCriteria.Equal("name",
				statName);
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("utcTime", false)
		};
		return authDb.selectOne(new SystemStatTable(), criteria, sort);
	}

	private List<SystemStat> doGetSystemStats(Database authDb, User user,
			String statName, String start, String end) throws HttpException,
			DatabaseException {
		if (user.getRole() != Role.ADMIN)
			throw new ForbiddenException();
		StringBuilder errorBuilder = new StringBuilder();
		List<HttpFieldError> fieldErrors = new ArrayList<>();
		List<DatabaseCriteria> andCriteria = new ArrayList<>();
		andCriteria.add(new DatabaseCriteria.Equal("name", statName));
		if (start != null && !start.isEmpty()) {
			try {
				ZonedDateTime startTime = DateTimeUtils.parseDateTime(start,
						ZonedDateTime.class);
				andCriteria.add(new DatabaseCriteria.GreaterEqual("utcTime",
						startTime.toInstant().toEpochMilli()));
			} catch (ParseException ex) {
				if (!errorBuilder.isEmpty())
					errorBuilder.append("\n");
				errorBuilder.append("Invalid value for parameter \"start\": " +
					ex.getMessage());
				fieldErrors.add(new HttpFieldError("start", ex.getMessage()));
			}
		}
		if (end != null && !end.isEmpty()) {
			try {
				ZonedDateTime endTime = DateTimeUtils.parseDateTime(end,
						ZonedDateTime.class);
				andCriteria.add(new DatabaseCriteria.LessThan("utcTime",
						endTime.toInstant().toEpochMilli()));
			} catch (ParseException ex) {
				if (!errorBuilder.isEmpty())
					errorBuilder.append("\n");
				errorBuilder.append("Invalid value for parameter \"end\": " +
					ex.getMessage());
				fieldErrors.add(new HttpFieldError("end", ex.getMessage()));
			}
		}
		if (!fieldErrors.isEmpty()) {
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT,
					errorBuilder.toString());
			error.setFieldErrors(fieldErrors);
			throw new BadRequestException(error);
		}
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				andCriteria.toArray(new DatabaseCriteria[0]));
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("utcTime", true)
		};
		return authDb.select(new SystemStatTable(), criteria, 0, sort);
	}
}
