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

		let header = new PageBackHeader($('.page-back-header'));
		header.title = i18next.t('my_account');
		header.backUrl = basePath + '/me';
		header.render();

		let user = this._user;
		new MyAccountAuthForm(this, user);
		new MyAccountMfaForm();
		this._createProfileForm();

		menuController.showSidebar();
		menuController.selectMenuItem('me-account');
		$(document.body).addClass('tinted-background');
		let content = $('#content');
		content.addClass('white-background');
		content.css('visibility', 'visible');
	}

	_createProfileForm() {
		let user = this._user;
		let self = this;

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

	updateUser(updateUserFunction) {
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
