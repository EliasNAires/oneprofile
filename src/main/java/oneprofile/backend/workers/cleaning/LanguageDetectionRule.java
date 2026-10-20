package oneprofile.backend.workers.cleaning;

import java.lang.Character.UnicodeScript;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Detects the language a description is written in, for a vacancy whose board declared none, so that
 * the body pass skips one written in a language it does not read rather than reading it as English.
 * <p>
 * Two signals, in order. A language written in a script of its own is told by that script: where a
 * fifth of the letters are Hangul it is Korean, kana Japanese, Han without kana Chinese, and so on.
 * A fifth rather than a half, because a description in Japanese still names its technologies in
 * Latin letters, and a word of those takes several letters where a word of Japanese takes one or
 * two. A language written in Latin letters is told by its function words — the articles,
 * conjunctions and prepositions every sentence of it is made of, and no technology's name — and the
 * language with the most of them wins. Where the text is too short to say, or two languages tie,
 * nothing is detected, and the vacancy is read as before.
 */
@Component
public class LanguageDetectionRule {

	/** The share of a text's letters a script needs to tell the language it is written in. */
	private static final double SCRIPT_SHARE = 0.2;

	/** Fewer letters than this say nothing about a language. */
	private static final int MIN_LETTERS = 20;

	/** Fewer function words than this are too few to tell one Latin-script language from another. */
	private static final int MIN_FUNCTION_WORDS = 3;

	private static final Pattern NON_LETTERS = Pattern.compile("[^\\p{L}]+");

	/** The scripts that tell their language, other than the scripts Japanese mixes. */
	private static final Map<UnicodeScript, String> SCRIPT_LANGUAGES = Map.of(UnicodeScript.HANGUL, "ko",
			UnicodeScript.HEBREW, "he", UnicodeScript.GREEK, "el", UnicodeScript.ARABIC, "ar", UnicodeScript.THAI,
			"th", UnicodeScript.DEVANAGARI, "hi", UnicodeScript.CYRILLIC, "ru");

	/** The letters Ukrainian writes in Cyrillic and Russian does not. */
	private static final Pattern UKRAINIAN_LETTERS = Pattern.compile("[іїєґІЇЄҐ]");

	/**
	 * The commonest function words of each language written in Latin letters that boards are written
	 * in, as ISO 639-1 codes. A word several languages share counts for each of them, and is outweighed
	 * by the words they do not.
	 */
	private static final Map<String, Set<String>> FUNCTION_WORDS = functionWords();

	private static Map<String, Set<String>> functionWords() {
		Map<String, Set<String>> words = new LinkedHashMap<>();
		words.put("en", Set.of("the", "and", "of", "to", "with", "for", "you", "we", "our", "your", "is", "are",
				"will", "this", "that", "be", "have", "from", "an", "at", "by", "or", "in", "on", "as"));
		words.put("es", Set.of("el", "la", "los", "las", "y", "que", "de", "en", "del", "por", "para", "con", "una",
				"un", "es", "su", "sus", "se", "nuestro", "nuestra", "nuestros", "como", "al", "lo", "más"));
		words.put("fr", Set.of("le", "la", "les", "et", "des", "du", "une", "un", "est", "pour", "vous", "nous",
				"avec", "dans", "sur", "au", "aux", "qui", "que", "ou", "en", "de", "votre", "notre", "nos"));
		words.put("pt", Set.of("o", "os", "as", "e", "que", "do", "da", "dos", "das", "em", "para", "com", "uma",
				"um", "é", "você", "no", "na", "não", "nosso", "nossa", "ao", "pelo", "pela", "de"));
		words.put("de", Set.of("der", "die", "das", "und", "mit", "für", "wir", "du", "sie", "ist", "ein", "eine",
				"zu", "von", "bei", "auf", "den", "dem", "des", "nicht", "oder", "dich", "dein", "deine", "unser"));
		words.put("it", Set.of("il", "lo", "la", "gli", "le", "e", "di", "del", "della", "che", "per", "con", "un",
				"una", "sono", "è", "nel", "nella", "delle", "dei", "degli", "alla", "nostro", "nostra", "siamo"));
		words.put("nl", Set.of("de", "het", "een", "en", "van", "op", "met", "voor", "je", "we", "zijn", "is",
				"ons", "onze", "bij", "als", "naar", "te", "wij", "jij", "jouw", "niet", "ook", "dat", "die"));
		words.put("pl", Set.of("i", "w", "z", "na", "do", "się", "jest", "oraz", "dla", "że", "nie", "od", "jak",
				"lub", "przez", "są", "być", "naszego", "nasz", "nasze", "pracy", "który", "która", "które", "we"));
		words.put("sv", Set.of("och", "att", "det", "en", "ett", "för", "med", "som", "på", "är", "av", "vi", "du",
				"till", "har", "om", "inte", "oss", "vår", "våra", "din", "dig", "kommer", "också", "eller"));
		words.put("da", Set.of("og", "at", "det", "en", "et", "for", "med", "som", "på", "er", "af", "vi", "du",
				"til", "har", "om", "ikke", "os", "vores", "din", "dig", "også", "eller", "kan", "skal"));
		words.put("no", Set.of("og", "at", "det", "en", "et", "for", "med", "som", "på", "er", "av", "vi", "du",
				"til", "har", "om", "ikke", "oss", "vår", "våre", "din", "deg", "også", "eller", "skal"));
		words.put("fi", Set.of("ja", "on", "että", "sekä", "tai", "kanssa", "olet", "meidän", "sinun", "ovat", "ei",
				"myös", "joka", "jossa", "kuin", "olla", "meillä", "sinulla", "voit", "tämä", "mutta", "kun", "jos",
				"niin", "se"));
		return words;
	}

	/**
	 * Detects the language of a text.
	 * @param text the text, such as a cleaned description
	 * @return its language as an ISO 639-1 code, or empty if the text does not say clearly enough
	 */
	public Optional<String> detect(String text) {
		Map<UnicodeScript, Integer> letters = new EnumMap<>(UnicodeScript.class);
		int total = 0;
		for (int i = 0; i < text.length();) {
			int codePoint = text.codePointAt(i);
			i += Character.charCount(codePoint);
			if (Character.isLetter(codePoint)) {
				letters.merge(UnicodeScript.of(codePoint), 1, Integer::sum);
				total++;
			}
		}
		if (total < MIN_LETTERS) {
			return Optional.empty();
		}
		Optional<String> byScript = byScript(text, letters, total);
		return byScript.isPresent() ? byScript : byFunctionWords(text);
	}

	private static Optional<String> byScript(String text, Map<UnicodeScript, Integer> letters, int total) {
		int kana = letters.getOrDefault(UnicodeScript.HIRAGANA, 0) + letters.getOrDefault(UnicodeScript.KATAKANA, 0);
		int han = letters.getOrDefault(UnicodeScript.HAN, 0);
		if (kana >= SCRIPT_SHARE * total || (kana > 0 && kana + han >= SCRIPT_SHARE * total)) {
			return Optional.of("ja");
		}
		if (han >= SCRIPT_SHARE * total) {
			return Optional.of("zh");
		}
		UnicodeScript dominant = null;
		for (UnicodeScript script : SCRIPT_LANGUAGES.keySet()) {
			int count = letters.getOrDefault(script, 0);
			if (count >= SCRIPT_SHARE * total && (dominant == null || count > letters.get(dominant))) {
				dominant = script;
			}
		}
		if (dominant == null) {
			return Optional.empty();
		}
		if (dominant == UnicodeScript.CYRILLIC && UKRAINIAN_LETTERS.matcher(text).find()) {
			return Optional.of("uk");
		}
		return Optional.of(SCRIPT_LANGUAGES.get(dominant));
	}

	private static Optional<String> byFunctionWords(String text) {
		Map<String, Integer> hits = new LinkedHashMap<>();
		for (String word : NON_LETTERS.split(text.toLowerCase(Locale.ROOT))) {
			FUNCTION_WORDS.forEach((language, words) -> {
				if (words.contains(word)) {
					hits.merge(language, 1, Integer::sum);
				}
			});
		}
		String best = null;
		int bestHits = 0;
		int secondHits = 0;
		for (Map.Entry<String, Integer> language : hits.entrySet()) {
			if (language.getValue() > bestHits) {
				secondHits = bestHits;
				best = language.getKey();
				bestHits = language.getValue();
			}
			else if (language.getValue() > secondHits) {
				secondHits = language.getValue();
			}
		}
		return (bestHits >= MIN_FUNCTION_WORDS && bestHits > secondHits) ? Optional.of(best) : Optional.empty();
	}

}
