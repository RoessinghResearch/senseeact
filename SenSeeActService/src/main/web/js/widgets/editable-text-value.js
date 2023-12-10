/**
 * Properties that can be set before rendering:
 * 
 * - value: the current text value
 * - onEdit: function with this signature:
 *     jqXHR onEdit(String value)
 * 
 * Private properties:
 * 
 * - _viewDiv (jQuery element)
 * - _editForm (jQuery element)
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
		let valueView = $('<div></div>')
			.addClass('value');
		this._valueView = valueView;
		viewDiv.append(valueView);
		let icon = $('<button></button>')
			.addClass('icon icon-edit');
		var self = this;
		icon.on('click', function() {
			self._onEditClick();
		});
		viewDiv.append(icon);
		let editForm = $('<form></form>')
			.addClass('edit');
		editForm.submit(function(e) {
			e.preventDefault();
		});
		this._editForm = editForm;
		root.append(editForm);
		let edit = $('<input></input>')
			.attr('name', root.attr('id'));
		this._input = edit;
		editForm.append(edit);
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
		this._setValue(this.value);
	}

	_onEditClick() {
		this._viewDiv.css('visibility', 'hidden');
		this._editForm.css('visibility', 'visible');
		this._input.focus();
	}

	_onCancelClick() {
		this._setValue(this._currentValue);
		this._editForm.css('visibility', 'hidden');
		this._viewDiv.css('visibility', 'visible');
	}

	_onConfirmClick() {
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
		this._setValue(this._currentValue);
		showToast(i18next.t('unexpected_error'));
		this._onConfirmComplete();
	}

	_onConfirmComplete() {
		this._editForm.css('visibility', 'hidden');
		this._viewDiv.css('visibility', 'visible');
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
