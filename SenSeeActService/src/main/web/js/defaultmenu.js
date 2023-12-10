class DefaultMenuController {
	constructor() {
		menuController.appendMenuItem(
			i18next.t('home'),
			basePath + '/');
		menuController.appendMenuItem(
			i18next.t('my_senseeact'),
			basePath + '/me');
	}
}

new DefaultMenuController();
