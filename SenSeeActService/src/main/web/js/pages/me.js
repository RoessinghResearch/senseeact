class AccountPage {
	/**
	 * Properties:
	 * 
	 * - _user (SenSeeAct user object)
	 * - _logoutButton (jQuery element)
	 */
	constructor() {
		let client = new SenSeeActClient();
		var self = this;
		client.getUser()
			.done(function(data) {
				self._onGetUserDone(data);
			})
			.fail(function(jqXHR, textStatus, errorThrown) {
				self._onGetUserFail(jqXHR, textStatus, errorThrown);
			});
	}

	_onGetUserDone(data) {
		this._user = data;
		this._createView();
		this._registerEvents();
	}

	_onGetUserFail(jqXHR, textStatus, errorThrown) {
		if (jqXHR.status == 401)
			window.location.href = basePath + '/login';
	}

	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();

		let user = this._user;
		var self = this;

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

		let logoutButton = $('#logout-button');
		this._logoutButton = logoutButton;

		$(document.body).addClass('tinted-background');
		let root = $('#root');
		root.addClass('white-background');
		root.css('visibility', 'visible');
	}

	_registerEvents() {
		let logoutButton = this._logoutButton;
		var self = this;
		animator.addAnimatedClickHandler(logoutButton, logoutButton,
			'animate-button-click',
			function(clickId) {
				self._onLogoutClick(clickId);
			},
			function(result) {
				self._onLogoutCompleted(result);
			}
		);
	}

	_onLogoutClick(clickId) {
		let client = new SenSeeActClient();
		var self = this;
		client.logout()
			.done(function(result) {
				self._onLogoutDone(clickId, result);
			})
			.fail(function(xhr, status, error) {
				self._onLogoutFail(clickId, xhr, status, error);
			});
	}

	_onLogoutDone(clickId, result) {
		animator.onAnimatedClickHandlerCompleted(clickId, true);
	}

	_onLogoutFail(clickId, xhr, status, error) {
		animator.onAnimatedClickHandlerCompleted(clickId, false);
	}

	_onLogoutCompleted(success) {
		if (success)
			window.location.href = basePath + '/';
		else
			showToast(i18next.t('unexpected_error'));
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

new AccountPage();
