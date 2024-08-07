var animator;
var menuController;

var i18nInitialized = false;
var documentInitialized = false;
var currentToastId = null;

function onI18nInitialized() {
	i18nInitialized = true;
	if (documentInitialized)
		initPage();
}

function onDocumentInitialized() {
	documentInitialized = true;
	if (i18nInitialized)
		initPage();
}

function initPage() {
	animator = new ElementAnimator();
	let url = parseURL(window.location.href);
	let pagePath = url.path.substring(basePath.length).replace(/^\/+/, '')
	let trimmedPagePath = pagePath.replace(/\/+$/, '')
	if (pagePath != trimmedPagePath) {
		window.location.href = basePath + '/' + trimmedPagePath;
		return;
	}
	let context = {
		basePath: basePath,
		servicePath: servicePath
	};
	let html = Handlebars.templates.header(context);
	$('#header').append(html);
	html = Handlebars.templates.footer(context);
	$('#footer').append(html);
	html = Handlebars.templates.menu(context);
	$('#menu').append(html);
	let template = findPageTemplate(pagePath);
	if (!template) {
		window.location.href = basePath + '/';
		return;
	}
	html = template(context);
	$('#content').append(html);
	menuController = new MenuController();
	$('body').localize();
	checkLogin(function() {
		menuController.appendLogout();
	}, false);
}

function findPageTemplate(pagePath) {
	if (pagePath == '')
		return findPageTemplate('home');
	let language = i18next.resolvedLanguage;
	let template = Handlebars.templates['pages_' + language + '/' + pagePath];
	if (!template)
		template = Handlebars.templates['pages/' + pagePath];
	return template;
}

/**
 * Loads one or more JavaScript files. If you load multiple files, they will be
 * loaded asynchronously in sequence.
 * 
 * Params:
 * - names: string with one name or array with multiple names. Each name should
 *       be the relative path to the JavaScript file from the "js" directory,
 *       without the ".js" extension.
 */
function loadJS(names) {
	let name;
	let rest = [];
	if (Array.isArray(names)) {
		if (names.length == 0)
			return;
		name = names[0];
		rest = names.slice(1);
	} else {
		name = names;
	}
	let script = document.createElement('script');
	if (rest.length > 0) {
		script.onload = function() {
			loadJS(rest);
		};
	}
	script.src = basePath + '/js/' + name + '.js?v=' + version;
	document.head.appendChild(script);
}

function uuidv4() {
	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
		let r = Math.random() * 16 | 0;
		let v = c == 'x' ? r : (r & 0x3 | 0x8);
		return v.toString(16);
	});
}

function showToast(message) {
	hideToast();
	let toastId = uuidv4();
	currentToastId = toastId;
	let toast = $('#toast');
	let toastText = $('#toast-text')
	toastText.text(message);
	toast.show();
	setTimeout(function() {
		_onToastEnd(toastId);
	}, 4000);
	toastText.on('click', function() {
		_onToastEnd(toastId);
	})
	animator.startAnimation(toast, 'animate-toast-fade-in');
}

function _onToastEnd(toastId) {
	if (currentToastId != toastId)
		return;
	let toast = $('#toast');
	animator.startAnimation(toast, 'animate-toast-fade-out', function() {
		_onToastFadeOut(toastId);
	});
}

function _onToastFadeOut(toastId) {
	if (currentToastId == toastId)
		hideToast();
}

function hideToast() {
	currentToastId = null;
	let toast = $('#toast');
	toast.hide();
	toast.removeClass('animate-toast-fade-in');
	toast.removeClass('animate-toast-fade-out');
	toast.off('click');
	toast.off('animationend');
	let toastText = $('#toast-text')
	toastText.text('');
}

function setCookie(cname, cvalue, exdays) {
	let cookie = cname + '=' + encodeURIComponent(cvalue);
	if (exdays !== null) {
		let time = new Date();
		time.setTime(time.getTime() + exdays * 24 * 60 * 60 * 1000);
		cookie += ';expires=' + time.toUTCString();
	}
	document.cookie = cookie + ';path=' + basePath + '/;SameSite=Strict';
}

function getCookie(cname) {
	var cookies = document.cookie.split(';');
	for (let i = 0; i < cookies.length; i++) {
		let cookie = cookies[i].trim();
		let nameVal = cookie.split('=');
		if (nameVal.length < 2)
			continue;
		if (nameVal[0] == cname)
			return decodeURIComponent(nameVal[1]);
	}
	return '';
}

function clearCookie(cname) {
	let time = new Date('1970-01-01');
	let expires = 'expires=' + time.toUTCString();
	document.cookie = cname + '=;' + expires + ';path=' + basePath +
		'/;SameSite=Strict';
}

function clearAllCookies() {
	let cookieList = document.cookie.trim().split(';');
	for (let i = 0; i < cookieList.length; i++) {
		let cookieNameVal = cookieList[i].split('=');
		clearCookie(cookieNameVal[0]);
	}
}

/**
 * Parses a HTTP or HTTPS URL and returns an object with the following
 * properties:
 * - protocol: http or https
 * - host: host name
 * - port: port number
 * - path: path string
 * - pathSegments: array of path segments (the path split by /, leading and
 *       trailing / removed)
 * - paramsString: parameter string after ?
 * - params: object with parameter name and values
 * - anchor: anchor string after #
 */
function parseURL(url) {
	let re = /^(http(s?)):\/\/([^:/]+)(:([0-9]+))?(\/[^?#]*)(\?([^#]*))?(\#(.*))?$/i;
	let match = re.exec(url);
	let parsed = {
		protocol: match[1],
		host: match[3],
		port: match[5],
		path: match[6],
		pathSegments: [],
		paramsString: match[8],
		params: {},
		anchor: match[10]
	}
	let trimmedPath = parsed.path.replace(/^\//, '').replace(/\/$/, '');
	if (trimmedPath.length != 0) {
		parsed.pathSegments = trimmedPath.split('/');
	}
	if (parsed.paramsString) {
		let paramList = parsed.paramsString.split('&');
		for (let i = 0; i < paramList.length; i++) {
			let paramNameVal = paramList[i].split('=');
			parsed.params[paramNameVal[0]] = decodeURIComponent(
				paramNameVal[1]);
		}
	}
	return parsed;
}

function getUrlPath(url) {
	let re = /^(http(s?)):\/\/([^:/]+)(:([0-9]+))?(\/.*)$/i;
	let match = re.exec(url);
	return match[6];
}

function getParameterString(params) {
	let result = '';
	for (key in params) {
		if (result.length != 0)
			result += '&';
		result += key + '=' + encodeURIComponent(params[key]);
	}
	return result;
}

function uuidv4() {
	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
		let r = Math.random() * 16 | 0;
		let v = c == 'x' ? r : (r & 0x3 | 0x8);
		return v.toString(16);
	});
}

function checkLogin(onSuccess, redirectOnFail = true) {
	let path = '/me';
	let absPath = getUrlPath(window.location.href);
	if (absPath.startsWith(basePath))
		path = absPath.substring(basePath.length);
	let client = new SenSeeActClient();
	client.getUser()
		.done(function(data) {
			onSuccess(data);
		})
		.fail(function(jqXHR, textStatus, errorThrown) {
			if (redirectOnFail)
				_onCheckLoginFail(jqXHR, path);
		});
}

function _onCheckLoginFail(jqXHR, path) {
	if (jqXHR.status == 401) {
		setCookie('redirect_on_login', path, 1);
		window.location.href = basePath + '/login';
	} else {
		showToast(i18next.t('unexpected_error'));
	}
}

function redirectOnLogin() {
	let redirect = getCookie('redirect_on_login');
	if (redirect) {
		clearCookie('redirect_on_login');
		window.location.href = basePath + redirect;
	} else {
		window.location.href = basePath + '/me';
	}
}
