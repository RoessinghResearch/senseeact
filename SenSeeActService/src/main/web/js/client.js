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

	signup(email, password, project = null, tokenExpiration = 1440) {
		let data = {
			email: email,
			password: password,
			tokenExpiration: tokenExpiration,
			project: project,
			cookie: true,
			autoExtendCookie: true
		};
		return $.ajax({
			type: 'POST',
			url: servicePath + '/auth/signup',
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
	 * Calls endpoint PUT /auth/change-password
	 */
	changePassword(oldPassword, newPassword) {
		let data = {};
		if (oldPassword)
			data.oldPassword = oldPassword;
		data.newPassword = newPassword;
		data.tokenExpiration = 1440;
		data.cookie = true;
		data.autoExtendCookie = true;
		return $.ajax({
			type: 'POST',
			url: servicePath + '/auth/change-password',
			data: JSON.stringify(data),
			contentType: 'application/json'
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

	/**
	 * Calls endpoint GET /download/projects
	 */
	getDownloadProjects() {
		return $.ajax({
			type: 'GET',
			url: servicePath + '/download/projects'
		});
	}

	/**
	 * Calls endpoint GET /download/list
	 */
	getDownloadList() {
		return $.ajax({
			type: 'GET',
			url: servicePath + '/download/list'
		});
	}

	/**
	 * Calls endpoint POST /download/start
	 */
	startDownload(project) {
		let url = servicePath + '/download/start?project=' +
			encodeURIComponent(project);
		return $.ajax({
			type: 'POST',
			url: url,
			mimeType: 'text/plain'
		});
	}

	/**
	 * Calls endpoint DELETE /download/{exportId}
	 */
	deleteDownload(exportId) {
		let url = servicePath + '/download/' + exportId;
		return $.ajax({
			type: 'DELETE',
			url: url,
			mimeType: 'text/plain'
		});
	}

	hasInvalidInputField(xhr, field) {
		if (xhr.status != 400)
			return false;
		let response = xhr.responseJSON;
		if (response === null || typeof response !== 'object')
			return false;
		if (response.code != 'INVALID_INPUT')
			return false;
		if (!Array.isArray(response.fieldErrors))
			return false;
		for (let i = 0; i < response.fieldErrors.length; i++) {
			let fieldError = response.fieldErrors[i];
			if (fieldError !== null && typeof fieldError === 'object' &&
					fieldError.field == field) {
				return true;
			}
		}
		return false;
	}

	hasErrorCode(xhr, status, code) {
		if (xhr.status != status)
			return false;
		let response = xhr.responseJSON;
		if (response === null || typeof response !== 'object')
			return false;
		return response.code == code;
	}
}
