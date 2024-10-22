/**
 * Properties:
 * - _dlgRoot: jQuery element #dialogue-root
 * - _dlgContent: jQuery element .dialogue-content
 * - _fieldEditors: object that maps field names to editor objects
 * - _onclose: Callback function with this signature:
 *       void function()
 *       Called when the user clicks the overlay behind the dialogue window
 *       or a cancel button.
 */
class Dialogue {

	/**
	 * Opens an empty dialogue window. It will show the dialogue overlay and
	 * registers a click handler on the overlay outside the dialogue window that
	 * will close the dialogue. You can register an onclose handler with
	 * onclose().
	 * 
	 * After opening the dialogue, you can call initMainForm() and then add
	 * input fields and buttons.
	 * 
	 * Returns (Dialogue): the dialogue
	 */
	static openDialogue() {
		let result = new Dialogue();
		result._dlgContent = null;
		result._fieldEditors = {};
		result._onclose = null;
		let overlay = $('#main-root-overlay');
		overlay.show();
		overlay.off('click');
		overlay.on('click', function() {
			result.close();
		});
		let dlgRoot = $('#dialogue-root');
		result._dlgRoot = dlgRoot;
		dlgRoot.css('display', 'grid');
		dlgRoot.off('click');
		dlgRoot.on('click', function() {
			result.close();
		});
		let dlgDiv = dlgRoot.find('#dialogue');
		dlgDiv.empty();
		dlgDiv.off('click');
		dlgDiv.on('click', function(ev) {
			ev.stopPropagation();
		});
		return result;
	}

	/**
	 * Returns the element #dialogue-root.
	 */
	get dialogueRoot() {
		return this._dlgRoot;
	}

	/**
	 * Returns the element .dialogue.content that is created after
	 * initMainForm();
	 */
	get dialogueContentDiv() {
		return this._dlgContent;
	}

	/**
	 * Registers a handler that is called when the dialogue window is closed.
	 * 
	 * - onclose: function with this signature:
	 *       void onclose()
	 */
	onclose(onclose) {
		this._onclose = onclose;
	}
	
	/**
	 * Initializes the main form of the dialogue. It adds some formatting and
	 * a header text. You should call this method before adding any input fields
	 * or buttons.
	 * 
	 * - headerText (String): the header text
	 */
	initMainForm(headerText) {
		let form = $('<form></form>');
		form.addClass('dialogue-form');
		form.submit(function(e) {
			e.preventDefault();
		});
		let dlgDiv = this._dlgRoot.find('#dialogue');
		dlgDiv.append(form);
		let content = $('<div></div>');
		this._dlgContent = content;
		content.addClass('dialogue-content');
		form.append(content);
		let header = $('<div></div>');
		header.addClass('dialogue-header');
		header.text(headerText);
		content.append(header);
	}

	/**
	 * Adds a text div.
	 * 
	 * - text (String): the text
	 * 
	 * Returns (jQuery element): the text div
	 */
	addText(text) {
		let content = this._dlgContent;
		let textDiv = $('<div></div>');
		textDiv.addClass('dialogue-text');
		textDiv.text(text);
		content.append(textDiv);
		return textDiv;
	}

	/**
	 * Adds a text input, consisting of a label, text input field and error
	 * field.
	 * 
	 * - name (String): the field ID. The container div will get ID
	 *   'dialogue-field-<name>' and the text input field will get ID
	 *   '<name>'.
	 * - label (String): the label text
	 * - initialValue (String): the initial value or null
	 * 
	 * Returns (TextEdit): the text editor
	 */
	addTextInput(name, label, initialValue = null) {
		let content = this._dlgContent;
		let textInput = this.createTextInput(name, label, initialValue);
		content.append(textInput.fieldDiv);
		return textInput.edit;
	}

	/**
	 * Creates a text input, consisting of a label, text input field and error
	 * field.
	 * 
	 * - name (String): the field ID. The container div will get ID
	 *   'dialogue-field-<name>' and the text input field will get ID
	 *   '<name>'.
	 * - label (String): the label text
	 * - initialValue (String): the initial value or null
	 * 
	 * Returns: object with the following properties:
	 *   - fieldDiv: the container div
	 *   - edit (TextEdit): the text editor
	 */
	createTextInput(name, label, initialValue = null) {
		let fieldDiv = $('<div></div>');
		fieldDiv.attr('id', 'dialogue-field-' + name);
		fieldDiv.addClass('dialogue-field');
		let labelDiv = $('<div></div>');
		labelDiv.addClass('dialogue-field-label');
		labelDiv.text(label);
		fieldDiv.append(labelDiv);
		let edit = new TextEdit();
		edit.id = name;
		fieldDiv.append(edit.element);
		if (initialValue)
			edit.input.val(initialValue);
		let errorDiv = $('<div></div>');
		errorDiv.addClass('dialogue-field-error');
		errorDiv.hide();
		fieldDiv.append(errorDiv);
		this._fieldEditors[name] = edit;
		return {
			fieldDiv: fieldDiv,
			edit: edit
		};
	}

	/**
	 * Adds a checkbox input.
	 * 
	 * - name (string): the field ID. The container div will get ID
	 *     'dialogue-field-<name>' and the checkbox will get ID '<name>'.
	 * - label (string): the label text
	 * - initialValue (boolean): the initial value
	 * 
	 * Returns: the checkbox element
	 */
	addCheckboxInput(name, label, initialValue = false) {
		let content = this._dlgContent;
		let input = this.createCheckboxInput(name, label, initialValue);
		content.append(input.fieldDiv);
		return input.checkbox;
	}

	/**
	 * Creates a checkbox input.
	 * 
	 * - name (string): the field ID. The container div will get ID
	 *     'dialogue-field-<name>' and the checkbox will get ID '<name>'.
	 * - label (string): the label text
	 * - initialValue (boolean): the initial value
	 * 
	 * Returns: object with the following properties:
	 *   - fieldDiv: the container div
	 *   - checkbox: the checkbox element
	 */
	createCheckboxInput(name, label, initialValue = false) {
		let fieldDiv = $('<div></div>');
		fieldDiv.attr('id', 'dialogue-field-' + name);
		fieldDiv.addClass('dialogue-field');
		let checkContainer = $('<div></div>');
		checkContainer.addClass('dialogue-field-checkbox-container');
		fieldDiv.append(checkContainer);
		let checkbox = $('<input></input>');
		checkbox.attr('type', 'checkbox');
		checkbox.attr('id', name);
		checkbox.attr('name', name);
		checkbox.addClass('checkbox');
		checkbox.prop('checked', initialValue);
		checkContainer.append(checkbox);
		let labelDiv = $('<label></label>');
		labelDiv.addClass('dialogue-field-checkbox-label');
		labelDiv.attr('for', name);
		labelDiv.text(label);
		checkContainer.append(labelDiv);
		this._fieldEditors[name] = checkbox;
		return {
			fieldDiv: fieldDiv,
			checkbox: checkbox
		};
	}

	/**
	 * Adds a radio button input.
	 * 
	 * - name (string): the field ID. The container div will get ID
	 *     'dialogue-field-<name>' and the radio button will get ID '<name>'.
	 * - group (string): the group ID. This should be the same for all radio
	 *     buttons in the group.
	 * - label (string): the label text
	 * - initialValue (boolean): the initial value
	 * 
	 * Returns: the radio button
	 */
	addRadioInput(name, group, label, initialValue = false) {
		let content = this._dlgContent;
		let input = this.createRadioInput(name, group, label, initialValue);
		content.append(input.fieldDiv);
		return input.radio;
	}

	/**
	 * Creates a radio button input.
	 * 
	 * - name (string): the field ID. The container div will get ID
	 *     'dialogue-field-<name>' and the radio button will get ID '<name>'.
	 * - group (string): the group ID. This should be the same for all radio
	 *     buttons in the group.
	 * - label (string): the label text
	 * - initialValue (boolean): the initial value
	 * 
	 * Returns: object with the following properties:
	 *   - fieldDiv: the container div
	 *   - radio: the radio button
	 */
	createRadioInput(name, group, label, initialValue = false) {
		let fieldDiv = $('<div></div>');
		fieldDiv.attr('id', 'dialogue-field-' + name);
		fieldDiv.addClass('dialogue-field');
		let radioContainer = $('<div></div>');
		radioContainer.addClass('dialogue-field-radio-container');
		fieldDiv.append(radioContainer);
		let radio = $('<input></input>');
		radio.attr('type', 'radio');
		radio.attr('id', name);
		radio.attr('name', group);
		radio.addClass('radio');
		radio.prop('checked', initialValue);
		radioContainer.append(radio);
		let labelDiv = $('<label></label>');
		labelDiv.addClass('dialogue-field-radio-label');
		labelDiv.attr('for', name);
		labelDiv.text(label);
		radioContainer.append(labelDiv);
		this._fieldEditors[name] = radio;
		return {
			fieldDiv: fieldDiv,
			radio: radio
		};
	}

	get leftButtons() {
		return this._getButtonPanel('left');
	}

	get centralButtons() {
		return this._getButtonPanel('central');
	}

	get rightButtons() {
		return this._getButtonPanel('right');
	}

	clearButtons() {
		let parts = ['left', 'central', 'right'];
		for (let i = 0; i < parts.length; i++) {
			let panel = this._getButtonPanel(parts[i]);
			panel.clearButtons();
		}
	}

	_getButtonPanel(part) {
		let buttonPanel = this._dlgRoot.find('.dialogue-button-panel');
		if (buttonPanel.length != 0) {
			let partDiv = buttonPanel.find('.dialogue-button-panel-' +
				part);
			return new Dialogue_ButtonPanel(this, partDiv);
		}
		buttonPanel = $('<div></div>');
		buttonPanel.addClass('dialogue-button-panel');
		let parts = ['left', 'central', 'right'];
		let resultDiv;
		for (let i = 0; i < parts.length; i++) {
			let partDiv = $('<div></div>');
			partDiv.addClass('dialogue-button-panel-part');
			partDiv.addClass('dialogue-button-panel-' + parts[i]);
			buttonPanel.append(partDiv);
			if (part == parts[i])
				resultDiv = partDiv;
		}
		let form = this._dlgRoot.find('.dialogue-form');
		form.append(buttonPanel);
		return new Dialogue_ButtonPanel(this, resultDiv);
	}

	/**
	 * Closes the dialogue window.
	 */
	close() {
		let overlay = $('#main-root-overlay');
		overlay.hide();
		let dlgRoot = this._dlgRoot;
		dlgRoot.css('display', 'none');
		let dlgDiv = dlgRoot.find('#dialogue');
		dlgDiv.empty();
		if (this._onclose)
			this._onclose();
	}

	clearErrors() {
		let dlgRoot = this._dlgRoot;
		dlgRoot.find('*').removeClass('error');
		dlgRoot.find('.dialogue-field-error').hide();
	}

	/**
	 * Returns the text editor (TextEdit) for the specified text input
	 * field.
	 * 
	 * - name (string): name of the text input field
	 * 
	 * Returns (TextEdit): the text editor
	 */
	getTextInputEdit(name) {
		return this._fieldEditors[name];
	}

	/**
	 * Reads the input value of the text input field with the specified name.
	 * 
	 * - name (string): name of the text input field
	 * 
	 * Returns (string): the input value
	 */
	readTextInput(name) {
		return this._fieldEditors[name].input.val();
	}

	/**
	 * Shows an error at the text input field with the specified name. It marks
	 * the label and text input field red, and it triggers focus to the input
	 * field. It can also show an error text.
	 * 
	 * - name (String): name of the text input field
	 * - errorText (String): the error text or null
	 */
	showTextInputError(name, errorText = null) {
		let fieldDiv = this._dlgRoot.find('#dialogue-field-' + name);
		let labelDiv = fieldDiv.find('.dialogue-field-label');
		labelDiv.addClass('error');
		this._fieldEditors[name].showError();
		if (errorText) {
			let errorDiv = fieldDiv.find('.dialogue-field-error');
			errorDiv.text(errorText);
			errorDiv.show();
		}
	}

	/**
	 * Returns whether the checkbox field with the specified name is checked.
	 * 
	 * - name (string): name of the checkbox input field
	 * 
	 * Returns (boolean): true if the checkbox is checked, false otherwise
	 * 
	 */
	readCheckboxInput(name) {
		return this._fieldEditors[name].prop('checked');
	}
}

class Dialogue_ButtonPanel {
	constructor(dialogue, panelDiv) {
		this._dialogue = dialogue;
		this._panelDiv = panelDiv;
	}

	get panelDiv() {
		return this._panelDiv;
	}

	clearButtons() {
		this._panelDiv.empty();
	}

	/**
	 * Adds a submit button. This is the default button that will be activated
	 * when the user presses Enter. Button clicks are handled by
	 * ElementAnimator.
	 * 
	 * - text (String): the button text
	 * - asyncHandler: asynchronous handler function with this signature:
	 *       void asyncHandler(clickId)
	 *       The function is called immediately when the button is clicked. It
	 *       can start an asynchronous action. Eventually the handler must call
	 *       animator.onAnimatedClickHandlerCompleted().
	 *       You can set this to null.
	 * - callback: callback function with this signature:
	 *       void callback(result)
	 *       This is called when both the asynchronous action (if any) and the
	 *       animation are completed. It receives the result that was set with
	 *       onAnimatedClickHandlerCompleted() or null.
	 */
	addSubmitButton(text, asyncHandler, callback) {
		let button = $('<button></button>');
		button.addClass('small');
		button.attr('type', 'submit');
		button.text(text);
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click', asyncHandler, callback);
		this._panelDiv.append(button);
	}

	/**
	 * Adds a normal button. Button clicks are handled by ElementAnimator.
	 * See also addSubmitButton() and addCancelButton().
	 * 
	 * - text (String): the button text
	 * - asyncHandler: asynchronous handler function with this signature:
	 *       void asyncHandler(clickId)
	 *       The function is called immediately when the button is clicked. It
	 *       can start an asynchronous action. Eventually the handler must call
	 *       animator.onAnimatedClickHandlerCompleted().
	 *       You can set this to null.
	 * - callback: callback function with this signature:
	 *       void callback(result)
	 *       This is called when both the asynchronous action (if any) and the
	 *       animation are completed. It receives the result that was set with
	 *       onAnimatedClickHandlerCompleted() or null.
	 */
	addButton(text, asyncHandler, callback) {
		let button = $('<button></button>');
		button.addClass('small');
		button.attr('type', 'button');
		button.text(text);
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click', asyncHandler, callback);
		this._panelDiv.append(button);
	}

	/**
	 * Adds a cancel button. The button will just close the window.
	 */
	addCancelButton() {
		let button = $('<button></button>');
		button.addClass('small');
		button.attr('type', 'button');
		button.text(i18next.t('cancel'));
		var self = this;
		animator.addAnimatedClickHandler(button, button,
			'animate-blue-button-click',
			null,
			function() {
				self._dialogue.close();
			});
		this._panelDiv.append(button);
	}
}
