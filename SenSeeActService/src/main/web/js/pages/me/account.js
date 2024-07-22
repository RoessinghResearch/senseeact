class MyAccountPage {
	/**
	 * Properties:
	 * 
	 * - _user (SenSeeAct user object)
	 * - _logoutButton (jQuery element)
	 */
	constructor() {
		var self = this;
		checkLogin((data) => {
			self._onGetUserDone(data);
		})
	}

	_onGetUserDone(data) {
		this._user = data;
		this._createView();
	}

	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();

		let user = this._user;
		var self = this;

		let header = new PageBackHeader($('.page-back-header'));
		header.title = i18next.t('my_account');
		header.backUrl = basePath + '/me';
		header.render();

		let emailLabel = $('#email-label');
		emailLabel.text(i18next.t('email_address') + ':');
		let emailEdit = new EditableTextValue($('#email-value'));
		emailEdit.onEdit = (value) => {
			return self._onEmailEdit(emailEdit, value);
		};
		emailEdit.onEditComplete = (value) => {
			self._showEmail(emailEdit);
		};
		emailEdit.render();
		emailEdit.input.attr('type', 'email');
		this._showEmail(emailEdit);

		let passwordLabel = $('#password-label');
		passwordLabel.text(i18next.t('password') + ':');
		this._showPasswordChangeButton();

		let firstNameLabel = $('#first-name-label');
		firstNameLabel.text(i18next.t('first_name') + ':');
		let firstNameEdit = new EditableTextValue($('#first-name-value'));
		firstNameEdit.value = user.firstName;
		firstNameEdit.onEdit = (value) => {
			return self._onFirstNameEdit(value);
		}
		firstNameEdit.render();

		let lastNameLabel = $('#last-name-label');
		lastNameLabel.text(i18next.t('last_name') + ':');
		let lastNameEdit = new EditableTextValue($('#last-name-value'));
		lastNameEdit.value = user.lastName;
		lastNameEdit.onEdit = (value) => {
			return self._onLastNameEdit(value);
		}
		lastNameEdit.render();

		menuController.showSidebar();
		menuController.selectMenuItem('me-account');
		$(document.body).addClass('tinted-background');
		let content = $('#content');
		content.addClass('white-background');
		content.css('visibility', 'visible');
	}

	_showEmail(edit) {
		let user = this._user;
		if (user.hasTemporaryEmail) {
			edit.value = '';
			edit.placeholderValue = i18next.t('temporary_account');
		} else {
			edit.value = user.email;
			edit.placeholderValue = '';
		}
		let emailVerifyDiv = $('#email-verification-value');
		emailVerifyDiv.empty();
		let valueRow = emailVerifyDiv.parent();
		if (user.hasTemporaryEmail) {
			valueRow.hide();
			return;
		}
		valueRow.show();
		let verifyLine = $('<div></div>');
		verifyLine.addClass('email-verification-line');
		emailVerifyDiv.append(verifyLine);
		let iconDiv = $('<div></div>');
		let verifyClass = user.emailVerified ? 'verified' : 'unverified';
		let iconName = user.emailVerified ? 'icon_check_circle' :
			'icon_triangle_exclamation';
		let textId = user.emailVerified ? 'email_address_verified' :
			'email_address_not_verified';
		iconDiv.addClass('email-verification-icon ' + verifyClass);
		let icon = basePath + '/images/' + iconName + '.svg';
		iconDiv.css('mask-image', "url('" + icon + "')");
		iconDiv.css('-webkit-mask-image', "url('" + icon + "')");
		verifyLine.append(iconDiv);
		let textDiv = $('<div></div>');
		textDiv.addClass('email-verification-text ' + verifyClass);
		textDiv.text(i18next.t(textId));
		verifyLine.append(textDiv);

		if (!user.emailPendingVerification)
			return;
		textDiv = $('<div></div>');
		textDiv.addClass('email-pending-verification-pre');
		textDiv.text(i18next.t('email_address_pending_verification_1'));
		emailVerifyDiv.append(textDiv);
		textDiv = $('<div></div>');
		textDiv.addClass('email-pending-verification');
		textDiv.text(user.emailPendingVerification);
		emailVerifyDiv.append(textDiv);
		textDiv = $('<div></div>');
		textDiv.addClass('email-pending-verification-post');
		textDiv.text(i18next.t('email_address_pending_verification_2'));
		emailVerifyDiv.append(textDiv);
	}

	_onEmailEdit(edit, value) {
		let xhr = this._updateUser((user) => {
			user.email = value;
		});
		var self = this;
		xhr.fail((xhr, status, error) => {
			if (self._isInvalidInputField('email', xhr)) {
				edit.showError(i18next.t('invalid_email_address'));
			} else if (self._isUserAlreadyExists(xhr)) {
				edit.showError(i18next.t('create_account_email_exists'));
			} else {
				showToast(i18next.t('unexpected_error'));
			}
		});
		return xhr;
	}

	_isInvalidInputField(field, xhr) {
		if (xhr.status != 400)
			return false;
		return true;
	}

	_isUserAlreadyExists(xhr) {
		if (xhr.status != 403)
			return false;
		return true;
	}

	_onChangePasswordClick() {
		this._showPasswordChangeForm(this._user.hasTemporaryPassword);
	}

	_showPasswordChangeButton() {
		let valueDiv = $('#password-value');
		valueDiv.empty();
		let button = $('<button></button>');
		button.addClass('button small change-button');
		valueDiv.append(button);
		button.text(i18next.t('change'));
		var self = this;
		animator.addAnimatedClickHandler(button, button,
			'animate-button-click', null,
			() => {
				self._onChangePasswordClick();
			}
		);
	}

	_showPasswordChangeForm(temporary) {
		let valueDiv = $('#password-value');
		valueDiv.empty();
		let form = $('<form></form>');
		valueDiv.append(form);
		let oldInput = null;
		if (!temporary) {
			oldInput = $('<input></input>')
				.attr('name', 'old-password')
				.attr('type', 'password')
				.attr('placeholder', i18next.t('current_password'));
			form.append(oldInput);
		}
		let newInput = $('<input></input>')
			.attr('name', 'new-password')
			.attr('type', 'password')
			.attr('placeholder', i18next.t('new_password'));
		form.append(newInput);
		let repeatInput = $('<input></input>')
			.attr('name', 'repeat-new-password')
			.attr('type', 'password')
			.attr('placeholder', i18next.t('repeat_new_password'));
		form.append(repeatInput);
		let errorDiv = $('<div></div>')
			.addClass('password-error error');
		form.append(errorDiv);
		let buttonsDiv = $('<div></div>')
			.addClass('form-button-row');
		form.append(buttonsDiv);
		let button = $('<button></button>')
			.attr('type', 'button')
			.addClass('small')
			.text(i18next.t('cancel'));
		let self = this;
		animator.addAnimatedClickHandler(button, button, 'animate-button-click',
			null,
			() => {
				self._onChangePasswordCancelClick();
			}
		);
		buttonsDiv.append(button);
		button = $('<button></button>')
			.attr('type', 'submit')
			.addClass('small')
			.text(i18next.t('ok'));
		animator.addAnimatedClickHandler(button, button, 'animate-button-click',
			(clickId) => {
				self._onChangePasswordOkClick(clickId, oldInput, newInput,
					repeatInput);
			},
			(result) => {
				self._onChangePasswordOkComplete(result);
			}
		);
		buttonsDiv.append(button);
		form.find('input').eq(0).focus();
	}

	_onChangePasswordCancelClick() {
		this._showPasswordChangeButton();
	}

	_onChangePasswordOkClick(clickId, oldInput, newInput, repeatInput) {
		let valueDiv = $('#password-value');
		valueDiv.find('input').removeClass('error');
		let errorDiv = valueDiv.find('.password-error');
		errorDiv.hide();
		let error = false;
		let errorMessage = null;
		let oldPassword = null;
		if (oldInput)
			oldPassword = oldInput.val();
		let newPassword = newInput.val();
		let repeatPassword = repeatInput.val();
		if (repeatPassword.length < 6) {
			error = true;
			if (repeatPassword.length > 0)
				errorMessage = i18next.t('password_too_short');
			repeatInput.addClass('error');
			repeatInput.focus();
		}
		if (newPassword.length < 6) {
			error = true;
			if (newPassword.length > 0)
				errorMessage = i18next.t('password_too_short');
			newInput.addClass('error');
			newInput.focus();
		}
		if (!error && repeatPassword != newPassword) {
			error = true;
			errorMessage = i18next.t('no_repeat_new_password_match');
			repeatInput.addClass('error');
			newInput.addClass('error');
			newInput.focus();
		}
		if (oldInput && !oldPassword) {
			error = true;
			oldInput.addClass('error');
			oldInput.focus();
		}
		if (errorMessage) {
			errorDiv.text(errorMessage);
			errorDiv.show();
		}
		animator.onAnimatedClickHandlerCompleted(clickId, {
			success: !error
		});
	}

	_onChangePasswordOkComplete(result) {
		if (result.success)
			this._showPasswordChangeButton();
	}

	_onFirstNameEdit(value) {
		return this._updateUserFailUnexpected((user) => {
			user.firstName = value;
		});
	}

	_onLastNameEdit(value) {
		return this._updateUserFailUnexpected((user) => {
			user.lastName = value;
		});
	}

	_updateUser(updateUserFunction) {
		let newUser = JSON.parse(JSON.stringify(this._user));
		updateUserFunction(newUser);
		let client = new SenSeeActClient();
		let xhr = client.updateUser(null, newUser);
		var self = this;
		xhr.done((result) => {
			self._user = result;
		});
		return xhr;
	}

	_updateUserFailUnexpected(updateUserFunction) {
		let xhr = this._updateUser(updateUserFunction);
		xhr.fail(() => {
			showToast(i18next.t('unexpected_error'));
		});
		return xhr;
	}
}

new MyAccountPage();
