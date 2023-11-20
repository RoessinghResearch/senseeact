/**
 * Properties that can be set before rendering:
 * 
 * - icon: icon URL or null
 * - placeholder: placeholder text or null
 */
class IconTextEdit {
	constructor(root) {
		this._root = root;
	}

	get textInput() {
		return this._root.find('input');
	}

	render() {
		let root = this._root;
		root.addClass('icon-text-edit');
		let input = $('<input></input>')
			.attr('name', root.attr('id'));
		if (this.placeholder) {
			input.attr('placeholder', this.placeholder);
		}
		root.append(input);
		if (this.icon) {
			let imgContainer = $('<div></div>')
				.addClass('icon-text-edit-icon-container');
			root.append(imgContainer);
			let img = $('<div></div>')
				.addClass('icon')
				.css('mask-image', 'url(' + this.icon + ')');
			imgContainer.append(img);
		} else {
			root.addClass('no-icon');
		}
	}
}
