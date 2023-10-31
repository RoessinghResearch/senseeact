class MenuController {
	constructor() {
		this._registerEvents();
	}

	_registerEvents() {
		let closeIcon = $('.menu-icon.close');
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
	}

	_onHideMenuClick(clickId) {
		animator.onAnimatedClickHandlerCompleted(clickId, null);
	}

	_onHideMenuCallback() {
		$('#menu').css('display', 'none');
	}
}

new MenuController();
