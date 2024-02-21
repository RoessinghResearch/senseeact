i18next
	.use(i18nextHttpBackend)
	.use(i18nextBrowserLanguageDetector)
	.init({
		fallbackLng: 'en-GB',
		ns: ['default'],
		defaultNS: 'default',
		backend: {
			loadPath: basePath + '/i18n/{{lng}}/{{ns}}.json?v=' + version
		},
		detection: {
			order: ['querystring', 'navigator', 'cookie', 'localStorage'],
			lookupQuerystring: 'lng',
			lookupCookie: 'i18next',
			lookupLocalStorage: 'i18nextLng',
			caches: ['localStorage', 'cookie']
		}
	}, function() {
		onI18nInit();
	});

function onI18nInit() {
	jqueryI18next.init(i18next, $, {
		tName: 't', // --> appends $.t = i18next.t
		i18nName: 'i18n', // --> appends $.i18n = i18next
		handleName: 'localize', // --> appends $(selector).localize(opts);
		selectorAttr: 'data-i18n', // selector for translating elements
		targetAttr: 'i18n-target', // data-() attribute to grab target element to translate (if different than itself)
		optionsAttr: 'i18n-options', // data-() attribute that contains options, will load/set if useOptionsAttr = true
		useOptionsAttr: false, // see optionsAttr
		parseDefaultValueFromContent: true // parses default values from content ele.val or ele.text
	});
	onI18nInitialized();
}
