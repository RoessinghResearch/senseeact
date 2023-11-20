class SignupPage {
	/**
	 * Properties:
	 *
	 * - _emailCard (jQuery element)
	 * - _passwordCard (jQuery element)
	 * - _firstNameEdit (IconTextEdit)
	 * - _lastNameEdit (IconTextEdit)
	 * - _emailEdit (IconTextEdit)
	 * - _passwordEdit (IconTextEdit)
	 * - _repeatPasswordEdit (IconTextEdit)
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

		let acceptTerms = this._getAcceptTermsLabel();
		let label = $('#accept-terms-check-container .label');
		label.append(acceptTerms);

		let error = $('#email-error');
		this._error = error;
		error.hide();

		let button = $('#button');
		this._button = button;
		button.prop('disabled', true);

		$(document.body).addClass('tinted-background');
		$('#root').css('visibility', 'visible');
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
		let link = $('<a></a>');
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
		let button = this._button;
		animator.addAnimatedClickHandler(button, button, 'animate-button-click',
			null,
			function(result) {
				self._onButtonClick();
			}
		);
	}

	_updateButtonEnabled() {
		let email = this._emailEdit.textInput.val().trim();
		let hasInput = email.length > 0;
		let button = this._button;
		button.prop('disabled', !hasInput);
	}

	_onButtonClick() {
		this._emailCard.css('visibility', 'hidden');
		this._passwordCard.css('visibility', 'visible');
		this._passwordEdit.textInput.focus();
	}
}

new SignupPage();
