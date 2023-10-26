class ResetPasswordCompletedPage {
	constructor() {
		this._createView();
	}
	
	_createView() {
		let background = new BackgroundImage($('#background-image'), false);
		background.render();
		$('#root').css('visibility', 'visible');
	}
}

new ResetPasswordCompletedPage();
