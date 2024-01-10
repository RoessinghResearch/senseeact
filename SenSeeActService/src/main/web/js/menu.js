class MenuController {
	constructor() {
		this._hasLogOut = false;
		this._registerCloseEvent();
	}

	appendMenuItem(title, url) {
		let menu = $('.menu-item-list');
		let sidebar = $('#sidebar');
		let menuItem = this._createMenuItemDiv(title, url);
		let sidebarItem = this._createMenuItemDiv(title, url);
		if (this._hasLogOut) {
			let logoutItem = menu.children(':last-child');
			logoutItem.before(menuItem);
			logoutItem = sidebar.children(':last-child');
			logoutItem.before(sidebarItem);
		} else {
			menu.append(menuItem);
			sidebar.append(sidebarItem);
		}
	}

	appendLogout() {
		let item = this._createLogoutItemDiv();
		$('.menu-item-list').append(item);
		item = this._createLogoutItemDiv();
		$('#sidebar').append(item);
		this._hasLogOut = true;
	}

	_createMenuItemDiv(title, url = null) {
		let itemDiv = $('<a></a>');
		itemDiv.addClass('menu-item');
		if (url)
			itemDiv.attr('href', url);
		else
			itemDiv.attr('href', '#');
		itemDiv.text(title);
		if (url) {
			animator.addAnimatedClickHandler(itemDiv, itemDiv,
				'animate-menu-item-click',
				null,
				function() {
					window.location.href = url;
				}
			);
		}
		return itemDiv;
	}

	_createLogoutItemDiv() {
		let itemDiv = this._createMenuItemDiv(i18next.t('log_out'));
		var self = this;
		animator.addAnimatedClickHandler(itemDiv, itemDiv,
			'animate-menu-item-click',
			function(clickId) {
				self._onLogoutClick(clickId);
			},
			function(result) {
				self._onLogoutCompleted(result);
			}
		);
		return itemDiv;
	}

	showSidebar() {
		$('#sidebar-switch').css('display', 'flex');
		$('#toast').addClass('sidebar-visible');
	}

	_registerCloseEvent() {
		let closeIcon = $('.menu-icon.close');
		let background = $('#menu-background');
		let menuDiv = $('#menu');
		var self = this;
		animator.addAnimatedClickHandler(closeIcon, menuDiv,
			'animate-menu-hide',
			function(clickId) {
				self._onHideMenuClick(clickId);
			},
			function() {
				self._onHideMenuCallback();
			}
		);
		animator.addAnimatedClickHandler(background, menuDiv,
			'animate-menu-hide',
			function(clickId) {
				self._onHideMenuClick(clickId);
			},
			function() {
				self._onHideMenuCallback();
			}
		);
	}

	_onHideMenuClick(clickId) {
		animator.startAnimation($('#menu-background'),
			'animate-menu-background-hide');
		animator.onAnimatedClickHandlerCompleted(clickId, null);
	}

	_onHideMenuCallback() {
		$('#menu').hide();
		$('#menu-background').hide();
	}

	_registerItemEvents(item) {
		var self = this;
		animator.addAnimatedClickHandler(item, item, 'animate-menu-item-click',
			null,
			function() {
				self._onMenuItemClickCallback(item);
			}
		);
	}

	_onMenuItemClickCallback(item) {
		let id = item.attr('id');
		if (id == 'menu-item-account') {
			this._onMenuItemAccountClick();
		}
	}

	_onMenuItemAccountClick() {
		window.location.href = basePath + '/me';
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
};
