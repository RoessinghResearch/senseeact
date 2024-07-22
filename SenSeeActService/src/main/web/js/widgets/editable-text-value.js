/**
 * Properties that can be set before or after rendering:
 * 
 * - value: the current text value
 * - placeholderValue: shown in view mode instead of the current text value,
 *     which should be empty
 * - onEdit: function with this signature:
 *     jqXHR onEdit(String value)
 * - onEditComplete: function with this signature:
 *     void onEditComplete(String value)
 * 
 * Properties that can be obtained after rendering:
 * 
 * - input (jQuery element)
 * 
 * Private properties:
 * 
 * - _value (String)
 * - _placeholderValue (String)
 * - _viewDiv (jQuery element)
 * - _editDiv (jQuery element)
 * - _valueView (jQuery element)
 * - _input (jQuery element)
 */
class EditableTextValue {
	constructor(root) {
		this._root = root;
		this._value = '';
		this._placeholderValue = '';
	}

	render() {
		let root = this._root;
		root.addClass('editable-text-value');

		let viewDiv = $('<div></div>')
			.addClass('view');
		this._viewDiv = viewDiv;
		root.append(viewDiv);

		let viewRowDiv = $('<div></div>')
			.addClass('view-row');
		viewDiv.append(viewRowDiv);
		let valueView = $('<div></div>')
			.addClass('value');
		this._valueView = valueView;
		viewRowDiv.append(valueView);
		let icon = $('<button></button>')
			.addClass('icon icon-edit');
		var self = this;
		icon.on('click', function() {
			self._onEditClick();
		});
		viewRowDiv.append(icon);

		let editDiv = $('<div></div>')
			.addClass('edit');
		this._editDiv = editDiv;
		root.append(editDiv);

		let editForm = $('<form></form>')
			.addClass('edit-form');
		editForm.submit(function(e) {
			e.preventDefault();
		});
		editDiv.append(editForm);
		let input = $('<input></input>')
			.attr('name', root.attr('id'));
		this._input = input;
		editForm.append(input);
		
		icon = $('<button></button>')
			.attr('type', 'button')
			.addClass('icon icon-cancel');
		icon.on('click', function() {
			self._onCancelClick();
		});
		editForm.append(icon);
		icon = $('<button></button>')
			.attr('type', 'submit')
			.addClass('icon icon-confirm');
		icon.on('click', function() {
			self._onConfirmClick();
		});
		editForm.append(icon);

		let errorDiv = $('<div></div>')
			.addClass('error');
		errorDiv.text('Error');
		this._errorDiv = errorDiv;
		editDiv.append(errorDiv);
		
		this.value = this._value;
	}

	get input() {
		return this._input;
	}

	set value(value) {
		if (!value)
			value = '';
		value = value.trim();
		this._value = value;
		this._updateValueViews();
	}

	set placeholderValue(placeholderValue) {
		this._placeholderValue = placeholderValue;
		this._updateValueViews();
	}

	_updateValueViews() {
		if (!this._valueView) {
			// not rendered yet
			return;
		}
		let valueView = this._valueView;
		valueView.removeClass('placeholder not-filled');
		let value = this._value;
		let placeholderValue = this._placeholderValue;
		if (value) {
			valueView.text(value);
		} else if (placeholderValue) {
			valueView.text(placeholderValue);
			valueView.addClass('placeholder');
		} else {
			valueView.text(i18next.t('not_filled'));
			valueView.addClass('not-filled');
		}
		this._input.val(value);
	}

	showError(error) {
		let errorDiv = this._errorDiv;
		errorDiv.text(error);
		errorDiv.show();
		this._input.addClass('error');
	}

	hideError() {
		this._errorDiv.hide();
		this._input.removeClass('error');
	}

	_onEditClick() {
		this._viewDiv.hide();
		this._editDiv.show();
		this._input.focus();
	}

	_onCancelClick() {
		this.hideError();
		this.value = this._value;
		this._editDiv.hide();
		this._viewDiv.show();
	}

	_onConfirmClick() {
		this.hideError();
		let newValue = this._input.val().trim();
		var self = this;
		if (this.onEdit) {
			this.onEdit(newValue)
				.done(function(result) {
					self._onConfirmDone(newValue);
				})
				.fail(function(xhr, status, error) {
					self._onConfirmFail();
				});
		} else {
			this._onConfirmDone(newValue);
		}
	}

	_onConfirmDone(newValue) {
		this.value = newValue;
		this._onConfirmComplete();
		if (this.onEditComplete)
			this.onEditComplete(newValue);
	}

	_onConfirmFail() {
		this._input.addClass('error');
		this._input.focus();
	}

	_onConfirmComplete() {
		this._editDiv.hide();
		this._viewDiv.show();
	}
}
