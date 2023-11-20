class MenuController {
	constructor() {
		this._registerEvents();
	}

	_registerEvents() {
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
		$('#menu .menu-item').each(function() {
			self._registerItemEvents($(this));
		});
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
			function(clickId) {
				self._onMenuItemClick(clickId);
			},
			function() {
				self._onMenuItemClickCallback(item);
			}
		);
	}

	_onMenuItemClick(clickId) {
		animator.onAnimatedClickHandlerCompleted(clickId, null);
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
}

new MenuController();
