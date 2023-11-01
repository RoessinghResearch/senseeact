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
		let background = $('#menu-background');
		background.show();
		$('#menu').show();
		animator.startAnimation(background, 'animate-menu-background-show');
		animator.onAnimatedClickHandlerCompleted(clickId, null);
	}
}

new HeaderController();
