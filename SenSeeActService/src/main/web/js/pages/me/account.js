class MyAccountPage {
	/**
	 * Properties:
	 * 
	 * - _user (SenSeeAct user object)
	 * - _logoutButton (jQuery element)
	 */
	constructor() {
		var self = this;
		checkLogin(function(data) {
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
		emailEdit.value = user.email;
		emailEdit.onEdit = function(value) {
			return self._onEmailEdit(emailEdit, value);
		}
		emailEdit.render();
		emailEdit.input.attr('type', 'email');

		let firstNameLabel = $('#first-name-label');
		firstNameLabel.text(i18next.t('first_name') + ':');
		let firstNameEdit = new EditableTextValue($('#first-name-value'));
		firstNameEdit.value = user.firstName;
		firstNameEdit.onEdit = function(value) {
			return self._onFirstNameEdit(value);
		}
		firstNameEdit.render();

		let lastNameLabel = $('#last-name-label');
		lastNameLabel.text(i18next.t('last_name') + ':');
		let lastNameEdit = new EditableTextValue($('#last-name-value'));
		lastNameEdit.value = user.lastName;
		lastNameEdit.onEdit = function(value) {
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

	_onEmailEdit(edit, value) {
		let xhr = this._updateUser(function(user) {
			user.email = value;
		});
		var self = this;
		xhr.fail(function(xhr, status, error) {
			console.log(xhr);
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

	_onFirstNameEdit(value) {
		return this._updateUserFailUnexpected(function(user) {
			user.firstName = value;
		});
	}

	_onLastNameEdit(value) {
		return this._updateUserFailUnexpected(function(user) {
			user.lastName = value;
		});
	}

	_updateUser(updateUserFunction) {
		let newUser = JSON.parse(JSON.stringify(this._user));
		updateUserFunction(newUser);
		let client = new SenSeeActClient();
		let xhr = client.updateUser(null, newUser);
		var self = this;
		xhr.done(function() {
			self._user = newUser;
		});
		return xhr;
	}

	_updateUserFailUnexpected(updateUserFunction) {
		let xhr = this._updateUser(updateUserFunction);
		xhr.fail(function() {
			showToast(i18next.t('unexpected_error'));
		});
		return xhr;
	}
}

new MyAccountPage();
