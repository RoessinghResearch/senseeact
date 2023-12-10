class MySenSeeActPage {
	/**
	 * Properties:
	 * 
	 * - _user (SenSeeAct user object)
	 */

	constructor() {
		var self = this;
		checkLogin(function(data) {
			self._onGetUserDone(data);
		});
	}

	_onGetUserDone(data) {
		this._user = data;
		this._createView();
	}

	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();

		let header = new PageBackHeader($('.page-back-header'));
		header.title = i18next.t('my_senseeact');
		header.render();

		let dashboard = $('#dashboard');
		dashboard.append(this._createDashboardWidget(
			'images/icon_user.svg',
			i18next.t('my_account'),
			basePath + '/me/account'));
		dashboard.append(this._createDashboardWidget(
			'images/icon_download.svg',
			i18next.t('download_data'),
			basePath + '/me/download'));

		$(document.body).addClass('tinted-background');
		$('#content').css('visibility', 'visible');
	}

	_createDashboardWidget(icon, title, url) {
		let link = $('<a></a>');
		link.addClass('dashboard-widget');
		link.attr('href', url);
		let iconImg = $('<div></div>');
		iconImg.addClass('dashboard-widget-icon');
		iconImg.css('mask-image', "url('" + icon + "')");
		iconImg.css('-webkit-mask-image', "url('" + icon + "')");
		link.append(iconImg);
		let titleDiv = $('<div></div>');
		titleDiv.addClass('dashboard-widget-title');
		titleDiv.text(title);
		link.append(titleDiv);
		let goImg = $('<img></img>');
		goImg.addClass('dashboard-widget-go');
		goImg.attr('src', 'images/icon_dashboard_go.svg');
		link.append(goImg);
		return link;
	}
}

new MySenSeeActPage();
