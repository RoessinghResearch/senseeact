class MenuController {
	constructor() {
		this._registerCloseEvent();
	}

	appendMenuItem(title, url) {
		let item = this._createMenuItemDiv(title, url);
		$('.menu-item-list').append(item);
		item = this._createMenuItemDiv(title, url);
		$('#sidebar').append(item);
	}

	_createMenuItemDiv(title, url) {
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

	showSidebar() {
		$('#sidebar-switch').css('display', 'flex');
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
};
