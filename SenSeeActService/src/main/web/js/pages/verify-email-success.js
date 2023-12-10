class VerifyEmailSuccessPage {
	constructor() {
		this._createView();
	}
	
	_createView() {
		let background = new BackgroundImage($('#background-image'), true);
		background.render();
		$(document.body).addClass('tinted-background');
		$('#content').css('visibility', 'visible');
	}
}

new VerifyEmailSuccessPage();
