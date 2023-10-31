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
	}

	_onShowMenuClick(clickId) {
		$('#menu').css('display', 'block');
		animator.onAnimatedClickHandlerCompleted(clickId, null);
	}
}

new HeaderController();
