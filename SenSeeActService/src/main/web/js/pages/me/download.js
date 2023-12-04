class DownloadPage {
	/**
	 * Member variables:
	 * 
	 * - _lastStartTime (moment)
	 */

	constructor() {
		var self = this;
		checkLogin(function(data) {
			self._onGetUserDone();
		})
	}

	_onGetUserDone() {
		this._createView();
		this._updateActiveDownloads();
		let client = new SenSeeActClient();
		var self = this;
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

	_onGetDownloadProjects(projects) {
		$('#start-download-wait').hide();
		if (projects.length == 0) {
			$('start-download-empty').show();
			return;
		}
		let list = $('#start-download-list');
		list.show();
		var self = this;
		for (let i = 0; i < projects.length; i++) {
			let project = projects[i];
			let item = $('<div></div>');
			item.addClass('bordered-list-item');
			item.text(project.name);
			list.append(item);
			animator.addAnimatedClickHandler(item, item,
				'animate-bordered-list-item-click',
				function(clickId) {
					self._onProjectClick(clickId, project.code);
				},
				null);
		}
	}

	_onProjectClick(clickId, project) {
		// check for delay of at least 1 second since last download start, to
		// avoid two downloads at double click
		let now = moment();
		let minTime = null;
		if (this._lastStartTime != null) {
			minTime = moment(this._lastStartTime).add(1, 's');
		}
		if (minTime == null || !now.isBefore(minTime)) {
			this._lastStartTime = now;
			var self = this;
			let client = new SenSeeActClient();
			client.startDownload(project)
				.done(function() {
					self._updateActiveDownloads();
				})
				.fail(function(xhr, status, error) {
					self._onClientError();
				});
		}
		animator.onAnimatedClickHandlerCompleted(clickId, null);
	}

	_updateActiveDownloads() {
		var self = this;
		let client = new SenSeeActClient();
		client.getDownloadList()
			.done(function(data) {
				self._onGetDownloadList(data);
			})
			.fail(function(xhr, status, error) {
				self._onClientError();
			});
	}

	_onGetDownloadList(list) {
		$('#active-downloads-wait').hide();
		if (list.length == 0) {
			$('#active-downloads-empty').show();
			return;
		}
		let listDiv = $('#active-downloads-list');
		listDiv.show();
		listDiv.empty();
		for (let i = 0; i < list.length; i++) {
			let item = list[i];
			let itemDiv = this._createActiveDownloadItem(item);
			listDiv.append(itemDiv);
		}
	}

	_onClientError() {
		showToast(i18next.t('unexpected_error'));
	}

	_createActiveDownloadItem(item) {
		let itemDiv = $('<div></div>');
		itemDiv.addClass('active-download-item');
		let leftCol = $('<div></div>');
		leftCol.addClass('active-download-item-left');
		itemDiv.append(leftCol);
		let rightCol = $('<div></div>');
		rightCol.addClass('active-download-item-right');
		itemDiv.append(rightCol);
		let nameDiv = $('<div></div>');
		nameDiv.addClass('active-download-item-name');
		nameDiv.text(item.project);
		leftCol.append(nameDiv);
		let timeDiv = $('<div></div>');
		timeDiv.addClass('active-download-item-time');
		timeDiv.text(item.localTime);
		leftCol.append(timeDiv);
		let progressDiv = $('<div></div>');
		progressDiv.addClass('active-download-item-progress');
		rightCol.append(progressDiv);
		let button = $('<button></button>');
		button.addClass('small');
		button.text(i18next.t('download'));
		rightCol.append(button);
		return itemDiv;
	}
}

new DownloadPage();
