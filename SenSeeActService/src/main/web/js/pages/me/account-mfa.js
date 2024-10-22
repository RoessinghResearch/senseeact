class MyAccountMfaForm {
	constructor() {
		var self = this;
		$('#mfa-title').text(i18next.t('mfa_title'));
		$('#mfa-intro').text(i18next.t('mfa_intro'));
		let button = $('#mfa-add-button');
		button.text(i18next.t('enable'));
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click',
			null,
			(result) => {
				self._onAddClick();
			}
		);
	}

	_onAddClick() {
		let dlg = Dialogue.openDialogue();
		dlg.initMainForm(i18next.t('mfa_title'));
		dlg.centralButtons.addCancelButton();
	}
}
