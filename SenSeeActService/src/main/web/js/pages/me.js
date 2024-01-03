class MySenSeeActPage {
	/**
	 * Properties:
	 * 
	 * - _user (SenSeeAct user object)
	 */

	start() {
		var self = this;
		checkLogin(function(data) {
			self.onGetUserDone(data);
		});
	}

	onGetUserDone(data) {
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
		let widget = this._createDashboardWidget(
			'images/icon_logout.svg',
			i18next.t('log_out'), null);
		var self = this;
		animator.addAnimatedClickHandler(widget, widget,
			'animate-dashboard-widget-click',
			function(clickId) {
				self._onLogoutClick(clickId);
			},
			function(result) {
				self._onLogoutCompleted(result);
			}
		);
		dashboard.append(widget);

		menuController.showSidebar();
		$(document.body).addClass('tinted-background');
		$('#content').css('visibility', 'visible');
	}

	_createDashboardWidget(icon, title, url) {
		let link = $('<a></a>');
		link.addClass('dashboard-widget');
		if (url)
			link.attr('href', url);
		else
			link.attr('href', '#');
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
		if (url) {
			animator.addAnimatedClickHandler(link, link,
				'animate-dashboard-widget-click',
				null,
				function() {
					window.location.href = url;
				}
			);
		}
		return link;
	}

	_onLogoutClick(clickId) {
		let client = new SenSeeActClient();
		var self = this;
		client.logout()
			.done(function(result) {
				self._onLogoutDone(clickId, result);
			})
			.fail(function(xhr, status, error) {
				self._onLogoutFail(clickId, xhr, status, error);
			});
	}

	_onLogoutDone(clickId, result) {
		animator.onAnimatedClickHandlerCompleted(clickId, true);
	}

	_onLogoutFail(clickId, xhr, status, error) {
		animator.onAnimatedClickHandlerCompleted(clickId, false);
	}

	_onLogoutCompleted(success) {
		if (success)
			window.location.href = basePath + '/';
		else
			showToast(i18next.t('unexpected_error'));
	}
}
