package oneprofile.backend.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns the title of a vacancy into a comparable one, dropping what is formatting and
 * keeping what is the job. It says nothing about the level or the work mode: those are
 * taken out afterwards, each by its own extractor.
 *
 * <p>It is a pure function —no network, no database and no Spring— like
 * {@link GreenhouseBoardUrl} and {@link HtmlToText}.
 */
public final class TitleCleaner {

	/**
	 * What survives the cleanup, spelled as what is kept rather than what is dropped:
	 * listing the separators that bother us always misses one, while this is a closed
	 * set. It is unicode-aware on purpose, so a title in Korean does not end up empty.
	 */
	private static final String KEPT = "\\p{IsAlphabetic}\\p{IsDigit}";

	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

	/** The only character dropped without leaving a space: "women's" -> "womens". */
	private static final Pattern APOSTROPHE = Pattern.compile("['’]");

	/** Kept only where it means something: ".net" and "node.js" survive, "engineer." does not. */
	private static final Pattern LONE_DOT = Pattern.compile("\\.(?![" + KEPT + "])");

	/** "c#" survives, "#hiring" does not. */
	private static final Pattern LONE_HASH = Pattern.compile("(?<![" + KEPT + "])#");

	/** Another plus counts as a predecessor too, or "c++" would come out as "c+". */
	private static final Pattern LONE_PLUS = Pattern.compile("(?<![" + KEPT + "+])\\+");

	/** "r&d" survives; in "sales & marketing" the ampersand is just a separator. */
	private static final Pattern LONE_AMPERSAND = Pattern
			.compile("(?<![" + KEPT + "])&|&(?![" + KEPT + "])");

	private static final Pattern DROPPED = Pattern.compile("[^" + KEPT + " .+#&]");

	private static final Pattern SPACES = Pattern.compile("\\s+");

	/** Only a run of plain letters can be an acronym: it rules out ".net", "c#" and "91359". */
	private static final Pattern LETTERS_ONLY = Pattern.compile("\\p{IsAlphabetic}+");

	private static final int ACRONYM_MIN = 2;

	private static final int ACRONYM_MAX = 6;

	/**
	 * A word this short may be missing from the acronym without breaking it: "Senior
	 * Software Development Engineer in Test (SDET)" skips the "in".
	 */
	private static final int SKIPPABLE_LENGTH = 2;

	/**
	 * How the gender mark of a job ad looks once the cleanup turned its slashes into
	 * spaces: "(H/F)" is "h f", "(m/w/d)" is "m w d". They are matched as a whole run and
	 * never letter by letter, because a lone "f" can be part of anything.
	 */
	private static final Set<String> GENDER_MARKS = Set.of(
			"h f", "f h", "m f d", "m w d", "w m d", "m f x", "m w x", "f m x");

	private static final int GENDER_MARK_MAX_WORDS = 3;

	private TitleCleaner() {
	}

	/** The comparable title, or an empty string when nothing readable is left. */
	public static String clean(String title) {
		String text = whitelist(title);
		if (text.isEmpty()) {
			return text;
		}
		List<String> tokens = new ArrayList<>(Arrays.asList(SPACES.split(text)));
		dropGenderMark(tokens);
		dropRedundantAcronym(tokens);
		return String.join(" ", tokens);
	}

	/**
	 * Drops what is punctuation and keeps what is a language: everything that is not a
	 * letter, a digit or a space goes, except four characters kept where they mean
	 * something.
	 */
	private static String whitelist(String title) {
		if (title == null) {
			return "";
		}
		String text = Normalizer.normalize(title.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		text = DIACRITICS.matcher(text).replaceAll("");
		text = APOSTROPHE.matcher(text).replaceAll("");
		text = LONE_DOT.matcher(text).replaceAll(" ");
		text = LONE_HASH.matcher(text).replaceAll(" ");
		text = LONE_PLUS.matcher(text).replaceAll(" ");
		text = LONE_AMPERSAND.matcher(text).replaceAll(" ");
		text = DROPPED.matcher(text).replaceAll(" ");
		text = SPACES.matcher(text).replaceAll(" ").trim();
		// Back together: NFD also splits a Hangul syllable into its letters, and leaving
		// it that way would give two spellings of the same Korean title.
		return Normalizer.normalize(text, Normalizer.Form.NFC);
	}

	/**
	 * Takes out the gender mark wherever it sits. It barely collapses the title count,
	 * but it is what leaves single letters —"f" in 1.607 titles, "h" in 970— loose inside
	 * the title, and those are noise for anything that reads the title by tokens.
	 */
	private static void dropGenderMark(List<String> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			for (int words = GENDER_MARK_MAX_WORDS; words >= 2; words--) {
				if (i + words <= tokens.size()
						&& GENDER_MARKS.contains(String.join(" ", tokens.subList(i, i + words)))) {
					tokens.subList(i, i + words).clear();
					i--;
					break;
				}
			}
		}
	}

	/**
	 * Takes out the acronym that closes the title when it only repeats what the title
	 * already says: "Registered Behavior Technician (RBT)" and "Registered Behavior
	 * Technician" are the same job, and 1.393 vacancies were spelled both ways.
	 *
	 * <p>Dropping whatever closes the title in brackets would be the obvious rule and it
	 * is the wrong one: ".NET", "(US)", "(91359)" and the gender mark live in that same
	 * place and none of them is redundant. Asking the letters to be the initials of the
	 * words before it is what tells them apart, and it needs nothing but this title.
	 */
	private static void dropRedundantAcronym(List<String> tokens) {
		int last = tokens.size() - 1;
		if (last < 1 || !isRedundantAcronym(tokens, last)) {
			return;
		}
		tokens.remove(last);
	}

	private static boolean isRedundantAcronym(List<String> tokens, int last) {
		String acronym = tokens.get(last);
		if (acronym.length() < ACRONYM_MIN || acronym.length() > ACRONYM_MAX
				|| !LETTERS_ONLY.matcher(acronym).matches()) {
			return false;
		}

		int letter = acronym.length() - 1;
		for (int i = last - 1; i >= 0 && letter >= 0; i--) {
			String word = tokens.get(i);
			if (word.charAt(0) == acronym.charAt(letter)) {
				letter--;
			}
			else if (word.length() > SKIPPABLE_LENGTH) {
				// A word long enough to deserve a letter, and it has none: whatever this
				// acronym stands for, it is not the words of this title.
				return false;
			}
		}
		return letter < 0;
	}
}
