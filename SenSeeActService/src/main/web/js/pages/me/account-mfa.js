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
		this._addDialogue = null;
		$('#mfa-title').text(i18next.t('mfa_title'));
		$('#mfa-intro').text(i18next.t('mfa_intro'));
		this._loadRecords();
	}

	_loadRecords() {
		var self = this;
		let url = servicePath + '/auth/mfa/list';
		$.ajax({
			url: url
		})
		.done((result) => {
			self._onGetMfaListDone(result);
		})
		.fail((xhr, status, error) => {
			showToast(i18next.t('unexpected_error'));
		});
	}

	_onGetMfaListDone(records) {
		var self = this;
		let form = $('#mfa-form-content');
		let waitCircle = form.find('.wait-circle');
		waitCircle.hide();
		let container = $('#mfa-records-list');
		container.empty();
		for (let i = 0; i < records.length; i++) {
			let record = records[i];
			if (record.type == 'totp')
				this._showTotpRecord(record);
			else if (record.type == 'sms')
				this._showSmsRecord(record);
		}
		let button = $('#mfa-add-button');
		if (records.length > 0)
			button.text(i18next.t('add'));
		else
			button.text(i18next.t('enable'));
		button.show();
		animator.clearAnimatedClickHandler(button);
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click',
			null,
			() => {
				self._onAddClick(records);
			}
		);
	}

	_showTotpRecord(record) {
		let container = $('#mfa-records-list');
		let date = luxon.DateTime.fromISO(record.created)
			.setLocale(i18next.resolvedLanguage);
		let recordDiv = $('<div></div>');
		recordDiv.addClass('mfa-record');
		container.append(recordDiv);

		let contentDiv = $('<div></div>');
		contentDiv.addClass('mfa-record-content');
		recordDiv.append(contentDiv);
		let titleDiv = $('<div></div>');
		titleDiv.addClass('mfa-record-title');
		titleDiv.text(i18next.t('authenticator_app'));
		contentDiv.append(titleDiv);
		let dateDiv = $('<div></div>');
		dateDiv.addClass('mfa-record-date');
		contentDiv.append(dateDiv);
		dateDiv.text(date.toLocaleString(luxon.DateTime.DATE_FULL));

		let buttonsDiv = $('<div></div>');
		buttonsDiv.addClass('mfa-record-buttons');

		let button = $('<button></button>');
		buttonsDiv.append(button);
		button.addClass('icon');
		let icon = basePath + '/images/icon_scan_qrcode.svg';
		let iconUrl = "url('" + icon + "')";
		button.css('mask-image', iconUrl);
		button.css('-webkit-mask-image', iconUrl);
		var self = this;
		button.on('click', () => {
			self._onShowQRCodeClick(record);
		});

		button = $('<button></button>');
		buttonsDiv.append(button);
		button.addClass('icon');
		icon = basePath + '/images/icon_trash_can.svg';
		iconUrl = "url('" + icon + "')";
		button.css('mask-image', iconUrl);
		button.css('-webkit-mask-image', iconUrl);
		var self = this;
		button.on('click', () => {
			self._onDeleteRecordClick(record);
		});
		recordDiv.append(buttonsDiv);
	}

	_showSmsRecord(record) {
	}

	_onShowQRCodeClick(record) {
		let dlg = Dialogue.openDialogue();
		dlg.initMainForm(i18next.t('qr_code'));

		let dlgContent = dlg.dialogueContentDiv;
		this._addTotpQRCodeInstructions(dlg, dlgContent);
		let imgBox = $('#mfa-add-totp-qr-box');
		let img = $('<img></img>');
		img.attr('src', servicePath + '/auth/mfa/add/totp/qrcode?id=' +
			record.id);
		imgBox.append(img);

		dlg.centralButtons.addButton(i18next.t('ok'), null,
			() => {
				dlg.close();
			}
		);
	}

	_onDeleteRecordClick(record) {
		var self = this;
		let dlg = Dialogue.openDialogue();
		dlg.initMainForm(i18next.t('delete'));
		dlg.addText(i18next.t('mfa_confirm_delete'));
		dlg.centralButtons.addButton(i18next.t('yes'),
			(clickId) => {
				self._onDeleteRecordYesClick(clickId, record);
			},
			(result) => {
				self._onDeleteRecordYesCallback(result, dlg);
			});
		dlg.centralButtons.addButton(i18next.t('no'), null,
			() => {
				dlg.close();
			}
		);
	}

	_onDeleteRecordYesClick(clickId, record) {
		let url = servicePath + '/auth/mfa?id=' + record.id;
		$.ajax({
			method: 'DELETE',
			url: url,
			mimeType: 'text/plain'
		})
		.done(() => {
			animator.onAnimatedClickHandlerCompleted(clickId, true);
		})
		.fail(() => {
			animator.onAnimatedClickHandlerCompleted(clickId, false);
		});
	}

	_onDeleteRecordYesCallback(result, dlg) {
		if (result) {
			dlg.close();
			this._loadRecords();
		} else {
			showToast(i18next.t('unexpected_error'));
		}
	}

	_onAddClick(records) {
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
		this._addErrorCard(dlg, cardContainer, 'mfa-totp-max-card',
			i18next.t('mfa_error_type_totp_max'));
		this._addErrorCard(dlg, cardContainer, 'mfa-add-verify-max-card',
			i18next.t('mfa_error_add_verify_max'));
		dlg.leftButtons.addCancelButton();
		this._continueButton = dlg.rightButtons.addSubmitButton(
			i18next.t('continue'), null,
			(result) => {
				self._onMfaTypeContinueClick(dlg, records);
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
		this._addTotpQRCodeInstructions(dlg, card);
		let waitCircle = $('<div></div>');
		waitCircle.addClass('wait-circle');
		let imgBox = $('#mfa-add-totp-qr-box');
		imgBox.append(waitCircle);
		let textDiv = dlg.createText(i18next.t('mfa_add_totp_enter_code'));
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

	_addTotpQRCodeInstructions(dlg, container) {
		let textDiv = dlg.createText(i18next.t(
			'mfa_add_totp_install_authenticator'));
		textDiv.addClass('small-text');
		container.append(textDiv);
		textDiv = dlg.createText(i18next.t('mfa_add_totp_scan_qr'));
		textDiv.addClass('small-text');
		textDiv.css('margin-top', '8px');
		container.append(textDiv);
		let imgBox = $('<div></div>');
		imgBox.attr('id', 'mfa-add-totp-qr-box');
		container.append(imgBox);
	}

	_addErrorCard(dlg, cardContainer, cardId, text) {
		let card = $('<div></div>');
		card.attr('id', cardId);
		card.addClass('card');
		card.css('visibility', 'hidden');
		cardContainer.append(card);
		let lines = text.split('\n');
		for (let i = 0; i < lines.length; i++) {
			let textDiv = dlg.createText(lines[i]);
			textDiv.addClass('error-card-text');
			card.append(textDiv);
		}
	}

	_onMfaTypeContinueClick(dlg, records) {
		let dlgContent = dlg.dialogueContentDiv;
		let totpRadio = dlgContent.find('#mfatype-totp');
		if (totpRadio.prop('checked')) {
			this._onMfaTypeTotpContinueClick(dlg, records);
			return;
		}
		let smsRadio = dlgContent.find('#mfatype-sms');
		if (smsRadio.prop('checked')) {
			this._onMfaTypeSmsContinueClick(dlg, records);
			return;
		}
	}

	_onMfaTypeTotpContinueClick(dlg, records) {
		if (this._countMfaType('totp', records) >= 1) {
			this._showErrorCard(dlg, 'mfa-totp-max-card');
			return;
		}
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

	_countMfaType(type, records) {
		let count = 0;
		for (let i = 0; i < records.length; i++) {
			let record = records[i];
			if (record.type == type)
				count++;
		}
		return count;
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
				success: false,
				error: null
			});
			return;
		}
		this._runTotpVerify(dlg, code, clickId);
	}

	_runTotpVerify(dlg, code, clickId) {
		this._totpVerifyCodeEdit.hideError();
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
		if (clickId) {
			animator.onAnimatedClickHandlerCompleted(clickId, {
				success: true
			});
		} else {
			this._handleAddTotpVerifyDone(dlg);
		}
	}

	_onAddTotpVerifyFail(dlg, clickId, xhr) {
		if (clickId) {
			animator.onAnimatedClickHandlerCompleted(clickId, {
				success: false,
				error: xhr
			});
		} else {
			this._handleAddTotpVerifyFail(dlg, xhr);
		}
	}

	_onAddTotpOkClickDone(dlg, result) {
		if (result.success)
			this._handleAddTotpVerifyDone(dlg);
		else if (result.error)
			this._handleAddTotpVerifyFail(dlg, result.error);
	}

	_handleAddTotpVerifyDone(dlg) {
		if (dlg != this._addDialogue)
			return;
		this._totpVerifyRunning = false;
		dlg.close();
		this._loadRecords();
	}

	_handleAddTotpVerifyFail(dlg, xhr) {
		if (dlg != this._addDialogue)
			return;
		this._totpVerifyRunning = false;
		let client = new SenSeeActClient();
		if (xhr.status == 400 && xhr.responseJSON) {
			if (xhr.responseJSON.code == 'AUTH_MFA_TYPE_MAX') {
				this._showErrorCard(dlg, 'mfa-totp-max-card');
			} else if (xhr.responseJSON.code == 'AUTH_MFA_VERIFY_MAX') {
				showToast(i18next.t('mfa_error_verify_max'));
			} else if (client.hasInvalidInputField(xhr, 'code')) {
				let codeEdit = this._totpVerifyCodeEdit;
				codeEdit.showError();
				codeEdit.focus();
			} else {
				showToast(i18next.t('unexpected_error'));
			}
		} else if (xhr.status == 404) {
			this._showErrorCard(dlg, 'mfa-add-verify-max-card');
		} else {
			showToast(i18next.t('unexpected_error'));
		}
	}

	_onMfaTypeSmsContinueClick(dlg, records) {

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
