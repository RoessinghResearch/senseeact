class DownloadPage {
	constructor() {
		var self = this;
		checkLogin(function(data) {
			self._onGetUserDone();
		})
	}

	_onGetUserDone() {
		this._createView();
		let client = new SenSeeActClient();
		var self = this;
		client.getDownloadList()
			.done(function(data) {
				self._onGetDownloadList(data);
			})
			.fail(function(xhr, status, error) {
				self._onClientError();
			});
		client.getDownloadProjects()
			.done(function(data) {
				self._onGetDownloadProjects(data);
			})
			.fail(function(xhr, status, error) {
				self._onClientError();
			});
	}

	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();

		$(document.body).addClass('tinted-background');
		let root = $('#root');
		root.addClass('white-background');
		root.css('visibility', 'visible');
	}

	_onGetDownloadList(list) {
	}

	_onGetDownloadProjects(projects) {
	}

	_onClientError() {
		showToast(i18next.t('unexpected_error'));
	}
}

new DownloadPage();
