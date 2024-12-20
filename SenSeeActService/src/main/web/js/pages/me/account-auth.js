class MyAccountAuthForm {
	constructor(accountPage, user) {
		this._accountPage = accountPage;
		this._user = user;
		let self = this;
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
		let emailVerifyLabel = $('#email-verification-label');
		emailVerifyLabel.addClass('empty');
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
		let xhr = this._accountPage.updateUser((user) => {
			user.email = value;
		});
		let client = new SenSeeActClient();
		xhr.fail((xhr, status, error) => {
			if (client.hasInvalidInputField(xhr, 'email')) {
				edit.showError(i18next.t('invalid_email_address'));
			} else if (client.hasErrorCode(xhr, 403, 'USER_ALREADY_EXISTS')) {
				edit.showError(i18next.t('create_account_email_exists'));
			} else {
				showToast(i18next.t('unexpected_error'));
			}
		});
		return xhr;
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
			'animate-blue-button-click', null,
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
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click',
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
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click',
			(clickId) => {
				self._onChangePasswordOkClick(clickId, oldInput, newInput,
					repeatInput);
			},
			(result) => {
				self._onChangePasswordOkComplete(result);
			}
		);
		buttonsDiv.append(button);
		form.find('input').eq(0).trigger('focus');
	}

	_onChangePasswordCancelClick() {
		this._showPasswordChangeButton();
	}

	_onChangePasswordOkClick(clickId, oldInput, newInput, repeatInput) {
		let validation = this._validateChangePasswordInput(oldInput, newInput,
			repeatInput);
		if (!validation.success) {
			animator.onAnimatedClickHandlerCompleted(clickId,
				{ success: false });
			return;
		}
		var self = this;
		let client = new SenSeeActClient();
		client.changePassword(validation.oldPassword, validation.newPassword)
		.done(() => {
			self._onChangePasswordDone(clickId);
		})
		.fail((xhr, status, error) => {
			self._onChangePasswordFail(clickId, oldInput, xhr);
		});
	}

	_validateChangePasswordInput(oldInput, newInput, repeatInput) {
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
			repeatInput.trigger('focus');
		}
		if (newPassword.length < 6) {
			error = true;
			if (newPassword.length > 0)
				errorMessage = i18next.t('password_too_short');
			newInput.addClass('error');
			newInput.trigger('focus');
		}
		if (!error && repeatPassword != newPassword) {
			error = true;
			errorMessage = i18next.t('no_repeat_new_password_match');
			repeatInput.addClass('error');
			newInput.addClass('error');
			newInput.trigger('focus');
		}
		if (oldInput && !oldPassword) {
			error = true;
			oldInput.addClass('error');
			oldInput.trigger('focus');
		}
		if (errorMessage) {
			errorDiv.text(errorMessage);
			errorDiv.show();
		}
		return {
			success: !error,
			oldPassword: oldPassword,
			newPassword: newPassword,
			repeatPassword: repeatPassword
		};
	}

	_onChangePasswordFail(clickId, oldInput, xhr) {
		let valueDiv = $('#password-value');
		valueDiv.find('input').removeClass('error');
		let errorDiv = valueDiv.find('.password-error');
		let client = new SenSeeActClient();
		if (client.hasInvalidInputField(xhr, 'oldPassword')) {
			oldInput.addClass('error');
			oldInput.trigger('focus');
			errorDiv.text(i18next.t('old_password_incorrect'));
			errorDiv.show();
		} else {
			showToast(i18next.t('unexpected_error'));
		}
		animator.onAnimatedClickHandlerCompleted(clickId, { success: false });
	}

	_onChangePasswordDone(clickId) {
		var self = this;
		let client = new SenSeeActClient();
		client.getUser()
		.done((result) => {
			self._onChangePasswordGetUserDone(clickId, result);
		})
		.fail((xhr, status, error) => {
			self._onChangePasswordGetUserFail(clickId);
		});
	}

	_onChangePasswordGetUserDone(clickId, user) {
		this._user = user;
		animator.onAnimatedClickHandlerCompleted(clickId,
			{ success: true });
	}

	_onChangePasswordGetUserFail(clickId) {
		showToast(i18next.t('unexpected_error'));
		animator.onAnimatedClickHandlerCompleted(clickId,
			{ success: false });
	}
	
	_onChangePasswordOkComplete(result) {
		if (result.success) {
			this._showPasswordChangeButton();
			showToast(i18next.t('password_changed'));
		}
	}
}
