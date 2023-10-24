package nl.rrd.senseeact.service.i18n;

import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.i18n.I18nResourceFinder;
import nl.rrd.utils.json.JsonObjectStreamReader;
import nl.rrd.utils.json.JsonParseException;
import nl.rrd.utils.validation.MapReader;
import nl.rrd.senseeact.client.model.Gender;
import nl.rrd.senseeact.client.model.User;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class I18n {
	private static final Object LOCK = new Object();

	private static final List<String> FORMALITIES = Arrays.asList(
			"formal", "informal");

	private static Map<String,List<Term>> defaults = null;
	private static Map<String,Map<String,List<Term>>> languageMap =
			new LinkedHashMap<>();

	private User user;
	private Map<String,List<Term>> strings;

	private I18n(User user, Map<String,List<Term>> strings) {
		this.user = user;
		this.strings = strings;
	}

	public static I18n init(User user) {
		synchronized (LOCK) {
			String defaultPath = "strings/strings.json";
			try {
				if (defaults == null)
					defaults = loadFile("strings/strings.json");
			} catch (ParseException ex) {
				throw new RuntimeException(
						"Error while parsing i18n strings: " + defaultPath + ": " +
								ex.getMessage(), ex);
			} catch (IOException ex) {
				throw new RuntimeException(
						"Error while reading i18n strings: " + defaultPath + ": " +
								ex.getMessage(), ex);
			}
			Locale locale = user.toLocale();
			String language = locale.getLanguage();
			if (languageMap.containsKey(language))
				return new I18n(user, languageMap.get(language));
			I18nResourceFinder finder = new I18nResourceFinder("strings/strings");
			finder.setExtension("json");
			finder.setUserLocale(locale);
			if (!finder.find())
				return new I18n(user, new HashMap<>());
			locale = finder.getLocale();
			language = locale.getLanguage();
			if (languageMap.containsKey(language))
				return new I18n(user, languageMap.get(language));
			String path = finder.getName();
			Map<String, List<Term>> strings;
			try {
				strings = loadFile(path);
			} catch (ParseException ex) {
				throw new RuntimeException(
						"Error while parsing i18n strings: " + path + ": " +
								ex.getMessage(), ex);
			} catch (IOException ex) {
				throw new RuntimeException(
						"Error while reading i18n strings: " + path + ": " +
								ex.getMessage(), ex);
			}
			languageMap.put(language, strings);
			return new I18n(user, strings);
		}
	}

	public String t(String id) {
		return translate(id, id);
	}

	public String translate(String id, String defaultResult) {
		List<Term> terms = strings.get(id);
		if (terms == null)
			terms = defaults.get(id);
		if (terms == null)
			return defaultResult;
		return findPreferredTerm(terms);
	}

	private String findPreferredTerm(List<Term> terms) {
		if (terms.size() == 1)
			return terms.get(0).text;
		List<Term> beforeFilter = terms;
		List<Term> filtered = filterFormality(beforeFilter,
				user == null ? null : user.getLanguageFormality());
		if (filtered.size() == 1)
			return filtered.get(0).text;
		if (!filtered.isEmpty())
			beforeFilter = filtered;
		filtered = filterGender(beforeFilter,
				user == null ? null : user.getGender());
		if (filtered.isEmpty())
			return beforeFilter.get(0).text;
		else
			return filtered.get(0).text;
	}

	private List<Term> filterFormality(List<Term> terms, String formality) {
		List<Term> result = new ArrayList<>();
		if (formality == null) {
			formality = "formal";
		} else {
			formality = formality.toLowerCase();
			if (!FORMALITIES.contains(formality))
				formality = "formal";
		}
		for (Term term : terms) {
			if (formality.equals("formal") &&
					term.context.contains("informal")) {
				continue;
			}
			if (formality.equals("informal") &&
					term.context.contains("formal")) {
				continue;
			}
			result.add(term);
		}
		return result;
	}

	private List<Term> filterGender(List<Term> terms, Gender gender) {
		List<Term> result = new ArrayList<>();
		if (gender == null)
			gender = Gender.MALE;
		for (Term term : terms) {
			if (gender == Gender.MALE &&
					term.context.contains("female_addressee")) {
				continue;
			}
			if (gender == Gender.FEMALE &&
					term.context.contains("male_addressee")) {
				continue;
			}
			result.add(term);
		}
		return result;
	}

	private static Map<String,List<Term>> loadFile(String path)
			throws ParseException, IOException {
		Object json;
		try (JsonObjectStreamReader reader = new JsonObjectStreamReader(
				new InputStreamReader(
				I18n.class.getClassLoader().getResourceAsStream(path),
				StandardCharsets.UTF_8))) {
			json = reader.readValue();
		} catch (JsonParseException ex) {
			throw new ParseException("Failed to parse JSON content: " +
					ex.getMessage(), ex);
		}
		if (json instanceof List) {
			return parseJsonArray((List<?>)json);
		} else {
			throw new ParseException(
					"Invalid content: Expected list, found " +
					(json == null ? "null" : json.getClass().getSimpleName()));
		}
	}

	private static Map<String,List<Term>> parseJsonArray(List<?> list)
			throws ParseException {
		Map<String,List<Term>> result = new HashMap<>();
		for (Object item : list) {
			if (!(item instanceof Map))
				throw new ParseException("List item is not a map");
			Map<?,?> map = (Map<?,?>)item;
			MapReader reader = new MapReader(map);
			String definition = reader.readString("definition");
			if (definition.isEmpty())
				continue;
			String key = reader.readString("term");
			String contextStr = reader.readString("context", null);
			Set<String> context = parseContext(contextStr);
			List<Term> termList = result.get(key);
			if (termList == null) {
				termList = new ArrayList<>();
				result.put(key, termList);
			}
			termList.add(new Term(context, definition));
		}
		return result;
	}

	private static Set<String> parseContext(String s) {
		Set<String> result = new HashSet<>();
		if (s == null)
			return result;
		s = s.trim();
		if (s.isEmpty())
			return result;
		String[] list = s.split("\\s+");
		Collections.addAll(result, list);
		return result;
	}

	private static final class Term {
		public Set<String> context;
		public String text;

		public Term(Set<String> context, String text) {
			this.context = context;
			this.text = text;
		}
	}
}
