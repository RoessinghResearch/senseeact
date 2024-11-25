class LoginPage {
	/**
	 * Properties:
	 *
	 * - _emailEdit (IconTextEdit)
	 * - _passwordEdit (IconTextEdit)
	 * - _mfaVerifyCodeEdit (NumCodeEdit)
	 * - _mfaVerifyRunning (boolean)
	 * - _button (jQuery element)
	 * - _error (jQuery element)
	 */
	constructor() {
		this._createView();
		this._registerEvents();
	}

	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();

		let emailEdit = new IconTextEdit($('#email-edit'));
		this._emailEdit = emailEdit;
		emailEdit.placeholder = i18next.t('email_address');
		emailEdit.icon = basePath + '/images/icon_user.svg';
		emailEdit.render();
		emailEdit.textInput.attr('type', 'email');

		let passwordEdit = new IconTextEdit($('#password-edit'));
		this._passwordEdit = passwordEdit;
		passwordEdit.placeholder = i18next.t('password');
		passwordEdit.icon = basePath + '/images/icon_password.svg';
		passwordEdit.render();
		passwordEdit.textInput.attr('type', 'password');

		let mfaVerifyCodeEdit = new NumCodeEdit($('#mfa-verify-code-edit'));
		this._mfaVerifyCodeEdit = mfaVerifyCodeEdit;
		mfaVerifyCodeEdit.render();
		this._mfaVerifyRunning = false;
		
		let error = $('#login-error');
		this._error = error;
		error.hide();

		let button = $('#button');
		this._button = button;
		button.prop('disabled', true);

		$(document.body).addClass('tinted-background');
		$('#content').css('visibility', 'visible');
		emailEdit.textInput.trigger('focus');
	}

	_registerEvents() {
		let emailInput = this._emailEdit.textInput;
		var self = this;
		emailInput.on('input', function() {
			self._updateButtonEnabled();
		});
		let passwordInput = this._passwordEdit.textInput;
		passwordInput.on('input', function() {
			self._updateButtonEnabled();
		});
		let button = this._button;
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click',
			function(clickId) {
				self._onLoginClick(clickId);
			},
			function(result) {
				self._onLoginCompleted(result);
			}
		);
	}

	_onLoginClick(clickId) {
		this._error.hide();
		let client = new SenSeeActClient();
		let email = this._emailEdit.textInput.val().trim();
		let password = this._passwordEdit.textInput.val();
		var self = this;
		client.login(email, password)
			.done(function(result) {
				self._onLoginDone(clickId, result);
			})
			.fail(function(xhr, status, error) {
				self._onLoginFail(clickId, xhr, status, error);
			});
	}

	_onLoginDone(clickId, result) {
		animator.onAnimatedClickHandlerCompleted(clickId, {
			success: true,
			result: result
		});
	}

	_onLoginFail(clickId, xhr, status, error) {
		let message = 'unexpected_error';
		if (xhr.status == 401 && xhr.responseJSON) {
			let code = xhr.responseJSON.code;
			if (code == 'INVALID_CREDENTIALS') {
				message = 'invalid_credentials';
			} else if (code == 'ACCOUNT_INACTIVE') {
				message = 'account_inactive';
			} else if (code == 'ACCOUNT_BLOCKED') {
				message = 'account_blocked';
			}
		}
		animator.onAnimatedClickHandlerCompleted(clickId, {
			success: false,
			error: message
		});
	}

	_onLoginCompleted(result) {
		if (result.success && result.result.status == 'COMPLETE') {
			redirectOnLogin();
		} else if (result.success) {
			this._showMfaLogin(result.result.mfaRecord);
		} else if (result.error == 'unexpected_error') {
			showToast(i18next.t(result.error));
		} else {
			this._error.text(i18next.t(result.error))
			this._error.show();
		}
	}

	_showMfaLogin(mfaRecord) {
		$('#factor1').hide();
		$('#factor2').show();
		this._mfaVerifyCodeEdit.focus();
		let button = this._button;
		var self = this;
		let textDiv = $('#mfa-verify-code-text');
		textDiv.text(i18next.t('mfa_verify_' + mfaRecord.type + '_instruct'));
		let mfaVerifyCodeEdit = this._mfaVerifyCodeEdit;
		mfaVerifyCodeEdit.onenter((edit, code) => {
			self._onMfaVerifyCodeEnter(mfaRecord.id, code);
		});
		animator.clearAnimatedClickHandler(button);
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click',
			(clickId) => {
				self._onMfaLoginClick(clickId, mfaRecord.id);
			},
			(result) => {
				self._onMfaLoginClickCallback(result);
			}
		);
	}

	_onMfaVerifyCodeEnter(mfaId, code) {
		this._runMfaVerify(null, mfaId, code);
	}

	_onMfaLoginClick(clickId, mfaId) {
		let codeEdit = this._mfaVerifyCodeEdit;
		let code = codeEdit.code;
		if (!code) {
			animator.onAnimatedClickHandlerCompleted(clickId, {
				status: 'error',
				error: 'invalid_code'
			});
			return;
		}
		this._runMfaVerify(clickId, mfaId, code);
	}

	_runMfaVerify(clickId, mfaId, code) {
		if (this._mfaVerifyRunning) {
			if (clickId) {
				animator.onAnimatedClickHandlerCompleted(clickId, {
					status: 'cancelled',
				});
			}
			return;
		}
		this._error.hide();
		this._mfaVerifyCodeEdit.hideError();
		this._mfaVerifyRunning = true;
		var self = this;
		let url = servicePath + '/auth/mfa/verify';
		let data = {
			'mfaId': mfaId,
			'code': code
		};
		$.ajax({
			method: 'POST',
			url: url,
			data: JSON.stringify(data),
			contentType: 'application/json'
		})
		.done((result) => {
			self._onMfaVerifyDone(clickId);
		})
		.fail((xhr, status, error) => {
			self._onMfaVerifyFail(clickId, xhr);
		});
	}

	_onMfaVerifyDone(clickId) {
		this._mfaVerifyRunning = false;
		if (clickId) {
			animator.onAnimatedClickHandlerCompleted(clickId, {
				status: 'success'
			});
		} else {
			this._handleMfaVerifyDone();
		}
	}

	_onMfaVerifyFail(clickId, xhr) {
		this._mfaVerifyRunning = false;
		if (clickId) {
			animator.onAnimatedClickHandlerCompleted(clickId, {
				status: 'error',
				error: xhr
			});
		} else {
			this._handleMfaVerifyFail(xhr);
		}
	}

	_onMfaLoginClickCallback(result) {
		if (result.status == 'success')
			this._handleMfaVerifyDone();
		else if (result.status == 'error')
			this._handleMfaVerifyFail(result.error);
	}

	_handleMfaVerifyDone() {
		redirectOnLogin();
	}

	_handleMfaVerifyFail(error) {
		let client = new SenSeeActClient();
		if (error == 'invalid_code' || client.hasInvalidInputField(error,
				'code')) {
			this._mfaVerifyCodeEdit.showError();
		} else if (client.hasErrorCode('AUTH_MFA_VERIFY_MAX')) {
			this._error.show();
			this._error.text(i18next.t('mfa_error_verify_max'));
		} else {
			showToast(i18next.t('unexpected_error'));
		}
	}

	_updateButtonEnabled() {
		let email = this._emailEdit.textInput.val().trim();
		let password = this._passwordEdit.textInput.val();
		let hasInput = email.length != 0 && password.length != 0;
		let button = this._button;
		button.prop('disabled', !hasInput);
	}
}

new LoginPage();
