/**
 * Properties that can be set any time.
 * 
 * - step (int): default 0
 * - total (int): default 100
 */
class ProgressBar {
	constructor(root) {
		this._root = root;
		this._step = 0;
		this._total = 100;
	}

	get step() {
		return this._step;
	}

	set step(value) {
		this._step = value;
		this._updateFill();
	}

	get total() {
		return this._total;
	}

	set total(value) {
		this._total = value;
		this._updateFill();
	}

	render() {
		let root = this._root;
		root.addClass('progress-bar');
		let borderDiv = $('<div></div>');
		borderDiv.addClass('progress-bar-border');
		root.append(borderDiv);
		let fillDiv = $('<div></div>');
		fillDiv.addClass('progress-bar-fill');
		root.append(fillDiv);
		this._updateFill();
	}

	_updateFill() {
		let root = this._root;
		let fillDiv = root.find('.progress-bar-fill');
		let clipPct = 100;
		if (this._total > 0)
			clipPct = 100 - this._step * 100.0 / this._total;
		fillDiv.css('clip-path', 'inset(0 ' + clipPct + '% 0 0)');
	}
}
