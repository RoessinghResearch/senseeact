package nl.rrd.senseeact.service.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.model.*;
import nl.rrd.senseeact.service.QueryRunner;
import nl.rrd.senseeact.service.controller.model.ChangePasswordParams;
import nl.rrd.senseeact.service.controller.model.ResetPasswordParams;
import nl.rrd.senseeact.service.exception.HttpException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v{version}/auth")
public class AuthController {
	private AuthControllerExecution exec = new AuthControllerExecution();

	@RequestMapping(value="/signup", method=RequestMethod.POST, consumes={
			MediaType.APPLICATION_JSON_VALUE })
	public Object signup(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestBody
			SignupParams signupParams) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.signup(version, request, response, null, null, null,
							false, false, null, signupParams, authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/signup", method=RequestMethod.POST)
	@Hidden
	public Object signupUrlEncoded(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			String versionName,
			@RequestParam(value="email", required=false, defaultValue="")
			final String email,
			@RequestParam(value="password", required=false, defaultValue="")
			final String password,
			@RequestParam(value="project", required=false, defaultValue="")
			String project,
			@RequestParam(value="cookie", required=false, defaultValue="false")
			boolean cookie,
			@RequestParam(value="autoExtendCookie", required=false, defaultValue="false")
			boolean autoExtendCookie,
			@RequestParam(value="emailTemplate", required=false, defaultValue="")
			String emailTemplate) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.signup(version, request, response, email, password,
							project, cookie, autoExtendCookie, emailTemplate,
							null, authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/signup-temporary-user", method=RequestMethod.POST,
			consumes={ MediaType.APPLICATION_JSON_VALUE })
	public Object signupTemporaryUser(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestBody
			SignupTemporaryUserParams signupParams) throws HttpException,
			Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.signupTemporaryUser(version, request, response, null,
					false, false, signupParams, authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/signup-temporary-user", method=RequestMethod.POST)
	@Hidden
	public Object signupTemporaryUserUrlEncoded(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			String versionName,
			@RequestParam(value="project", required=false, defaultValue="")
			String project,
			@RequestParam(value="cookie", required=false, defaultValue="false")
			boolean cookie,
			@RequestParam(value="autoExtendCookie", required=false, defaultValue="false")
			boolean autoExtendCookie) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.signupTemporaryUser(version, request, response,
							project, cookie, autoExtendCookie, null, authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/request-verify-email", method=RequestMethod.GET)
	public void requestVerifyEmail(HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="template", required=false, defaultValue="")
			String template) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.requestVerifyEmail(request, user, template, authDb),
					versionName, request, response);
		}
	}

	@RequestMapping(value="/verify-email", method=RequestMethod.POST)
	public void verifyEmail(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user")
			String userId,
			@RequestParam(value="code")
			String code) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.verifyEmail(request, userId, code, authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/login", method=RequestMethod.POST, consumes={
			MediaType.APPLICATION_JSON_VALUE })
	public Object login(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestBody
			LoginParams loginParams) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.login(version, request, response, null, null, false,
							false, loginParams, authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/login", method=RequestMethod.POST)
	@Hidden
	public Object loginUrlEncoded(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			String versionName,
			@RequestParam(value="email", required=false, defaultValue="")
			String email,
			@RequestParam(value="password", required=false, defaultValue="")
			String password,
			@RequestParam(value="cookie", required=false, defaultValue="false")
			boolean cookie,
			@RequestParam(value="autoExtendCookie", required=false, defaultValue="false")
			boolean autoExtendCookie) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.login(version, request, response, email, password,
							cookie, autoExtendCookie, null, authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/login-username", method=RequestMethod.POST,
			consumes={ MediaType.APPLICATION_JSON_VALUE })
	public Object loginUsername(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestBody
			LoginUsernameParams loginParams) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.loginUsername(version, request, response, null, null,
							false, false, loginParams, authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/login-username", method=RequestMethod.POST)
	@Hidden
	public Object loginUsernameUrlEncoded(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			String versionName,
			@RequestParam(value="username", required=false, defaultValue="")
			String username,
			@RequestParam(value="password", required=false, defaultValue="")
			String password,
			@RequestParam(value="cookie", required=false, defaultValue="false")
			boolean cookie,
			@RequestParam(value="autoExtendCookie", required=false, defaultValue="false")
			boolean autoExtendCookie) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.loginUsername(version, request, response, username,
							password, cookie, autoExtendCookie, null, authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/login-as", method=RequestMethod.GET)
	public Object loginAs(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="user")
			String asUser) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.loginAs(version, asUser, authDb, user),
					versionName, request, response);
		}
	}

	@RequestMapping(value="/logout", method=RequestMethod.GET)
	public void logout(
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName) throws HttpException, Exception {
		exec.clearAuthTokenCookie(response);
	}
	
	@RequestMapping(value="/change-password", method=RequestMethod.POST,
			consumes={ MediaType.APPLICATION_JSON_VALUE })
	public String changePassword(
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			HttpServletRequest request,
			@RequestBody
			ChangePasswordParams params) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.changePassword(version, request, response, null, null,
							null, false, false, params, authDb, user),
					versionName, request, response);
		}
	}

	@RequestMapping(value="/change-password", method=RequestMethod.POST)
	@Hidden
	public String changePasswordUrlEncoded(
			HttpServletResponse response,
			@PathVariable("version")
			String versionName,
			HttpServletRequest request,
			@RequestParam(value="email", required=false, defaultValue="")
			String email,
			@RequestParam(value="oldPassword", required=false, defaultValue="")
			String oldPassword,
			@RequestParam(value="newPassword", required=false, defaultValue="")
			String newPassword,
			@RequestParam(value="cookie", required=false, defaultValue="false")
			boolean cookie,
			@RequestParam(value="autoExtendCookie", required=false, defaultValue="false")
			boolean autoExtendCookie) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.changePassword(version, request, response, email,
					oldPassword, newPassword, cookie, autoExtendCookie, null,
					authDb, user),
					versionName, request, response);
		}
	}
	
	@RequestMapping(value="/request-reset-password", method=RequestMethod.GET)
	public void requestResetPassword(
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			HttpServletRequest request,
			@RequestParam("email")
			final String email,
			@RequestParam(value="template", required=false, defaultValue="")
			String template) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.requestResetPassword(request, email, template, authDb),
					versionName, null, response);
		}
	}
	
	@RequestMapping(value="/reset-password", method=RequestMethod.POST,
			consumes={ MediaType.APPLICATION_JSON_VALUE })
	public void resetPassword(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestBody
			ResetPasswordParams params) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.resetPassword(request, null, null, null, params,
							authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/reset-password", method=RequestMethod.POST)
	@Hidden
	public void resetPasswordUrlEncoded(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			String versionName,
			@RequestParam(value="email", required=false, defaultValue="")
			String email,
			@RequestParam(value="code", required=false, defaultValue="")
			String resetCode,
			@RequestParam(value="password", required=false, defaultValue="")
			String password) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.resetPassword(request, email, resetCode, password,
							null, authDb),
					versionName, null, response);
		}
	}

	@RequestMapping(value="/mfa/add", method=RequestMethod.POST)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		content = {
			@Content(
				mediaType = "application/json",
				schema = @Schema(type = "string")
			)
		}
	)
	public MfaRecord addMfaRecord(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="type")
			String type) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.addMfaRecord(request, type, authDb, user),
					versionName, request, response);
		}
	}

	@RequestMapping(value="/mfa/add/verify", method=RequestMethod.POST)
	public MfaRecord verifyAddMfaRecord(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="id")
			String id,
			@RequestParam(value="code")
			String code) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.verifyAddMfaRecord(id, code, authDb, user),
					versionName, request, response);
		}
	}

	@RequestMapping(value="/mfa", method=RequestMethod.DELETE)
	public void deleteMfaRecord(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="id")
			String id) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.deleteMfaRecord(id, authDb, user),
					versionName, request, response);
		}
	}

	@RequestMapping(value="/mfa/list", method=RequestMethod.GET)
	public List<MfaRecord> getMfaRecords(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			return QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.getMfaRecords(user),
					versionName, request, response);
		}
	}

	@RequestMapping(value="/mfa/add/totp/qrcode", method=RequestMethod.GET)
	public void getMfaTotpQRCode(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden = true)
			String versionName,
			@RequestParam(value="id")
			String id) throws HttpException, Exception {
		synchronized (AuthControllerExecution.AUTH_LOCK) {
			QueryRunner.runAuthQuery((version, authDb, user) ->
					exec.getMfaTotpQRCode(response, authDb, user, id),
					versionName, request, response);
		}
	}
}
