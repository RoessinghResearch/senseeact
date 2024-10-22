class MyAccountMfaForm {
	constructor() {
		$('#mfa-title').text(i18next.t('mfa_title'));
		$('#mfa-intro').text(i18next.t('mfa_intro'));
		let button = $('#mfa-add-button');
		button.text(i18next.t('enable'));
	}
}
