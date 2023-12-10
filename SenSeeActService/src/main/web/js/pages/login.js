class LoginPage {
	/**
	 * Properties:
	 *
	 * - _emailEdit (IconTextEdit)
	 * - _passwordEdit (IconTextEdit)
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
		
		let error = $('#login-error');
		this._error = error;
		error.hide();

		let button = $('#button');
		this._button = button;
		button.prop('disabled', true);

		$(document.body).addClass('tinted-background');
		$('#content').css('visibility', 'visible');
		emailEdit.textInput.focus();
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
		animator.addAnimatedClickHandler(button, button, 'animate-button-click',
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
		animator.onAnimatedClickHandlerCompleted(clickId, null);
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
		animator.onAnimatedClickHandlerCompleted(clickId, message);
	}

	_onLoginCompleted(error) {
		if (error == 'unexpected_error') {
			showToast(i18next.t(error));
		} else if (error) {
			this._error.text(i18next.t(error))
			this._error.show();
		} else {
			redirectOnLogin();
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
