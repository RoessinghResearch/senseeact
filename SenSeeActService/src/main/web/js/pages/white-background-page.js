class WhiteBackgroundPage {
	constructor() {
		this._createView();
	}
	
	_createView() {
		let background = new BackgroundImage($('#background-image'), false);
		background.render();
		$('#content').css('visibility', 'visible');
	}
}

new WhiteBackgroundPage();
