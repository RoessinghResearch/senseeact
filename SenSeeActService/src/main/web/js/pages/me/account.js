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

		let firstNameLabel = $('#first-name-label');
		firstNameLabel.text(i18next.t('first_name') + ':');
		let firstNameValue = new EditableTextValue($('#first-name-value'));
		firstNameValue.value = user.firstName;
		firstNameValue.onEdit = function(value) {
			return self._onFirstNameEdit(value);
		}
		firstNameValue.render();

		let lastNameLabel = $('#last-name-label');
		lastNameLabel.text(i18next.t('last_name') + ':');
		let lastNameValue = new EditableTextValue($('#last-name-value'));
		lastNameValue.value = user.lastName;
		lastNameValue.onEdit = function(value) {
			return self._onLastNameEdit(value);
		}
		lastNameValue.render();

		menuController.showSidebar();
		$(document.body).addClass('tinted-background');
		let content = $('#content');
		content.addClass('white-background');
		content.css('visibility', 'visible');
	}

	_onFirstNameEdit(value) {
		return this._updateUser(function(user) {
			user.firstName = value;
		});
	}

	_onLastNameEdit(value) {
		return this._updateUser(function(user) {
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
}

new MyAccountPage();
