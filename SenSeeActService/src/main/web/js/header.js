class HeaderController {
	constructor() {
		this._registerEvents();
	}

	_registerEvents() {
		let menuIcon = $('.header-icon.menu');
		let menuDiv = $('#menu');
		var self = this;
		animator.addAnimatedClickHandler(menuIcon, menuDiv,
			'animate-menu-show',
			function(clickId) {
				self._onShowMenuClick(clickId);
			},
			null
		);
		let menuItems = $('.header-menu-item');
		for (let i = 0; i < menuItems.length; i++) {
			let menuItem = menuItems.eq(i);
			let title = menuItem.find('.header-title');
			let submenu = menuItem.find('.header-submenu-container');
			if (submenu.length == 0)
				continue;
			var self = this;
			menuItem.on('mouseover', () => {
				self._showSubmenu(menuItem);
			});
			menuItem.on('mouseout', () => {
				submenu.hide();
			});
			title.on('touchstart', (ev) => {
				self._toggleSubmenu(menuItem);
				ev.preventDefault();
			});
		}
		let submenuItems = $('.header-submenu-title');
		for (let i = 0; i < submenuItems.length; i++) {
			let submenuItem = submenuItems.eq(i);
			submenuItem.on('click', (ev) => {
				ev.preventDefault();
			});
			animator.addAnimatedClickHandler(submenuItem, submenuItem,
				'animate-header-submenu-item-click',
				null,
				() => {
					self._onSubmenuItemClick(submenuItem);
				}
			);
		}
	}

	_toggleSubmenu(menuItem) {
		let submenu = menuItem.find('.header-submenu-container');
		let wasVisible = submenu.is(':visible');
		let allMenus = $('#header-menu').find('.header-submenu-container');
		allMenus.hide();
		if (!wasVisible)
			this._showSubmenu(menuItem);
	}

	_showSubmenu(menuItem) {
		let header = $('#header');
		let title = menuItem.find('.header-title');
		let submenu = menuItem.find('.header-submenu-container');
		let left = title.position().left;
		let maxRight = header.width();
		let width = submenu.width();
		let maxLeft = maxRight - width - 16;
		if (maxLeft < left)
			left = maxLeft;
		submenu.css('left', left + 'px');
		submenu.show();
	}

	_onShowMenuClick(clickId) {
		let background = $('#menu-background');
		background.show();
		$('#menu').show();
		animator.startAnimation(background, 'animate-menu-background-show');
		animator.onAnimatedClickHandlerCompleted(clickId, null);
	}

	_onSubmenuItemClick(item) {
		window.location = item.attr('href');
	}
}

new HeaderController();
