class MyAccountMfaForm {
	/**
	 * Properties:
	 * - _addDialogue (Dialogue)
	 * - _continueButton (jQuery element)
	 * - _totpVerifyCodeEdit (NumCodeEdit)
	 * - _addTotpState: object with the following property:
	 *     - mfaId (string)
	 * - _totpVerifyRunning (boolean)
	 */

	constructor() {
		var self = this;
		this._addDialogue = null;
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
		var self = this;
		let dlg = Dialogue.openDialogue();
		this._addDialogue = dlg;
		dlg.onclose(() => {
			self._onAddDialogueClose();
		});
		dlg.initMainForm(i18next.t('mfa_title'));
		let dlgContent = dlg.dialogueContentDiv;
		let cardContainer = $('<div></div>');
		cardContainer.addClass('card-container');
		dlgContent.append(cardContainer);
		this._addMfaTypeCard(dlg, cardContainer);
		this._addMfaTypeTotpCard(dlg, cardContainer);
		this._addTotpMaxCard(dlg, cardContainer);
		dlg.leftButtons.addCancelButton();
		this._continueButton = dlg.rightButtons.addSubmitButton(
			i18next.t('continue'), null,
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
			'mfa_add_totp_install_authenticator'));
		textDiv.addClass('small-text');
		card.append(textDiv);
		textDiv = dlg.createText(i18next.t('mfa_add_totp_scan_qr'));
		textDiv.addClass('small-text');
		textDiv.css('margin-top', '8px');
		card.append(textDiv);
		let imgBox = $('<div></div>');
		imgBox.attr('id', 'mfa-add-totp-qr-box');
		card.append(imgBox);
		let waitCircle = $('<div></div>');
		waitCircle.addClass('wait-circle');
		imgBox.append(waitCircle);
		textDiv = dlg.createText(i18next.t('mfa_add_totp_enter_code'));
		textDiv.addClass('small-text');
		textDiv.css('margin-top', '8px');
		card.append(textDiv);
		let numCodeEditDiv = $('<div></div>');
		numCodeEditDiv.attr('id', 'mfa-add-totp-verify-code');
		let numCodeEdit = new NumCodeEdit(numCodeEditDiv);
		this._totpVerifyCodeEdit = numCodeEdit;
		this._addTotpState = {
			mfaId: null
		};
		this._totpVerifyRunning = false;
		numCodeEdit.render();
		card.append(numCodeEditDiv);
	}

	_addTotpMaxCard(dlg, cardContainer) {
		let card = $('<div></div>');
		card.attr('id', 'mfa-totp-max-card');
		card.addClass('card');
		card.css('visibility', 'hidden');
		cardContainer.append(card);
		let text = i18next.t('mfa_error_type_totp_max');
		let lines = text.split('\n');
		for (let i = 0; i < lines.length; i++) {
			let textDiv = dlg.createText(lines[i]);
			card.append(textDiv);
		}
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
		var self = this;
		let button = this._continueButton;
		animator.clearAnimatedClickHandler(button);
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click',
			(clickId) => {
				self._onAddTotpOkClick(dlg, clickId);
			},
			(result) => {
				self._onAddTotpOkClickDone(dlg, result);
			}
		);
		this._continueButton.text(i18next.t('ok'));
		let dlgContent = dlg.dialogueContentDiv;
		dlgContent.find('.card').css('visibility', 'hidden');
		let addTotpCard = dlgContent.find('#mfa-add-totp-card');
		addTotpCard.css('visibility', 'visible');
		this._totpVerifyCodeEdit.focus();
		let url = servicePath + '/auth/mfa/add?type=totp';
		var self = this;
		$.ajax({
			method: 'POST',
			url: url
		})
		.done((result) => {
			self._onAddMfaTotpDone(dlg, result);
		})
		.fail((xhr, status, error) => {
			self._onAddMfaTotpFail(dlg, xhr);
		});
	}

	_onAddMfaTotpDone(dlg, result) {
		if (dlg != this._addDialogue)
			return;
		let dlgContent = dlg.dialogueContentDiv;
		let addTotpCard = dlgContent.find('#mfa-add-totp-card');
		let imgBox = addTotpCard.find('#mfa-add-totp-qr-box');
		imgBox.empty();
		let mfaId = result['id'];
		this._addTotpState.mfaId = mfaId;
		let img = $('<img></img>');
		img.attr('src', servicePath + '/auth/mfa/add/totp/qrcode?id=' + mfaId);
		imgBox.append(img);
		var self = this;
		let numCodeEdit = this._totpVerifyCodeEdit;
		numCodeEdit.onenter((edit, code) => {
			self._onEnterTotpCode(dlg, code);
		});
	}

	_onAddMfaTotpFail(dlg, xhr) {
		if (dlg != this._addDialogue)
			return;
		if (xhr.status == 400 && xhr.responseJSON) {
			if (xhr.responseJSON.code == 'AUTH_MFA_TYPE_MAX') {
				this._showErrorCard(dlg, 'mfa-totp-max-card');
			} else if (xhr.responseJSON.code == 'AUTH_MFA_ADD_MAX') {
				showToast(i18next.t('mfa_error_add_max'));
			} else {
				showToast(i18next.t('unexpected_error'));
			}
		} else {
			showToast(i18next.t('unexpected_error'));
		}
	}

	_onEnterTotpCode(dlg, code) {
		if (!this._totpVerifyRunning)
			this._runTotpVerify(dlg, code, null);
	}

	_onAddTotpOkClick(dlg, clickId) {
		let state = this._addTotpState;
		let code = this._totpVerifyCodeEdit.code;
		if (!state.mfaId || !code || this._totpVerifyRunning) {
			animator.onAnimatedClickHandlerCompleted(clickId, {
				success: false
			});
			return;
		}
		this._runTotpVerify(dlg, code, clickId);
	}

	_runTotpVerify(dlg, code, clickId) {
		this._totpVerifyRunning = true;
		let url = servicePath + '/auth/mfa/add/verify';
		let mfaId = this._addTotpState.mfaId;
		let data = {
			'mfaId': mfaId,
			'code': code
		}
		var self = this;
		$.ajax({
			method: 'POST',
			url: url,
			data: JSON.stringify(data),
			contentType: 'application/json'
		})
		.done((result) => {
			self._onAddTotpVerifyDone(dlg, clickId);
		})
		.fail((xhr, status, error) => {
			self._onAddTotpVerifyFail(dlg, clickId, xhr);
		});
	}

	_onAddTotpVerifyDone(dlg, clickId) {
		if (dlg != this._addDialogue)
			return;
		this._totpVerifyRunning = false;
		if (clickId) {
			animator.onAnimatedClickHandlerCompleted(clickId, {
				success: true
			});
		} else {
			this._completeAddTotp(dlg);
		}
	}

	_onAddTotpVerifyFail(dlg, clickId, xhr) {
		if (dlg != this._addDialogue)
			return;
		this._totpVerifyRunning = false;
		if (clickId) {
			animator.onAnimatedClickHandlerCompleted(clickId, {
				success: false
			});
		}
	}

	_onAddTotpOkClickDone(dlg, result) {
		if (result.success) {
			this._completeAddTotp(dlg);
			return;
		}
	}

	_completeAddTotp(dlg) {
		dlg.close();
	}

	_onMfaTypeSmsContinueClick(dlg) {

	}

	_showErrorCard(dlg, cardId) {
		let button = this._continueButton;
		animator.clearAnimatedClickHandler(button);
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click',
			null,
			(result) => {
				dlg.close();
			}
		);
		this._continueButton.text(i18next.t('ok'));
		let dlgContent = dlg.dialogueContentDiv;
		dlgContent.find('.card').css('visibility', 'hidden');
		let card = dlgContent.find('#' + cardId);
		card.css('visibility', 'visible');
	}

	_onAddDialogueClose() {
		this._addDialogue = null;
	}
}
