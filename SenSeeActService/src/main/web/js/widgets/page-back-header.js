/**
 * Properties that can be set before rendering:
 * 
 * - title (string)
 * - backUrl (string)
 * - backAction (function)
 *
 * Only one of backUrl or backAction should be set. You can also set both to
 * null (default). In that case the back button is not shown.
 */
class PageBackHeader {
	constructor(root) {
		this._root = root;
		this.title = '';
		this.backUrl = null;
		this.backAction = null;
	}

	render() {
		let root = this._root;
		root.addClass('page-back-header');
		if (this.backUrl || this.backAction) {
			let backBgDiv = $('<div></div>');
			backBgDiv.addClass('page-back-header-back-bg');
			root.append(backBgDiv);
			let backIconDiv = $('<img></img>');
			backIconDiv.addClass('page-back-header-back-icon');
			backIconDiv.attr('src', basePath + '/images/icon_back.svg');
			backBgDiv.append(backIconDiv);
			var self = this;
			animator.addAnimatedClickHandler(backBgDiv, backBgDiv,
				'animate-blue-button-click',
				null,
				function() {
					self._onBackClick();
				}
			);
		}
		let titleDiv = $('<h1></h1>');
		titleDiv.addClass('center');
		titleDiv.text(this.title);
		root.append(titleDiv);

	}

	_onBackClick() {
		if (this.backUrl) {
			window.location.href = this.backUrl;
		} else if (this.backAction) {
			this.backAction();
		}
	}
}
