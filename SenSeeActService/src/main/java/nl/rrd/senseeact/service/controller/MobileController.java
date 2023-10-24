package nl.rrd.senseeact.service.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.service.QueryRunner;
import nl.rrd.senseeact.service.exception.HttpException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v{version}/mobile")
public class MobileController {
	private MobileControllerExecution exec = new MobileControllerExecution();

	@RequestMapping(value="/log/{app}", method=RequestMethod.POST)
	public void writeMobileLogForAppCode(
			final HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@PathVariable("app")
			final String app,
			@RequestParam(value="device", required=false, defaultValue="")
			final String device,
			@RequestParam(value="date")
			@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
			final LocalDate date,
			@RequestParam(value="position")
			final long position,
			@RequestParam(value="zip", defaultValue="false")
			final boolean zip) throws HttpException, Exception {
		QueryRunner.runAuthQuery((version, authDb, user) ->
				exec.writeMobileLogForAppCode(request, app, device, date,
						position, zip, user),
				versionName, request, response);
	}

	@RequestMapping(value="/wake/register", method=RequestMethod.POST)
	public void registerMobileWakeRequest(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="deviceId")
			String deviceId,
			@RequestParam(value="fcmToken")
			String fcmToken,
			@RequestParam(value="interval")
			int interval) throws HttpException, Exception {
		QueryRunner.runAuthQuery((version, authDb, user) ->
				exec.registerMobileWakeRequest(version, authDb, user, subject,
						deviceId, fcmToken, interval),
				versionName, request, response);
	}

	@RequestMapping(value="/wake/unregister", method=RequestMethod.POST)
	public void unregisterMobileWakeRequest(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user", required=false, defaultValue="")
			String subject,
			@RequestParam(value="deviceId")
			String deviceId) throws HttpException, Exception {
		QueryRunner.runAuthQuery((version, authDb, user) ->
				exec.unregisterMobileWakeRequest(version, authDb, user, subject,
						deviceId),
				versionName, request, response);
	}
}
