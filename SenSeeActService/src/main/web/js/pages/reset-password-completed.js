class ResetPasswordCompletedPage {
	constructor() {
		this._createView();
	}
	
	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();
		$(document.body).addClass('tinted-background');
		$('#root').css('visibility', 'visible');
	}
}

new ResetPasswordCompletedPage();