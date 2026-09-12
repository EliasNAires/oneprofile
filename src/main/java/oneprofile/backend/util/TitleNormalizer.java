package oneprofile.backend.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import oneprofile.backend.model.Seniority;

/**
 * Turns the title of a vacancy into a comparable one, and takes the experience level
 * out of it into a field of its own.
 *
 * <p>The level is not noise: it was measured to sit inside one out of four titles, and
 * pulling it out is what makes "Senior Software Engineer" and "Sr. Software Engineer"
 * meet. What is left in the title is the function of the job, so hierarchical roles
 * —director, lead, head— stay: "Director of Engineering" is not an engineer.
 */
public final class TitleNormalizer {

	/** The title with its level taken out, and the level itself; both null when absent. */
	public record NormalizedTitle(String title, Seniority seniority) {
	}

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

	/**
	 * The words that carry a level. Hierarchical roles (director, lead, head, vp, chief)
	 * are missing on purpose: they are the function, not a modifier of it. So is
	 * "associate", which is ambiguous, and "intern", which is a kind of contract.
	 */
	private static final Map<String, Seniority> LEVELS = Map.ofEntries(
			Map.entry("entry", Seniority.ENTRY),
			Map.entry("junior", Seniority.JUNIOR),
			Map.entry("jr", Seniority.JUNIOR),
			Map.entry("semisenior", Seniority.SEMI_SENIOR),
			Map.entry("ssr", Seniority.SEMI_SENIOR),
			Map.entry("mid", Seniority.MID),
			Map.entry("senior", Seniority.SENIOR),
			Map.entry("sr", Seniority.SENIOR),
			Map.entry("staff", Seniority.STAFF),
			Map.entry("principal", Seniority.PRINCIPAL),
			Map.entry("ii", Seniority.LEVEL_2),
			Map.entry("iii", Seniority.LEVEL_3));

	/** "entry" and "mid" only count as a level next to this word. */
	private static final String LEVEL_NOUN = "level";

	/** The Spanish speaking step below senior, written in two words. */
	private static final String SEMI = "semi";

	/** What joins the two ends of a range: "junior to senior", "staff or principal". */
	private static final Set<String> CONNECTORS = Set.of("to", "or", "and");

	/** Jobs where "staff" means on the payroll, not a level above senior. */
	private static final Set<String> STAFF_JOBS = Set.of("nurse", "accountant", "attorney");

	private TitleNormalizer() {
	}

	public static NormalizedTitle normalize(String title) {
		String clean = clean(title);
		if (clean.isEmpty()) {
			return new NormalizedTitle(null, null);
		}

		List<String> tokens = Arrays.asList(SPACES.split(clean));
		boolean[] isLevel = new boolean[tokens.size()];
		Seniority found = null;

		for (int i = 0; i < tokens.size(); i++) {
			Seniority level = levelAt(tokens, i, isLevel);
			if (level != null && (found == null || level.compareTo(found) < 0)) {
				// A vacancy naming more than one level is publishing the floor it takes,
				// so the lowest wins. The numbered levels are declared after the whole
				// scale of words, which is what makes a word beat "ii" for free.
				found = level;
			}
		}

		List<String> kept = new ArrayList<>();
		for (int i = 0; i < tokens.size(); i++) {
			if (!isLevel[i]) {
				kept.add(tokens.get(i));
			}
		}
		return new NormalizedTitle(kept.isEmpty() ? null : String.join(" ", kept), found);
	}

	/**
	 * The level the token at {@code i} carries, or null when it carries none. Marks in
	 * {@code isLevel} every token that goes out of the title, which is more than one
	 * when the level is spelled with two words ("entry level", "semi senior").
	 */
	private static Seniority levelAt(List<String> tokens, int i, boolean[] isLevel) {
		if (SEMI.equals(tokens.get(i)) && isSenior(at(tokens, i + 1))) {
			isLevel[i] = true;
			isLevel[i + 1] = true;
			return Seniority.SEMI_SENIOR;
		}
		if (isLevel[i]) {
			return null;
		}

		Seniority level = LEVELS.get(tokens.get(i));
		if (level == null || !counts(tokens, i)) {
			return null;
		}

		isLevel[i] = true;
		if (LEVEL_NOUN.equals(at(tokens, i + 1)) && (level == Seniority.ENTRY || level == Seniority.MID)) {
			isLevel[i + 1] = true;
		}
		return level;
	}

	/**
	 * Whether the token really means a level here. Three of them have uses where the
	 * word is the job and not its level, and they were measured: "entry door" and "data
	 * entry", "mid market" and "mid atlantic", "chief of staff" and "staff nurse".
	 */
	private static boolean counts(List<String> tokens, int i) {
		int last = tokens.size() - 1;
		return switch (tokens.get(i)) {
			case "entry" -> LEVEL_NOUN.equals(at(tokens, i + 1));
			case "mid" -> LEVEL_NOUN.equals(at(tokens, i + 1)) || i == last || inRange(tokens, i);
			case "staff" -> !"of".equals(at(tokens, i - 1)) && !"of".equals(at(tokens, i - 2))
					&& i != last && !STAFF_JOBS.contains(at(tokens, i + 1));
			default -> true;
		};
	}

	/** Whether the token is one end of a range: "mid senior", "junior to mid". */
	private static boolean inRange(List<String> tokens, int i) {
		return isWordLevel(at(tokens, i - 1)) || isWordLevel(at(tokens, i + 1))
				|| (CONNECTORS.contains(at(tokens, i - 1)) && isWordLevel(at(tokens, i - 2)))
				|| (CONNECTORS.contains(at(tokens, i + 1)) && isWordLevel(at(tokens, i + 2)));
	}

	/** A level written as a word, which is what the other end of a range looks like. */
	private static boolean isWordLevel(String token) {
		Seniority level = LEVELS.get(token);
		return level != null && level != Seniority.MID && level.compareTo(Seniority.LEVEL_2) < 0;
	}

	private static boolean isSenior(String token) {
		return LEVELS.get(token) == Seniority.SENIOR;
	}

	/** Empty past the ends of the title, so the neighbour of a token is always a word. */
	private static String at(List<String> tokens, int i) {
		return i >= 0 && i < tokens.size() ? tokens.get(i) : "";
	}

	private static String clean(String title) {
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
}
