class VerifyEmailPage {
	/**
	 * Properties:
	 *
	 * - _user
	 * - _code
	 */
	
	constructor() {
		if (!this._readInput())
			return;
		this._createView();
		this._tryVerifyEmail();
	}
	
	_readInput() {
		let params = parseURL(window.location.href).params;
		let user = params['user'];
		let code = params['code'];
		if (!user || !code) {
			window.location.href = basePath + '/';
			return false;
		}
		this._user = user;
		this._code = code;
		return true;
	}
	
	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();
		$(document.body).addClass('tinted-background');
		$('#root').css('visibility', 'visible');
	}
	
	_tryVerifyEmail() {
		var self = this;
		$.post(servicePath + '/auth/verify-email',
			{
				user: this._user,
				code: this._code,
			}
		).done(function() {
			self._onVerifyCompleted(true);
		}).fail(function(xhr, status, error) {
			self._onVerifyFailed(xhr, status, error);
		});
	}
	
	_onVerifyFailed(xhr, status, error) {
		this._onVerifyCompleted(false, this._isInvalidInput(xhr));
	}
	
	_isInvalidInput(xhr) {
		if (xhr.status != 400)
			return false;
		let response = xhr.responseJSON;
		if (!response || typeof response !== 'object')
			return false;
		return response.code == 'INVALID_INPUT';
	}

	_onVerifyCompleted(success, invalidInput = false) {
		if (success) {
			window.location.href = basePath + '/verify-email-success';
		} else {
			window.location.href = basePath +
				'/verify-email-failed?invalid-input=' + invalidInput;
		}
	}
}

new VerifyEmailPage();
