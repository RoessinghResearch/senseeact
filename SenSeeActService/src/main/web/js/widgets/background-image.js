class BackgroundImage {
	constructor(root, tinted) {
		this._root = root;
		this._tinted = tinted;
	}
	
	render() {
		if (this._tinted) {
			this._renderTinted();
		} else {
			this._renderWhite();
		}
	}

	_renderTinted() {
		let root = this._root;
		root.addClass('background-image');
		let left = $('<div></div>')
			.addClass('background-image-left');
		root.append(left);
		let img = $('<img></img>')
			.attr('src', basePath + '/images/bg_illustration_tinted.png');
		root.append(img);
		let right = $('<div></div>')
			.addClass('background-image-right');
		root.append(right);
	}

	_renderWhite() {
		let root = this._root;
		root.addClass('background-image');
		let left = $('<div></div>')
			.addClass('background-image-left white');
		root.append(left);
		let img = $('<img></img>')
			.attr('src', basePath + '/images/bg_illustration_white.png');
		root.append(img);
		let right = $('<div></div>')
			.addClass('background-image-right white');
		root.append(right);
	}
}
