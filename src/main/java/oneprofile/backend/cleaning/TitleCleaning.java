package oneprofile.backend.cleaning;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oneprofile.backend.normalizedvacancy.CleanedTitle;
import oneprofile.backend.normalizedvacancy.SeniorityLevel;

/**
 * Takes out of a title what is noise in any title, so that two companies saying the same thing say
 * it the same way. What it collapses is what does not have to be read twice when the classifier is
 * built by reading the most common titles.
 * <p>
 * Three rules, applied in this order. Gender markers go first, while the brackets and slashes that
 * make them recognisable are still there. The character whitelist goes second, spelled as what
 * survives rather than as what is dropped, because a list of separators always misses one. The
 * seniority words go last, and only the ones that name a level wherever they appear: the words that
 * need a reading of the job stay in the title for the normalization step.
 * <p>
 * Each rule is also readable on its own, so that what a rule collapses can be measured against a
 * corpus without the other two hiding it.
 */
public class TitleCleaning {

	/**
	 * The letters a gender marker is written with: the German, French, Dutch, Polish and Italian
	 * forms. Two or more of them, slashed together, bracketed or not.
	 */
	private static final String GENDERS = "m|w|d|f|h|x|v|i|k|n|gn";

	private static final String SLASHED = "(?:%s)(?:\\s*/\\s*(?:%s))+".formatted(GENDERS, GENDERS);

	private static final Pattern GENDER_MARKER = Pattern.compile(
			"[(\\[]\\s*(?:%s)\\s*[)\\]]|(?<![\\p{L}\\p{N}])(?:%s)(?![\\p{L}\\p{N}])".formatted(SLASHED, SLASHED),
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

	/**
	 * What tells a gender marker from the other slashed letters a title carries — {@code C/C++},
	 * {@code D/N} for a day or night shift, {@code I/O}. Every form of the marker names at least one
	 * of these.
	 */
	private static final Pattern NAMES_A_GENDER = Pattern.compile("(?<![\\p{L}\\p{N}])[mwfh](?![\\p{L}\\p{N}])",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

	/**
	 * The words that name a level wherever they appear, in the order they are read: the two-word
	 * form first, so that the {@code senior} inside {@code semi senior} is not read as a senior. An
	 * abbreviation is taken with the full stop that ends it, which is part of the word rather than
	 * something the title would be left with.
	 */
	private static final List<SeniorityWord> SENIORITY_WORDS = List.of(
			new SeniorityWord(word("semi[\\s-]?senior|ssr"), SeniorityLevel.MID),
			new SeniorityWord(word("senior|sr"), SeniorityLevel.SENIOR),
			new SeniorityWord(word("junior|jr"), SeniorityLevel.JUNIOR),
			new SeniorityWord(word("principal"), SeniorityLevel.PRINCIPAL));

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private static Pattern word(String alternatives) {
		return Pattern.compile("(?<![\\p{L}\\p{N}])(?:%s)(?![\\p{L}\\p{N}])\\.?".formatted(alternatives),
				Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
	}

	/**
	 * Cleans one title: the three rules, in their order.
	 * @param title the title as the company wrote it
	 * @return the cleaned title and every seniority level it named
	 */
	public CleanedTitle clean(String title) {
		CleanedTitle cleaned = withoutSeniorityWords(whitelisted(withoutGenderMarkers(title)));
		// A removed word can leave behind the character that only carried a meaning beside it: the
		// plus of "Senior+", the full stop of "Sr.Principal". What is left of them is decoration
		// again, so what survives is asked a second time.
		return new CleanedTitle(whitelisted(cleaned.title()), cleaned.titleSeniorities());
	}

	/**
	 * A title with nothing taken out and only its spacing made even: the tidying every rule does on
	 * top of what it removes. What a rule collapses is measured against this rather than against the
	 * raw titles, so that bringing together two titles that differed only in their spacing is not
	 * credited to whichever rule happened to run.
	 * @param title the title to read
	 * @return it, as words separated by single spaces
	 */
	public String evenlySpaced(String title) {
		return collapsed(title);
	}

	/**
	 * Takes out the gender markers a title carries, each one whole. Never letter by letter: a lone
	 * {@code f} can be part of anything, and a title is about to be read token by token.
	 * @param title the title to read
	 * @return the title without them
	 */
	public String withoutGenderMarkers(String title) {
		Matcher markers = GENDER_MARKER.matcher(title);
		StringBuilder kept = new StringBuilder(title.length());
		while (markers.find()) {
			markers.appendReplacement(kept,
					NAMES_A_GENDER.matcher(markers.group()).find() ? " " : Matcher.quoteReplacement(markers.group()));
		}
		markers.appendTail(kept);
		return collapsed(kept);
	}

	/**
	 * Keeps of a title only the letters and digits of any script, and the four characters that carry
	 * a meaning of their own where they carry it: the {@code .} of {@code .NET}, the {@code #} of
	 * {@code C#}, the {@code +} of {@code C++} and the {@code &} of {@code R&D}. Everything else
	 * becomes a space, so that the words on either side of it stay apart.
	 * @param title the title to read
	 * @return what survives, as words separated by single spaces
	 */
	public String whitelisted(String title) {
		int[] characters = title.codePoints().toArray();
		StringBuilder kept = new StringBuilder(characters.length);
		for (int at = 0; at < characters.length; at++) {
			if (Character.isLetterOrDigit(characters[at]) || carriesAMeaning(characters, at)) {
				kept.appendCodePoint(characters[at]);
			}
			else {
				kept.append(' ');
			}
		}
		return collapsed(kept);
	}

	/**
	 * Whether one of the four characters is here as part of a word rather than as decoration:
	 * {@code .NET} and {@code Node.js} against a full stop, {@code C#} and {@code C++} against a
	 * heading, {@code R&D} against the ampersand joining two departments.
	 */
	private static boolean carriesAMeaning(int[] characters, int at) {
		return switch (characters[at]) {
			case '.' -> isLetterOrDigit(characters, at + 1);
			case '#' -> isLetterOrDigit(characters, at - 1);
			case '+' -> isLetterOrDigit(characters, at - 1) || at > 0 && characters[at - 1] == '+';
			case '&' -> isLetterOrDigit(characters, at - 1) && isLetterOrDigit(characters, at + 1);
			default -> false;
		};
	}

	private static boolean isLetterOrDigit(int[] characters, int at) {
		return at >= 0 && at < characters.length && Character.isLetterOrDigit(characters[at]);
	}

	/**
	 * Takes out the seniority words that mean a level wherever they appear, and names the levels they
	 * meant. The words that need a reading of the job — {@code staff}, {@code mid}, {@code entry} and
	 * the numbered levels — are left in the title, because only the engineering subset makes them
	 * readable.
	 * @param title the title to read
	 * @return the title without them, and every level they named
	 */
	public CleanedTitle withoutSeniorityWords(String title) {
		Set<SeniorityLevel> named = new LinkedHashSet<>();
		String left = title;
		for (SeniorityWord word : SENIORITY_WORDS) {
			Matcher said = word.pattern().matcher(left);
			if (said.find()) {
				named.add(word.level());
				left = said.reset().replaceAll(" ");
			}
		}
		return new CleanedTitle(collapsed(new StringBuilder(left)), named);
	}

	/** One of the words that names a level wherever it appears, and the level it names. */
	private record SeniorityWord(Pattern pattern, SeniorityLevel level) {
	}

	private static String collapsed(CharSequence title) {
		return WHITESPACE.matcher(title).replaceAll(" ").trim();
	}

}
