/**
 * Properties that can be set before rendering:
 *
 * - placeholder: placeholder text
 */
class PasswordEdit {
	constructor(root) {
		this._root = root;
	}

	get textInput() {
		return this._root.find('input');
	}

	render() {
		let root = this._root;
		root.addClass('password-edit');
		let input = $('<input></input>')
			.attr('type', 'password')
			.attr('name', root.attr('id'));
		if (this.placeholder) {
			input.attr('placeholder', this.placeholder);
		}
		root.append(input);
		let imgContainer = $('<div></div>')
			.addClass('password-edit-image-container');
		root.append(imgContainer);
		let img = $('<img></img>')
			.addClass('password-edit-eye')
			.attr('src', basePath + '/images/eye.png');
		var self = this;
		img.click(function() {
			self._onEyeClick();
		});
		imgContainer.append(img);
		img = $('<img></img>')
			.addClass('password-edit-eye-slash')
			.attr('src', basePath + '/images/eye_slash.png');
		img.css('display', 'none');
		img.click(function() {
			self._onEyeSlashClick();
		});
		imgContainer.append(img);
	}
	
	_onEyeClick() {
		let input = this._root.find('input');
		let eye = this._root.find('img.password-edit-eye');
		let eyeSlash = this._root.find('img.password-edit-eye-slash');
		input.attr('type', 'text');
		eye.hide();
		eyeSlash.show();
	}
	
	_onEyeSlashClick() {
		let input = this._root.find('input');
		let eye = this._root.find('img.password-edit-eye');
		let eyeSlash = this._root.find('img.password-edit-eye-slash');
		input.attr('type', 'password');
		eye.show();
		eyeSlash.hide();
	}
}
