class SignupPage {
	/**
	 * Properties:
	 *
	 * - _emailCard (jQuery element)
	 * - _passwordCard (jQuery element)
	 * - _firstNameEdit (IconTextEdit)
	 * - _lastNameEdit (IconTextEdit)
	 * - _emailEdit (IconTextEdit)
	 * - _emailError (jQuery element)
	 * - _passwordEdit (IconTextEdit)
	 * - _repeatPasswordEdit (IconTextEdit)
	 * - _passwordError (jQuery element)
	 * - _acceptTermsCheck (jQuery input element)
	 */
	constructor() {
		this._createView();
		this._registerEvents();
	}

	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();

		let emailCard = $('#email-card');
		this._emailCard = emailCard;
		let passwordCard = $('#password-card');
		this._passwordCard = passwordCard;
		passwordCard.css('visibility', 'hidden');

		let firstNameEdit = new IconTextEdit($('#first-name-edit'));
		this._firstNameEdit = firstNameEdit;
		firstNameEdit.placeholder = i18next.t('your_first_name_optional');
		firstNameEdit.render();
		firstNameEdit.textInput.attr('type', 'text');

		let lastNameEdit = new IconTextEdit($('#last-name-edit'));
		this._lastNameEdit = lastNameEdit;
		lastNameEdit.placeholder = i18next.t('your_last_name_optional');
		lastNameEdit.render();
		lastNameEdit.textInput.attr('type', 'text');

		let emailEdit = new IconTextEdit($('#email-edit'));
		this._emailEdit = emailEdit;
		emailEdit.placeholder = i18next.t('email_address');
		emailEdit.render();
		emailEdit.textInput.attr('type', 'email');

		let emailError = $('#email-error');
		this._emailError = emailError;
		emailError.hide();

		let passwordEdit = new IconTextEdit($('#password-edit'));
		this._passwordEdit = passwordEdit;
		passwordEdit.placeholder = i18next.t('password');
		passwordEdit.render();
		passwordEdit.textInput.attr('type', 'password');

		let repeatPasswordEdit = new IconTextEdit($('#repeat-password-edit'));
		this._repeatPasswordEdit = repeatPasswordEdit;
		repeatPasswordEdit.placeholder = i18next.t('repeat_password');
		repeatPasswordEdit.render();
		repeatPasswordEdit.textInput.attr('type', 'password');

		let passwordError = $('#password-error');
		this._passwordError = passwordError;
		passwordError.hide();

		let acceptTerms = this._getAcceptTermsLabel();
		let label = $('#accept-terms-check-container .label');
		label.append(acceptTerms);
		this._acceptTermsCheck = $('#accept-terms-check');

		let error = $('#email-error');
		this._error = error;
		error.hide();

		let button = $('#button');
		this._button = button;
		button.prop('disabled', true);

		$(document.body).addClass('tinted-background');
		$('#content').css('visibility', 'visible');
		firstNameEdit.textInput.focus();
	}

	_getAcceptTermsLabel() {
		let acceptTermsText = i18next.t('accept_terms_and_conditions');
		let startTag = '{link}';
		let endTag = '{/link}';
		let startTagPos = acceptTermsText.indexOf(startTag);
		let endTagPos = acceptTermsText.indexOf(endTag);
		let before = acceptTermsText.substring(0, startTagPos);
		let between = acceptTermsText.substring(startTagPos + startTag.length,
			endTagPos);
		let after = acceptTermsText.substring(endTagPos + endTag.length);
		let label = $('<span></span>');
		label.append(document.createTextNode(before));
		let link = $('<a target="_blank"></a>');
		link.attr('href', basePath + '/privacy-policy');
		link.text(between);
		label.append(link);
		label.append(document.createTextNode(after));
		return label;
	}

	_registerEvents() {
		let emailInput = this._emailEdit.textInput;
		var self = this;
		emailInput.on('input', function() {
			self._updateButtonEnabled();
		});
		let passwordEdit = this._passwordEdit.textInput;
		passwordEdit.on('input', function() {
			self._updateButtonEnabled();
		});
		let repeatPasswordEdit = this._repeatPasswordEdit.textInput;
		repeatPasswordEdit.on('input', function() {
			self._updateButtonEnabled();
		});
		let acceptTermsCheck = this._acceptTermsCheck;
		acceptTermsCheck.on('change', function() {
			self._updateButtonEnabled();
		});
		this._showEmailCard();
	}

	_updateButtonEnabled() {
		let emailCard = this._emailCard;
		let hasInput;
		if (emailCard.css('visibility') == 'visible') {
			let email = this._emailEdit.textInput.val().trim();
			hasInput = email.length != 0;
		} else {
			let password = this._passwordEdit.textInput.val();
			let repeatPassword = this._repeatPasswordEdit.textInput.val();
			let acceptTerms = this._acceptTermsCheck.prop('checked');
			hasInput = password.length != 0 && repeatPassword.length != 0 &&
				acceptTerms;
		}
		let button = this._button;
		button.prop('disabled', !hasInput);
	}

	_showEmailCard() {
		this._passwordCard.css('visibility', 'hidden');
		this._emailCard.css('visibility', 'visible');
		this._firstNameEdit.textInput.focus();
		let button = this._button;
		animator.clearAnimatedClickHandler(button);
		this._updateButtonEnabled();
		var self = this;
		animator.addAnimatedClickHandler(button, button, 'animate-button-click',
			null,
			function(result) {
				self._showPasswordCard();
			}
		);
	}

	_showPasswordCard() {
		this._emailCard.css('visibility', 'hidden');
		this._passwordCard.css('visibility', 'visible');
		this._passwordEdit.textInput.focus();
		let button = this._button;
		animator.clearAnimatedClickHandler(button);
		this._updateButtonEnabled();
		var self = this;
		animator.addAnimatedClickHandler(button, button, 'animate-button-click',
			function(clickId) {
				self._onSignupClick(clickId);
			},
			function(result) {
				self._onSignupCompleted(result);
			}
		);
	}

	_onSignupClick(clickId) {
		this._emailError.hide();
		this._passwordError.hide();
		let firstName = this._firstNameEdit.textInput.val().trim();
		let lastName = this._lastNameEdit.textInput.val().trim();
		let profile = {};
		if (firstName.length != 0)
			profile['firstName'] = firstName;
		if (lastName.length != 0)
			profile['lastName'] = lastName;
		let email = this._emailEdit.textInput.val().trim();
		let password = this._passwordEdit.textInput.val();
		let repeatPassword = this._repeatPasswordEdit.textInput.val();
		let errors = {};
		if (!this._isValidEmail(email))
			errors['emailError'] = 'invalid_email_address';
		else if (password.length < 6)
			errors['passwordError'] = 'password_too_short';
		else if (password != repeatPassword)
			errors['passwordError'] = 'no_repeat_password_match';
		if (Object.keys(errors).length != 0) {
			animator.onAnimatedClickHandlerCompleted(clickId, errors);
			return;
		}
		var self = this;
		let client = new SenSeeActClient();
		client.signup(email, password, 'default')
			.done(function(result) {
				self._onSignupDone(clickId, profile);
			})
			.fail(function(xhr, status, error) {
				self._onSignupFail(clickId, xhr, status, error);
			});
	}

	_isValidEmail(email) {
		if (email.length == 0)
			return false;
		let localDomainSep = email.lastIndexOf('@');
		if (localDomainSep == -1)
			return false;
		let localPart = email.substring(0, localDomainSep);
		let domainPart = email.substring(localDomainSep + 1);
		
		// validate local part
		let allowedLocalChars = /^[A-Za-z0-9!#$%&'*+\-/=?^_`.{|}~]+$/;
		if (!localPart.match(allowedLocalChars))
			return false;

		// validate domain part
		let split = domainPart.split('\.');
		if (split.length < 2)
			return false;
		for (let i = 0; i < split.length; i++) {
			let domainLabel = split[i];
			if (!domainLabel.match(/^[A-Za-z0-9\\-]+$/) ||
					domainLabel.startsWith("-") || domainLabel.endsWith("-")) {
				return false;
			}
			if (i == split.length - 1 && domainLabel.match(/^[0-9]+$/))
				return false;
		}
		return true;
	}

	_onSignupDone(clickId, profile) {
		if (Object.keys(profile) == 0) {
			animator.onAnimatedClickHandlerCompleted(clickId, {});
			return;
		}
		var self = this;
		let client = new SenSeeActClient();
		client.updateUser(null, profile)
			.done(function(result) {
				self._onUpdateUserDone(clickId);
			})
			.fail(function(xhr, status, error) {
				self._onUpdateUserFail(clickId, xhr, status, error);
			});
	}

	_onSignupFail(clickId, xhr, status, error) {
		let errors = {};
		if (xhr.status == 403 && xhr.responseJSON) {
			let code = xhr.responseJSON.code;
			if (code == 'USER_ALREADY_EXISTS') {
				errors['emailError'] = 'create_account_email_exists';
			}
		}
		if (Object.keys(errors).length == 0) {
			errors['generalError'] = 'unexpected_error';
		}
		animator.onAnimatedClickHandlerCompleted(clickId, errors)
	}

	_onUpdateUserDone(clickId) {
		animator.onAnimatedClickHandlerCompleted(clickId, {});
	}

	_onUpdateUserFail(clickId, xhr, status, error) {
		console.log('Failed to update user');
		console.log(xhr);
		animator.onAnimatedClickHandlerCompleted(clickId, {});
	}

	_onSignupCompleted(errors) {
		if (errors['generalError']) {
			showToast(i18next.t(errors['generalError']));
		} else if (errors['emailError']) {
			this._emailError.text(i18next.t(errors['emailError']));
			this._emailError.show();
			this._showEmailCard();
			this._emailEdit.textInput.focus();
		} else if (errors['passwordError']) {
			this._passwordError.text(i18next.t(errors['passwordError']));
			this._passwordError.show();
		} else {
			redirectOnLogin();
		}
	}
}

new SignupPage();
