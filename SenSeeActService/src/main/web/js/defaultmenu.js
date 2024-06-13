class DefaultMenuController {
	constructor() {
		menuController.appendMenuItem('home',
			i18next.t('home'),
			basePath + '/');
		menuController.appendMenuItem('me',
			i18next.t('my_senseeact'),
			basePath + '/me');
		menuController.appendSubMenuItem('me', 'me-account',
			i18next.t('my_account'),
			basePath + '/me/account');
		menuController.appendSubMenuItem('me', 'me-download',
			i18next.t('download_data'),
			basePath + '/me/download');
	}
}

new DefaultMenuController();
