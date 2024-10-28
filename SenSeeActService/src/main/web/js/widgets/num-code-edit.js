class NumCodeEdit {
	constructor(root) {
		this._root = root;
		root.addClass('num-code-edit');
		this._codeLength = 6;
		this._onenter = null;
	}

	get codeLength() {
		return this._codeLength;
	}

	set codeLength(value) {
		this._codeLength = value;
	}

	get code() {
		let code = '';
		this._root.find('input').each((index, elem) => {
			code += elem.value;
		});
		if (code.length == this._codeLength)
			return code;
		else
			return null;
	}

	onenter(onenter) {
		this._onenter = onenter;
	}

	render() {
		for (let i = 0; i < this._codeLength; i++) {
			this._addDigitBox(i);
		}
	}

	_addDigitBox(i) {
		let input = $('<input></input>');
		input.attr('type', 'text');
		input.attr('inputmode', 'numeric');
		this._root.append(input);
		var self = this;
		let inputElem = input.get(0);
		inputElem.addEventListener('focus', function(ev) {
			self._onFocus(inputElem, i, ev);
		});
		inputElem.addEventListener('input', function(ev) {
			self._onInput(inputElem, i, ev);
		});
		inputElem.addEventListener('keydown', function(ev) {
			self._onKeyDown(inputElem, i, ev);
		});
	}

	focus() {
		let input = this._root.find('input').get(0);
		input.focus();
	}

	_onFocus(input, i, ev) {
		input.select();
	}

	_onInput(input, i, ev) {
		let inputs = this._root.find('input');
		let val = input.value.substring(0, input.selectionStart)
			.replaceAll(/[^0-9]+/g, '');
		if (val.length == 0) {
			input.value = val;
			if (i > 0) {
				let prevInput = inputs.get(i - 1);
				let prevLen = prevInput.value.length;
				prevInput.focus();
				prevInput.setSelectionRange(prevLen, prevLen);
			}
		} else {
			let inputIndex = i;
			while (inputIndex < this._codeLength && val.length > 0) {
				let nextInput = inputs.get(inputIndex);
				nextInput.value = val.substring(0, 1);
				val = val.substring(1);
				inputIndex++;
			}
			if (inputIndex < this._codeLength) {
				let nextInput = inputs.get(inputIndex);
				nextInput.focus();
				nextInput.setSelectionRange(0, 0);
			} else {
				let nextInput = inputs.get(this._codeLength - 1);
				nextInput.focus();
				nextInput.setSelectionRange(1, 1);
				let code = this.code;
				if (this._onenter && code.length == this._codeLength) {
					this._onenter(this, code);
				}
			}
		}
	}

	_onKeyDown(input, i, ev) {
		if (ev.key == 'Backspace')
			this._onKeyBackspace(input, i);
		else if (ev.key == 'ArrowLeft')
			this._onKeyArrowLeft(input, i, ev);
		else if (ev.key == 'ArrowRight')
			this._onKeyArrowRight(input, i, ev);
	}

	_onKeyBackspace(input, i) {
		if (input.selectionStart == 0 && input.selectionEnd == 0 && i > 0) {
			let prevInput = this._root.find('input').get(i - 1);
			prevInput.value = '';
			prevInput.focus();
		}
	}

	_onKeyArrowLeft(input, i, ev) {
		if (input.selectionStart == 0 && input.selectionEnd == 0 && i > 0) {
			let prevInput = this._root.find('input').get(i - 1);
			let len = prevInput.value.length;
			prevInput.focus();
			prevInput.setSelectionRange(len, len);
			ev.preventDefault();
		}
	}

	_onKeyArrowRight(input, i, ev) {
		let len = input.value.length;
		if (input.selectionStart == len && input.selectionEnd == len &&
				i < this._codeLength - 1) {
			let nextInput = this._root.find('input').get(i + 1);
			nextInput.focus();
			nextInput.setSelectionRange(0, 0);
			ev.preventDefault();
		}
	}
}
