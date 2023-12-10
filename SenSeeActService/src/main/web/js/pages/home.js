class HomePage {
	constructor() {
		this._createView();
	}
	
	_createView() {
		let background = new BackgroundImage($('#background-image'), false);
		background.render();
		$(document.body).addClass('white-background');
		$('#content').css('visibility', 'visible');
	}
}

new HomePage();
