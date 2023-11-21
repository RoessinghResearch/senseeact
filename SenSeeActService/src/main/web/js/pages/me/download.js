class DownloadPage {
	constructor() {
		var self = this;
		checkLogin(function(data) {
			self._onGetUserDone();
		})
	}

	_onGetUserDone() {
		this._createView();
	}

	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();

		$(document.body).addClass('tinted-background');
		let root = $('#root');
		root.addClass('white-background');
		root.css('visibility', 'visible');
	}
}

new DownloadPage();
