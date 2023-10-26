var animator;

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
	let pagePath = url.path.substring(basePath.length).replace(/\/+/, '')
	let context = {
		basePath: basePath,
		servicePath: servicePath
	};
	let html = Handlebars.templates.header(context);
	$('#header').append(html);
	template = findPageTemplate(pagePath);
	html = template(context);
	$('#root').append(html);
	html = Handlebars.templates.footer(context);
	$('#footer').append(html);
	$('body').localize();
}

function findPageTemplate(pagePath) {
	let language = i18next.resolvedLanguage;
	let template = Handlebars.templates['pages_' + language + '/' + pagePath];
	if (!template)
		template = Handlebars.templates['pages/' + pagePath];
	if (!template)
		template = findPageTemplate('home');
	return template;
}

function loadJS(name) {
	let script = document.createElement('script');
	script.src = basePath + '/js/' + name + '.js?v=' + version;
	document.head.appendChild(script);
}

/**
 * Parses a HTTP or HTTPS URl and returns an object with the following
 * properties:
 * - protocol: http or https
 * - host: host name
 * - port: port number
 * - path: path string
 * - pathSegments: array of path segments (the path split by /, leading and
 *       trailing / removed)
 * - paramsString: parameter string after ?
 * - params: array of parameter objects. Each object has properties "name" and
 *       "value".
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
			parsed.params[paramNameVal[0]] =
				decodeURIComponent(paramNameVal[1]);
		}
	}
	return parsed;
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
	toastText.click(function() {
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
