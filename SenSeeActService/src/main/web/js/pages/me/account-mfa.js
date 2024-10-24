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
		let dlgContent = dlg.dialogueContentDiv;
		let cardContainer = $('<div></div>');
		cardContainer.addClass('card-container');
		dlgContent.append(cardContainer);
		this._addMfaTypeCard(dlg, cardContainer);
		this._addMfaTypeTotpCard(dlg, cardContainer);
		dlg.leftButtons.addCancelButton();
		var self = this;
		dlg.rightButtons.addSubmitButton(i18next.t('continue'), null,
			(result) => {
				self._onMfaTypeContinueClick(dlg);
			}
		);
	}

	_addMfaTypeCard(dlg, cardContainer) {
		let card = $('<div></div>');
		card.attr('id', 'mfa-type-card');
		card.addClass('card');
		cardContainer.append(card);
		let textDiv = dlg.createText(i18next.t('mfatype_intro'));
		textDiv.addClass('small-text');
		card.append(textDiv);
		let radio = dlg.createRadioInput('mfatype-totp', 'mfatype',
			i18next.t('authenticator_app'), true);
		card.append(radio.fieldDiv);
		textDiv = dlg.createText(i18next.t('mfatype_totp_explanation'));
		textDiv.addClass('radio-explanation');
		card.append(textDiv);
		radio = dlg.createRadioInput('mfatype-sms', 'mfatype',
			i18next.t('sms'), false);
		card.append(radio.fieldDiv);
		textDiv = dlg.createText(i18next.t('mfatype_sms_explanation'));
		textDiv.addClass('radio-explanation');
		card.append(textDiv);
	}

	_addMfaTypeTotpCard(dlg, cardContainer) {
		let card = $('<div></div>');
		card.attr('id', 'mfa-add-totp-card');
		card.addClass('card');
		card.css('visibility', 'hidden');
		cardContainer.append(card);
		let textDiv = dlg.createText(i18next.t(
			'mfatype_totp_install_authenticator'));
		textDiv.addClass('small-text');
		card.append(textDiv);
		textDiv = dlg.createText(i18next.t('mfatype_totp_add_code'));
		textDiv.addClass('small-text');
		textDiv.css('margin-top', '8px');
		card.append(textDiv);
		let imgBox = $('<div></div>');
		imgBox.attr('id', 'mfa-add-totp-qr-box');
		card.append(imgBox);
		let waitCircle = $('<div></div>');
		waitCircle.addClass('wait-circle');
		imgBox.append(waitCircle);
	}

	_onMfaTypeContinueClick(dlg) {
		let dlgContent = dlg.dialogueContentDiv;
		let totpRadio = dlgContent.find('#mfatype-totp');
		if (totpRadio.prop('checked')) {
			this._onMfaTypeTotpContinueClick(dlg);
			return;
		}
		let smsRadio = dlgContent.find('#mfatype-sms');
		if (smsRadio.prop('checked')) {
			this._onMfaTypeSmsContinueClick(dlg);
			return;
		}
	}

	_onMfaTypeTotpContinueClick(dlg) {
		let dlgContent = dlg.dialogueContentDiv;
		let mfaTypeCard = dlgContent.find('#mfa-type-card');
		mfaTypeCard.css('visibility', 'hidden');
		let addTotpCard = dlgContent.find('#mfa-add-totp-card');
		addTotpCard.css('visibility', 'visible');
		let url = servicePath + '/auth/mfa/add?type=totp';
		var self = this;
		$.ajax({
			method: 'POST',
			url: url
		})
		.done((result) => {
			self._onAddMfaTotpDone(result);
		})
		.fail((xhr, status, error) => {
			self._onAddMfaTotpFail(xhr);
		});
	}

	_onAddMfaTotpDone(result) {
	}

	_onAddMfaTotpFail(xhr) {
		showToast(i18next.t('unexpected_error'));
	}

	_onMfaTypeSmsContinueClick(dlg) {

	}
}
