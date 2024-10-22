class TextEdit {
	constructor() {
		let element = $('<div></div>');
		this._element = element;
		this._transformer = null;
		this._disabled = false;
		element.addClass('text-edit');
		let borderDiv = $('<div></div>');
		borderDiv.addClass('text-edit-border');
		element.append(borderDiv);
		let contentDiv = $('<div></div>');
		contentDiv.addClass('text-edit-content');
		element.append(contentDiv);
		let input = $('<input></input>');
		this._input = input;
		contentDiv.append(input);
		input.attr('type', 'text');
		var self = this;
		input.on('input', function(ev) {
			self._onInput(ev);
		});
		this._lastValue = '';
		this._lastSelectionStart = 0;
		this._lastSelectionEnd = 0;
		this._oninput = null;
	}

	get element() {
		return this._element;
	}

	get input() {
		return this._input;
	}

	set id(id) {
		this._element.attr('id', id);
		this._input.attr('name', id);
	}

	set placeholder(placeholder) {
		this._input.attr('placeholder', placeholder);
	}

	set transformer(transformer) {
		this._transformer = transformer;
	}

	set disabled(disabled) {
		if (disabled) {
			this._element.addClass('text-edit-disabled');
			this._input.css('pointer-events', 'none');
		} else {
			this._element.removeClass('text-edit-disabled');
			this._input.css('pointer-events', 'auto');
		}
		this._input.prop('disabled', disabled);
	}

	oninput(oninput) {
		this._oninput = oninput;
	}

	setInputTypeId() {
		var self = this;
		this._transformer = (preSelect, select, postSelect) => {
			return self._transformInputTypeId(preSelect, select, postSelect);
		}
	}

	setInputTypeInt() {
		this._input.attr('inputmode', 'numeric');
		var self = this;
		this._transformer = (preSelect, select, postSelect) => {
			return self._transformInputTypeInt(preSelect, select, postSelect);
		}
	}

	setInputTypeNonNegInt() {
		this._input.attr('inputmode', 'numeric');
		var self = this;
		this._transformer = (preSelect, select, postSelect) => {
			return self._transformInputTypeNonNegInt(preSelect, select,
				postSelect);
		}
	}

	setInputTypeFloat() {
		this._input.attr('inputmode', 'numeric');
		var self = this;
		this._transformer = (preSelect, select, postSelect) => {
			return self._transformInputTypeFloat(preSelect, select, postSelect);
		}
	}

	addEndIcon(icon) {
		let iconDiv = $('<div></div>');
		iconDiv.addClass('text-edit-end-icon');
		iconDiv.css('mask-image', "url('" + icon + "')");
		iconDiv.css('-webkit-mask-image', "url('" + icon + "')");
		let contentDiv = this._element.find('.text-edit-content');
		contentDiv.append(iconDiv);
		return iconDiv;
	}

	/**
	 * Reads the input value and checks whether the value is a valid ID. It
	 * checks whether it is formatted as [a-z][a-z0-9_]*. If not, it will mark
	 * the input field red and can trigger focus.
	 * 
	 * - allowEmpty (boolean): true if the field can be empty, false otherwise
	 * - focusOnError (boolean): true if the input field should get the focus
	 *     if the input is invalid
	 * 
	 * Returns:
	 * - the ID (string) if the input is valid and not empty
	 * - empty string if the input is empty and allowEmpty is true
	 * - false if the input is invalid
	 */
	readId(allowEmpty = false, focusOnError = true) {
		let id = this._input.val().trim();
		if (id.length == 0) {
			if (!allowEmpty) {
				this.showError(focusOnError);
				return false;
			}
			return '';
		}
		let regex = /^[a-z][a-z0-9_]*$/;
		if (!regex.test(id)) {
			this.showError(focusOnError);
			return false;
		}
		return id;
	}

	/**
	 * Reads the input value and checks whether the value is a valid integer.
	 * If not, it will mark the input field red and can trigger focus.
	 * 
	 * - min (int): the minimum value or null
	 * - max (int): the maximum value or null
	 * - allowEmpty (boolean): true if the field can be empty, false otherwise
	 * - focusOnError (boolean): true if the input field should get the focus
	 *     if the input is invalid
	 * 
	 * Returns:
	 * - the integer if the input is valid and not empty
	 * - null if the input is empty and allowEmpty is true
	 * - false if the input is invalid
	 */
	readInt(min = null, max = null, allowEmpty = false, focusOnError = true) {
		let valueStr = this._input.val().trim();
		if (valueStr.length == 0) {
			if (!allowEmpty) {
				this.showError(focusOnError);
				return false;
			}
			return null;
		}
		let regex = /^-?[0-9]+$/;
		if (!regex.test(valueStr)) {
			this.showError(focusOnError);
			return false;
		}
		let result = parseInt(valueStr);
		if (isNaN(result)) {
			this.showError(focusOnError);
			return false;
		}
		if ((min !== null && result < min) ||
				(max !== null && result > max)) {
			this.showError(focusOnError);
			return false;
		}
		return result;
	}

	/**
	 * Reads the input value and checks whether the value is a valid float. If
	 * not, it will mark the input field red and can trigger focus.
	 * 
	 * - min (int): the minimum value or null
	 * - max (int): the maximum value or null
	 * - allowEmpty (boolean): true if the field can be empty, false otherwise
	 * - focusOnError (boolean): true if the input field should get the focus
	 *     if the input is invalid
	 * 
	 * Returns:
	 * - the float if the input is valid and not empty
	 * - null if the input is empty and allowEmpty is true
	 * - false if the input is invalid
	 */
	readFloat(min = null, max = null, allowEmpty = false, focusOnError = true) {
		let valueStr = this._input.val().trim();
		if (valueStr.length == 0) {
			if (!allowEmpty) {
				this.showError(focusOnError);
				return false;
			}
			return null;
		}
		valueStr = valueStr.replaceAll(/[.,]/g, '.');
		let regex = /^-?[0-9]+(\.[0-9]+)?$/;
		if (!regex.test(valueStr)) {
			this.showError(focusOnError);
			return false;
		}
		let result = parseFloat(valueStr);
		if (isNaN(result)) {
			this.showError(focusOnError);
			return false;
		}
		if ((min !== null && result < min) ||
				(max !== null && result > max)) {
			this.showError(focusOnError);
			return false;
		}
		return result;
	}

	/**
	 * Marks the input field red and triggers focus.
	 * 
	 * - focus (boolean): true if the input field should get the focus
	 */
	showError(focus = true) {
		this._element.addClass('error');
		if (focus)
			this._input.trigger('focus');
	}

	_onInput(ev) {
		this._onInputTransform();
		if (this._oninput)
			this._oninput(ev);
	}

	_onInputTransform() {
		if (!this._transformer)
			return;
		let input = this._input.get(0);
		let value = input.value;
		let selStart = input.selectionStart;
		let selEnd = input.selectionEnd;
		let preSelect = value.substring(0, selStart);
		let select = value.substring(selStart, selEnd);
		let postSelect = value.substring(selEnd);
		let transformPreSelect, transformSelect, transformPostSelect;
		[transformPreSelect, transformSelect, transformPostSelect] =
			this._transformer(preSelect, select, postSelect);
		if (preSelect == transformPreSelect &&
				select == transformSelect &&
				postSelect == transformPostSelect) {
			return;
		}
		input.value = transformPreSelect + transformSelect +
			transformPostSelect;
		selStart = transformPreSelect.length;
		selEnd = selStart + transformSelect.length;
		input.setSelectionRange(selStart, selEnd);
	}

	_transformInputTypeId(preSelect, select, postSelect) {
		let list = [preSelect, select, postSelect];
		let result = [];
		for (let i = 0; i < list.length; i++) {
			let transformed = list[i].toLowerCase()
				.replaceAll(' ', '_')
				.replaceAll(/[^a-z0-9_]+/g, '');
			result.push(transformed);
		}
		return result;
	}

	_transformInputTypeInt(preSelect, select, postSelect) {
		let result = [preSelect, select, postSelect];
		// first remove all characters that are invalid in any case
		for (let i = 0; i < result.length; i++) {
			result[i] = result[i].replaceAll(/[^0-9-]+/g, '');
		}
		let atStart = true;
		for (let i = 0; i < result.length; i++) {
			let sign = '';
			if (atStart && result[i].startsWith('-'))
				sign = '-';
			result[i] = sign + result[i].replaceAll(/[^0-9]+/g, '');
			atStart = atStart && result[i].length == 0;
		}
		return result;
	}

	_transformInputTypeNonNegInt(preSelect, select, postSelect) {
		let result = [preSelect, select, postSelect];
		for (let i = 0; i < result.length; i++) {
			result[i] = result[i].replaceAll(/[^0-9]+/g, '');
		}
		return result;
	}

	_transformInputTypeFloat(preSelect, select, postSelect) {
		let result = [preSelect, select, postSelect];
		let decSep = this._getDecimalSeparator();
		// first remove all characters that are invalid in any case, and replace
		// . and , to the decimal separator of the current locale
		for (let i = 0; i < result.length; i++) {
			result[i] = result[i].replaceAll(/[^0-9.,-]+/g, '')
				.replaceAll(/[.,]/g, decSep);
		}
		// if preSelect contains a decimal separator, and select or postSelect
		// as well, then the user just entered a decimal separator while there
		// was already a separator after the cursor: remove the (just entered)
		// separators from preSelect
		if (result[0].includes(decSep) && (result[1].includes(decSep) ||
				result[2].includes(decSep))) {
			result[0] = result[0].replaceAll(new RegExp('\\' + decSep, 'g'),
				'');
		}
		// transform the current strings so that they form a valid start of a
		// float
		let transformed = [];
		// currPos is one of: start, afterSign, inInt, afterDecSep
		let currPos = 'start';
		for (let i = 0; i < result.length; i++) {
			let part = result[i];
			let transPart = '';
			for (let j = 0; j < part.length; j++) {
				let c = part[j];
				if (currPos == 'start') {
					if (c == '-') {
						transPart += c;
						currPos = 'afterSign';
					} else if (c >= '0' && c <= '9') {
						transPart += c;
						currPos = 'inInt';
					}
				} else if (currPos == 'afterSign') {
					if (c >= '0' && c <= '9') {
						transPart += c;
						currPos = 'inInt';
					}
				} else if (currPos == 'inInt') {
					if (c == decSep) {
						transPart += c;
						currPos += 'afterDecSep';
					} else if (c >= '0' && c <= '9') {
						transPart += c;
					}
				} else {
					if (c >= '0' && c <= '9') {
						transPart += c;
					}
				}
			}
			transformed.push(transPart);
		}
		return transformed;
	}

	_getDecimalSeparator() {
		let n = 1.1;
		let s = n.toLocaleString(i18next.resolvedLanguage);
		return s.substring(1, 2);
	}
}
