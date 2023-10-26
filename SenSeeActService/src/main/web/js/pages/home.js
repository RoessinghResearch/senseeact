class HomePage {
	constructor() {
		this._createView();
	}
	
	_createView() {
		let background = new BackgroundImage($('#background-image'), false);
		background.render();
		$(document.body).addClass('white-background');
		$('#root').css('visibility', 'visible');
	}
}

new HomePage();
