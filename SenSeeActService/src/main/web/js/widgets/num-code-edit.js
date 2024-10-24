class NumCodeEdit {
	constructor(root) {
		this._root = root;
		root.addClass('num-code-edit');
		this._codeLength = 6;
	}

	get codeLength() {
		return this._codeLength;
	}

	set codeLength(value) {
		this._codeLength = value;
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
		inputElem.addEventListener('keyup', function(ev) {
			self._onKeyUp(inputElem, i, ev);
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
			}
		}
	}

	_onKeyDown(input, i, ev) {
		this._keyDownValue = input.value;
	}

	_onKeyUp(input, i, ev) {
		if (ev.key == 'Backspace' && input.value == this._keyDownValue &&
				input.selectionStart == 0 &&
				input.selectionEnd == 0 && i > 0) {
			let prevInput = this._root.find('input').get(i - 1);
			prevInput.value = '';
			prevInput.focus();
		}
	}
}
