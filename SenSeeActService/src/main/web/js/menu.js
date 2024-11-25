class MenuController {
	constructor() {
		this._hasLogOut = false;
		this._registerCloseEvent();
	}

	appendMenuItem(id, title, url) {
		let menu = $('.menu-item-list');
		let sidebar = $('#sidebar');
		let menuItem = this._createMenuItem(id, title, url);
		let sidebarItem = this._createMenuItem(id, title, url);
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

	appendSubMenuItem(parentId, subId, title, url) {
		let menuItem = $('.menu-item-list').find('.menu-item-id-' + parentId)
			.parent();
		let sidebarItem = $('#sidebar').find('.menu-item-id-' + parentId)
			.parent();
		let subMenuItem = this._createSubMenuItem(subId, title, url);
		let subSidebarItem = this._createSubMenuItem(subId, title, url);
		menuItem.append(subMenuItem);
		sidebarItem.append(subSidebarItem);
	}

	appendLogout() {
		let item = this._createLogoutItem();
		$('.menu-item-list').append(item);
		item = this._createLogoutItem();
		$('#sidebar').append(item);
		this._hasLogOut = true;
	}

	selectMenuItem(id) {
		let menus = $('.menu-item-list').add('#sidebar');
		let items = menus.find('.menu-item');
		items.each((index, elem) => {
			if ($(elem).hasClass('menu-item-id-' + id))
				$(elem).addClass('menu-item-selected');
			else
				$(elem).removeClass('menu-item-selected');
		});
	}

	_createMenuItem(id, title, url = null) {
		let container = $('<div></div>');
		container.addClass('menu-item-container');
		let item = this._createMenuItemElement(id, title, url);
		container.append(item);
		return container;
	}

	_createSubMenuItem(id, title, url = null) {
		let item = this._createMenuItemElement(id, title, url);
		item.addClass('menu-item-sub');
		return item;
	}

	_createMenuItemElement(id, title, url = null) {
		let item = $('<a></a>');
		item.addClass('menu-item menu-item-id-' + id);
		if (url)
			item.attr('href', url);
		else
			item.attr('href', '#');
		item.text(title);
		if (url) {
			animator.addAnimatedClickHandler(item, item,
				'animate-menu-item-click',
				null,
				function() {
					window.location.href = url;
				}
			);
		}
		return item;
	}

	_createLogoutItem() {
		let container = this._createMenuItem('logout', i18next.t('log_out'));
		let item = container.find('.menu-item');
		var self = this;
		animator.addAnimatedClickHandler(item, item,
			'animate-menu-item-click',
			function(clickId) {
				self._onLogoutClick(clickId);
			},
			function(result) {
				self._onLogoutCompleted(result);
			}
		);
		return container;
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
		animator.onAnimatedClickHandlerCompleted(clickId, true);
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
		if (success) {
			clearAllCookies();
			window.location.href = basePath + '/';
		} else {
			showToast(i18next.t('unexpected_error'));
		}
	}
};
