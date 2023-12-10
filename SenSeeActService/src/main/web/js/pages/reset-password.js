class ResetPasswordPage {
	/**
	 * Properties:
	 *
	 * - _email
	 * - _code
	 *
	 * - _passwordEdit (PasswordEdit)
	 * - _repeatPasswordEdit (PasswordEdit)
	 * - _button (jQuery element)
	 * - _error (jQuery element)
	 */
	
	constructor() {
		if (!this._readInput())
			return;
		this._createView();
		this._registerEvents();
	}
	
	_readInput() {
		let params = parseURL(window.location.href).params;
		let email = params['email'];
		let code = params['code'];
		if (!email || !code) {
			window.location.href = basePath + '/';
			return false;
		}
		this._email = email;
		this._code = code;
		return true;
	}
	
	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();

		let passwordEdit = new PasswordEdit($('#password-edit'));
		this._passwordEdit = passwordEdit;
		passwordEdit.placeholder = i18next.t('password');
		passwordEdit.render();

		let repeatPasswordEdit = new PasswordEdit($('#repeat-password-edit'));
		this._repeatPasswordEdit = repeatPasswordEdit;
		repeatPasswordEdit.placeholder = i18next.t('repeat_password');
		repeatPasswordEdit.render();
		
		let error = $('#password-error');
		this._error = error;
		error.hide();

		let button = $('#button');
		this._button = button;
		button.prop('disabled', true);

		$(document.body).addClass('tinted-background');
		$('#content').show();
		passwordEdit.textInput.focus();
	}

	_registerEvents() {
		let passwordInput = this._passwordEdit.textInput;
		var self = this;
		passwordInput.on('input', function() {
			self._updateButtonEnabled();
		});
		let repeatPasswordInput = this._repeatPasswordEdit.textInput;
		repeatPasswordInput.on('input', function() {
			self._updateButtonEnabled();
		});
		let button = this._button;
		animator.addAnimatedClickHandler(button, button, 'animate-button-click',
			function(clickId) {
				self._onSaveClick(clickId);
			},
			function(result) {
				self._onSaveCompleted(result);
			}
		);
	}
	
	_onSaveClick(clickId) {
		this._error.hide();
		let password = this._passwordEdit.textInput.val();
		let repeatPassword = this._repeatPasswordEdit.textInput.val();
		let error = null;
		if (password.length < 6)
			error = 'password_too_short';
		else if (password != repeatPassword)
			error = 'no_repeat_password_match';
		if (error) {
			let result = {
				error: error
			};
			animator.onAnimatedClickHandlerCompleted(clickId, result);
			return;
		}
		var self = this;
		$.post(servicePath + '/auth/reset-password',
			{
				email: this._email,
				code: this._code,
				password: password
			}
		).done(function() {
			self._onSaveSuccess(clickId);
		}).fail(function(xhr, status, error) {
			self._onSaveError(clickId, xhr, status, error);
		});
	}
	
	_onSaveSuccess(clickId) {
		animator.onAnimatedClickHandlerCompleted(clickId, {});
	}
	
	_onSaveError(clickId, xhr, status, error) {
		let result = {};
		if (this._isInvalidInput(xhr)) {
			result.invalidInput = true;
		} else {
			result.error = 'unexpected_error';
		}
		animator.onAnimatedClickHandlerCompleted(clickId, result);
	}
	
	_isInvalidInput(xhr) {
		if (xhr.status != 400)
			return false;
		let response = xhr.responseJSON;
		if (!response || typeof response !== 'object')
			return false;
		return response.code == 'INVALID_INPUT';
	}
	
	_onSaveCompleted(result) {
		if (result.error) {
			this._error.text(i18next.t(result.error));
			this._error.show();
		} else if (result.invalidInput) {
			window.location.href = basePath + '/reset-password-failed';
		} else {
			window.location.href = basePath + '/reset-password-completed';
		}
	}
	
	_updateButtonEnabled() {
		let password = this._passwordEdit.textInput.val();
		let repeatPassword = this._repeatPasswordEdit.textInput.val();
		let hasInput = password.length > 0 && repeatPassword.length > 0;
		let button = this._button;
		button.prop('disabled', !hasInput);
	}
}

new ResetPasswordPage();
