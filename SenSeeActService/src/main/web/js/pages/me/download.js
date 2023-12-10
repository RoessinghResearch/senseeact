class DownloadPage {
	/**
	 * Member variables:
	 * 
	 * - _lastStartTime (moment)
	 * - _projects (array): Result of GET /download/projects. Each item is an
	 *       object with the following properties:
	 *       - code
	 *       - name
	 * - _runningDownloadIntervalId: If there is a running download, this is
	 *       set to the interval ID from setInterval(). Otherwise it's null.
	 */

	constructor() {
		this._lastStartTime = null;
		this._runningDownloadIntervalId = null;
		var self = this;
		checkLogin(function(data) {
			self._onGetUserDone();
		})
	}

	_onGetUserDone() {
		this._createView();
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

		let header = new PageBackHeader($('.page-back-header'));
		header.title = i18next.t('download_data');
		header.backUrl = basePath + '/me';
		header.render();

		menuController.showSidebar();
		$(document.body).addClass('tinted-background');
		let content = $('#content');
		content.addClass('white-background');
		content.css('visibility', 'visible');
	}

	_onGetDownloadProjects(projects) {
		$('#start-download-wait').hide();
		let emptyDiv = $('#start-download-empty');
		let list = $('#start-download-list');
		this._projects = projects;
		this._updateActiveDownloads(true);
		if (projects.length == 0) {
			emptyDiv.show();
			list.hide();
			return;
		}
		emptyDiv.hide();
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
					self._updateActiveDownloads(true);
				})
				.fail(function(xhr, status, error) {
					self._onClientError();
				});
		}
		animator.onAnimatedClickHandlerCompleted(clickId, null);
	}

	_updateActiveDownloads(showError = false) {
		var self = this;
		let client = new SenSeeActClient();
		client.getDownloadList()
			.done(function(data) {
				self._onGetDownloadList(data);
			})
			.fail(function(xhr, status, error) {
				if (showError)
					self._onClientError();
			});
	}

	_onGetDownloadList(list) {
		$('#active-downloads-wait').hide();
		let filtered = [];
		for (let i = 0; i < list.length; i++) {
			let item = list[i];
			if (item.status == 'IDLE' || item.status == 'RUNNING' ||
					item.status == 'COMPLETED') {
				filtered.push(item);
			}
		}
		let emptyDiv = $('#active-downloads-empty');
		let listDiv = $('#active-downloads-list');
		if (filtered.length == 0) {
			emptyDiv.show();
			listDiv.hide();
			return;
		}
		emptyDiv.hide();
		listDiv.show();
		listDiv.empty();
		let hasIncomplete = false;
		for (let i = 0; i < filtered.length; i++) {
			let item = filtered[i];
			if (item.status != 'COMPLETED')
				hasIncomplete = true;
			let itemDiv = this._createActiveDownloadItem(item);
			listDiv.append(itemDiv);
		}
		var self = this;
		if (hasIncomplete && this._runningDownloadIntervalId === null) {
			this._runningDownloadIntervalId = setInterval(
				function() {
					self._updateActiveDownloads();
				},
				1000);
		} else if (!hasIncomplete && this._runningDownloadIntervalId !== null) {
			clearInterval(this._runningDownloadIntervalId);
			this._runningDownloadIntervalId = null;
		}
	}

	/**
	 * Creates a widget for an active download item. The status should be
	 * IDLE, RUNNING or COMPLETED.
	 */
	_createActiveDownloadItem(item) {
		let project = this._findProjectForCode(item.project);
		let time = moment(item.localTime);
		let dateTimeFormat = i18next.t('date_hour_minute_format');
		let timeStr = time.format(dateTimeFormat);
		let itemDiv = $('<div></div>');
		itemDiv.addClass('active-download-item');

		let col1 = $('<div></div>');
		col1.addClass('active-download-item-col1');
		itemDiv.append(col1);
	
		let nameDiv = $('<div></div>');
		nameDiv.addClass('active-download-item-name');
		nameDiv.text(project.name);
		col1.append(nameDiv);
		let timeDiv = $('<div></div>');
		timeDiv.addClass('active-download-item-time');
		timeDiv.text(timeStr);
		col1.append(timeDiv);

		let col2 = $('<div></div>');
		col2.addClass('active-download-item-col2');
		itemDiv.append(col2);
		let progressDiv = $('<div></div>');
		progressDiv.addClass('active-download-item-progress');
		let progressBar = new ProgressBar(progressDiv)
		progressBar.step = item.step;
		progressBar.total = item.total;
		progressBar.render();
		col2.append(progressDiv);
		let button = $('<a></a>');
		button.addClass('button small');
		let url = servicePath + '/download/' + item.id;
		button.attr('href', url);
		button.text(i18next.t('download'));
		col2.append(button);
		if (item.status == 'IDLE' || item.status == 'RUNNING') {
			button.hide();
		} else {
			// COMPLETED
			progressDiv.hide();
		}

		let col3 = $('<div></div>');
		col3.addClass('active-download-item-col3');
		itemDiv.append(col3);
		let img = $('<div></div>')
			.addClass('icon')
			.css('mask-image', 'url(../images/icon_trash_can.svg)');
		col3.append(img);
		var self = this;
		img.on('click', function() {
			self._onDeleteDownloadClick(item);
		});

		return itemDiv;
	}

	_onDeleteDownloadClick(item) {
		let client = new SenSeeActClient();
		var self = this;
		client.deleteDownload(item.id)
			.done(function(data) {
				self._updateActiveDownloads();
			})
			.fail(function(xhr, status, error) {
				self._onClientError();
			});
	}

	_findProjectForCode(code) {
		for (let i = 0; i < this._projects.length; i++) {
			let project = this._projects[i];
			if (project.code == code)
				return project;
		}
		return null;
	}

	_onClientError() {
		showToast(i18next.t('unexpected_error'));
	}
}

new DownloadPage();
