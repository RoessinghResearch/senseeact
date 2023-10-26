class SenSeeActClient {
	/**
	 * Calls endpoint POST /auth/login.
	 */
	login(email, password, tokenExpiration = 1440) {
		let data = {
			email: email,
			password: password,
			tokenExpiration: tokenExpiration,
			cookie: true,
			autoExtendCookie: true
		};
		return $.ajax({
			type: 'POST',
			url: servicePath + '/auth/login',
			data: JSON.stringify(data),
			contentType: 'application/json'
		});
	}

	/**
	 * Calls endpoint GET /auth/logout.
	 */
	logout() {
		return $.ajax({
			type: 'GET',
			url: servicePath + '/auth/logout',
			mimeType: 'text/plain'
		});
	}

	/**
	 * Calls endpoint GET /user/.
	 */
	getUser(user = null) {
		let url = servicePath + '/user/';
		if (user)
			url += '?user=' + encodeURIComponent(user);
		return $.ajax({
			type: 'GET',
			url: url
		});
	}

	/**
	 * Calls endpoint PUT /user/
	 */
	updateUser(user, profile) {
		let url = servicePath + '/user/';
		if (user)
			url += '?user=' + encodeURIComponent(user);
		return $.ajax({
			type: 'PUT',
			url: url,
			data: JSON.stringify(profile),
			contentType: 'application/json'
		});
	}
}
