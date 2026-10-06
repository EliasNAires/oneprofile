package oneprofile.backend.workers.cleaning;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import org.springframework.stereotype.Component;

/**
 * Cuts a cleaned description into segments: headings, list items and sentences, each knowing the
 * heading it sits under.
 */
@Component
public class DescriptionSplittingRule {

	/**
	 * A bullet or an item number typed at the start of a line. A hyphen, an asterisk or a number
	 * needs a space after it, so that neither {@code -5°C}, {@code *required} nor {@code 2.5 years}
	 * is taken for one.
	 */
	private static final Pattern LEADING_BULLET = Pattern.compile("[•·]\\s*|[-*]\\s+|\\(?\\d{1,2}[.)]\\s+");

	/**
	 * A bullet typed inside a line. A middle dot needs spaces around it, so that the Catalan
	 * {@code l·l} stays one word; a hyphen or an asterisk inside a line is never one.
	 */
	private static final Pattern INLINE_BULLET = Pattern.compile("\\s*•\\s*|\\s+·\\s+");

	/** Longer than this, a line the sweep did not mark is prose rather than a heading. */
	private static final int HEADING_LENGTH = 60;

	/**
	 * The nouns a section of a job description is headed with, in English and Spanish. A short line
	 * that starts or ends with one is a heading, which reaches {@code Basic Qualifications} and
	 * {@code Requisitos excluyentes} alike.
	 */
	private static final Set<String> HEADING_NOUNS = Set.of("requirements", "qualifications", "responsibilities",
			"duties", "benefits", "perks", "requisitos", "requerimientos", "responsabilidades", "funciones", "tareas",
			"beneficios", "deseables");

	/** The phrases a section of a job description is headed with, in English and Spanish. */
	private static final Set<String> HEADING_PHRASES = Set.of("nice to have", "nice-to-have", "bonus points",
			"what we offer", "what you'll do", "what you will do", "what you'll bring", "what you bring",
			"what we're looking for", "what we are looking for", "who you are", "about you", "about us",
			"about the role", "about the job", "about the team", "the role", "your role", "role overview",
			"position overview", "job description", "job summary", "overview", "qué harás", "que harás",
			"qué buscamos", "que buscamos", "lo que buscamos", "buscamos", "qué ofrecemos", "que ofrecemos",
			"ofrecemos", "sobre nosotros", "acerca de nosotros", "quiénes somos", "sobre el puesto", "sobre el rol",
			"tu rol", "el rol", "perfil", "perfil buscado", "deseable", "se valorará", "valoramos", "será un plus");

	/**
	 * Where a list flattened without punctuation starts a new item: a word that opens a requirement
	 * or a heading, capitalized, or a number of years.
	 */
	private static final Pattern ITEM_START = Pattern.compile("(Experience|Strong|Excellent"
			+ "|Proven|Ability|Knowledge|Familiarity|Proficiency|Proficient|Understanding|Hands-on|Bachelor|Master|Degree"
			+ "|BS|BA|Minimum|Must|Solid|Demonstrated|Working|Good|Deep|Advanced|Fluent|Fluency|Comfortable|Exposure"
			+ "|Background|At least|Nice|Bonus|Preferred|Desired|Requirements|Qualifications|Responsibilities|You|What"
			+ "|\\d+\\+? years)\\b");

	private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+(?=[\\p{Lu}\\p{N}¿¡\"“‘'(])");

	private static final Pattern LETTERS_WITH_FULL_STOPS = Pattern.compile("(\\p{L}\\.)+");

	/** The abbreviations longer than a letter a description ends a word with, in English and Spanish. */
	private static final Set<String> ABBREVIATIONS = Set.of("approx.", "co.", "corp.", "dr.", "etc.", "inc.",
			"incl.", "jr.", "ltd.", "mr.", "mrs.", "ms.", "no.", "sr.", "sra.", "st.", "vs.", "aprox.", "ej.");

	/**
	 * Cuts one description.
	 * @param description the description once cleaned, one line per block
	 * @return its segments, in the order they are written
	 */
	public List<Segment> split(String description) {
		String[] lines = description.split("\n");
		// A description of one line kept no structure at all, which is when guessing is worth its risk.
		Cut cut = new Cut(lines.length == 1);
		for (String line : lines) {
			boolean boilerplate = line.startsWith("> ");
			cut.line(boilerplate ? line.substring(2) : line, boilerplate);
		}
		return cut.segments;
	}

	/** The segments cut so far, and the heading the next one sits under. */
	private static final class Cut {

		private final List<Segment> segments = new ArrayList<>();

		private String under;

		private boolean boilerplate;

		private final boolean guessingItems;

		Cut(boolean guessingItems) {
			this.guessingItems = guessingItems;
		}

		void line(String line, boolean boilerplate) {
			// A section of the company's text never heads the role's, nor the other way round.
			if (boilerplate != this.boilerplate) {
				this.under = null;
				this.boilerplate = boilerplate;
			}
			if (line.startsWith("# ")) {
				heading(line.substring(2));
			}
			else if (line.startsWith("- ")) {
				add(SegmentKindEnum.ITEM, LEADING_BULLET.matcher(line.substring(2)).replaceFirst(""));
			}
			else {
				plain(line);
			}
		}

		/**
		 * A line the sweep could not say anything of. A bullet or a number typed at its start makes it
		 * an item, and every bullet typed inside it starts another; what precedes the first is read as
		 * prose.
		 */
		private void plain(String line) {
			Matcher bullet = LEADING_BULLET.matcher(line);
			boolean item = bullet.lookingAt();
			String[] pieces = INLINE_BULLET.split(item ? line.substring(bullet.end()) : line);
			for (int at = 0; at < pieces.length; at++) {
				if (pieces[at].isBlank()) {
					continue;
				}
				if (item || at > 0) {
					add(SegmentKindEnum.ITEM, pieces[at].strip());
				}
				else if (readsLikeAHeading(pieces[at].strip())) {
					heading(pieces[at].strip());
				}
				else {
					for (String sentence : sentences(pieces[at].strip())) {
						sentence(sentence);
					}
				}
			}
		}

		/**
		 * One sentence, cutting out the heading that opens it when it opens with one, as in
		 * {@code Requirements: 5+ years of Java}. Only a heading said the way sections are headed
		 * is cut: {@code Location: Remote} is a sentence.
		 */
		private void sentence(String sentence) {
			int colon = sentence.indexOf(':');
			if (colon > 0 && namedLikeAHeading(sentence.substring(0, colon))) {
				heading(sentence.substring(0, colon));
				sentence = sentence.substring(colon + 1).strip();
			}
			if (sentence.isEmpty()) {
				return;
			}
			if (!this.guessingItems) {
				add(SegmentKindEnum.SENTENCE, sentence);
				return;
			}
			List<String> pieces = guessedItems(sentence);
			if (pieces.size() > 1 && readsLikeAHeading(pieces.get(0))) {
				heading(pieces.get(0));
			}
			else {
				add(SegmentKindEnum.SENTENCE, pieces.get(0));
			}
			for (String item : pieces.subList(1, pieces.size())) {
				add(SegmentKindEnum.ITEM, item);
			}
		}

		private void heading(String heading) {
			if (!hasAWord(heading)) {
				return;
			}
			String text = headingText(heading);
			this.under = null;
			add(SegmentKindEnum.HEADING, text);
			this.under = text;
		}

		/** Adds a segment, unless cleaning left its text without a word, as it does a line of emoji. */
		private void add(SegmentKindEnum kind, String text) {
			if (!hasAWord(text)) {
				return;
			}
			this.segments.add(new Segment(kind, this.under, text, this.boilerplate));
		}

	}

	/**
	 * A line cut where a sentence ends: a full stop, question or exclamation mark followed by what can
	 * start the next one. A full stop that ends an abbreviation, as in {@code e.g. Payments}, does not.
	 */
	private static List<String> sentences(String line) {
		List<String> sentences = new ArrayList<>();
		int start = 0;
		Matcher end = SENTENCE_END.matcher(line);
		while (end.find()) {
			if (!endsAnAbbreviation(line.substring(start, end.start()))) {
				sentences.add(line.substring(start, end.start()));
				start = end.end();
			}
		}
		sentences.add(line.substring(start));
		return sentences;
	}

	/**
	 * Whether a piece of text ends in an abbreviation: a run of single letters each with a full stop,
	 * or a known one.
	 */
	private static boolean endsAnAbbreviation(String text) {
		String word = text.substring(text.lastIndexOf(' ') + 1).toLowerCase(Locale.ROOT);
		return LETTERS_WITH_FULL_STOPS.matcher(word).matches() || ABBREVIATIONS.contains(word);
	}

	/**
	 * Whether a line the sweep did not mark reads like a heading: short, with no sentence ending
	 * inside it, and either leading into what follows with a colon, written in capitals, or said the
	 * way a section of a job description is headed in English or Spanish.
	 */
	private static boolean readsLikeAHeading(String line) {
		if (line.length() > HEADING_LENGTH || sentences(line).size() > 1) {
			return false;
		}
		if (line.endsWith(":")) {
			return true;
		}
		if (line.codePoints().filter(Character::isLetter).count() >= 4 && line.equals(line.toUpperCase(Locale.ROOT))) {
			return true;
		}
		return namedLikeAHeading(headingText(line));
	}

	/** Whether a piece of text is said the way a section of a job description is headed. */
	private static boolean namedLikeAHeading(String text) {
		String said = text.strip().replace('’', '\'').toLowerCase(Locale.ROOT);
		if (HEADING_PHRASES.contains(said)) {
			return true;
		}
		String[] words = said.split(" ");
		return words.length <= 4 && (HEADING_NOUNS.contains(words[0]) || HEADING_NOUNS.contains(words[words.length - 1]));
	}

	/**
	 * A sentence cut where a list flattened without punctuation seems to start a new item: at a space
	 * between a lowercase word, a digit or a closing parenthesis and a word that opens a requirement
	 * or a heading. The last resort, for a description that kept no structure at all.
	 */
	private static List<String> guessedItems(String sentence) {
		List<String> pieces = new ArrayList<>();
		Matcher start = ITEM_START.matcher(sentence);
		int from = 0;
		for (int at = 1; at + 1 < sentence.length(); at++) {
			if (sentence.charAt(at) == ' ' && mayStartAnItem(sentence, at)
					&& start.region(at + 1, sentence.length()).lookingAt()) {
				pieces.add(sentence.substring(from, at));
				from = at + 1;
			}
		}
		pieces.add(sentence.substring(from));
		return pieces;
	}

	/**
	 * Whether the space at an index sits between a lowercase word, a digit or a ")" and a capital or a
	 * digit.
	 */
	private static boolean mayStartAnItem(String sentence, int space) {
		char before = sentence.charAt(space - 1);
		char after = sentence.charAt(space + 1);
		return (Character.isLowerCase(before) || Character.isDigit(before) || before == ')')
				&& (Character.isUpperCase(after) || Character.isDigit(after));
	}

	private static boolean hasAWord(String text) {
		return text.codePoints().anyMatch(Character::isLetterOrDigit);
	}

	/** A heading without the colon that leads into what it heads. */
	private static String headingText(String heading) {
		return heading.endsWith(":") ? heading.substring(0, heading.length() - 1).strip() : heading;
	}

}
