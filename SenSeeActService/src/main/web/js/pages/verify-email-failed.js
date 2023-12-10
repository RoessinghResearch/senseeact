class VerifyEmailFailedPage {
	constructor() {
		let params = parseURL(window.location.href).params;
		let invalidInput = params['invalid-input'] == 'true';
		if (invalidInput)
			$('#verify-error').text(i18next.t('confirm_email_failed'));
		else
			$('#verify-error').text(i18next.t('unexpected_error'));
		this._createView();
	}
	
	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();
		$(document.body).addClass('tinted-background');
		$('#content').css('visibility', 'visible');
	}
}

new VerifyEmailFailedPage();
