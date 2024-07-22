/**
 * Properties that can be set before rendering:
 * 
 * - value: the current text value
 * - onEdit: function with this signature:
 *     jqXHR onEdit(String value)
 * 
 * Properties that can be obtained after rendering:
 * 
 * - input (jQuery element)
 * 
 * Private properties:
 * 
 * - _viewDiv (jQuery element)
 * - _editDiv (jQuery element)
 * - _valueView (jQuery element)
 * - _input (jQuery element)
 * - _currentValue (String)
 */
class EditableTextValue {
	constructor(root) {
		this._root = root;
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
		
		this._setValue(this.value);
	}

	get input() {
		return this._input;
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
		this._setValue(this._currentValue);
		this._editDiv.hide();
		this._viewDiv.show();
	}

	_onConfirmClick() {
		this.hideError();
		let newValue = this._input.val().trim();
		var self = this;
		this.onEdit(newValue)
			.done(function(result) {
				self._onConfirmDone(newValue);
			})
			.fail(function(xhr, status, error) {
				self._onConfirmFail();
			});
	}

	_onConfirmDone(newValue) {
		this._setValue(newValue);
		this._onConfirmComplete();
	}

	_onConfirmFail() {
		this._input.addClass('error');
		this._input.focus();
	}

	_onConfirmComplete() {
		this._editDiv.hide();
		this._viewDiv.show();
	}

	_setValue(value) {
		if (!value)
			value = '';
		value = value.trim();
		this._currentValue = value;
		let valueView = this._valueView;
		if (value) {
			valueView.text(value);
			valueView.removeClass('not-filled');
		} else {
			valueView.text(i18next.t('not_filled'));
			valueView.addClass('not-filled');
		}
		this._input.val(value);
	}
}
