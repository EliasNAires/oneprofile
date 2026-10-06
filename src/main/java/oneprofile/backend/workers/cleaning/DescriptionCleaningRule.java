package oneprofile.backend.workers.cleaning;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Takes out of a description what is decoration rather than text, so that the body pass and the
 * labeller it is scored against read the same words (ADR-0012).
 * <p>
 * The sweep already stores a description as plain text, its markup gone and one line per
 * block, so what is left to clean is characters, line by line. They are spelled as what survives
 * rather than as what is dropped, because a list of emoji always misses one: the letters, marks and digits of any script, punctuation, the
 * currency and mathematical signs that carry a pay range or a {@code C++}, and the accent a
 * keyboard types in place of an apostrophe, as in {@code You´re}. Everything else — emoji,
 * pictographs, the private-use glyphs of a word processor's bullets, the invisible characters
 * that join or vary them — becomes a space, so that the words on either side of it stay apart.
 * A {@code •} is punctuation and stays, and so do the marks the sweep puts at the start of a line:
 * where a list item begins is where a description is cut into segments.
 */
@Component
public class DescriptionCleaningRule {

	private static final Pattern LINE_BREAK = Pattern.compile("\\R");

	private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

	/**
	 * Cleans one description.
	 * @param description the description as the sweep stored it
	 * @return what survives, as lines of words separated by single spaces, without the lines that
	 * kept no text
	 */
	public String clean(String description) {
		return LINE_BREAK.splitAsStream(description)
			.map(DescriptionCleaningRule::cleanLine)
			.filter((line) -> !line.isEmpty())
			.collect(Collectors.joining("\n"));
	}

	private static String cleanLine(String description) {
		StringBuilder kept = new StringBuilder(description.length());
		description.codePoints().forEach((character) -> {
			if (isText(character)) {
				kept.appendCodePoint(character);
			}
			else {
				kept.append(' ');
			}
		});
		return WHITESPACE.matcher(kept).replaceAll(" ").trim();
	}

	private static boolean isText(int character) {
		if (Character.isLetterOrDigit(character)) {
			return true;
		}
		return switch (Character.getType(character)) {
			// A variation selector is a mark too, but one that only chooses how an emoji is drawn.
			case Character.NON_SPACING_MARK -> !isVariationSelector(character);
			case Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK, Character.LETTER_NUMBER,
					Character.OTHER_NUMBER, Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION,
					Character.START_PUNCTUATION, Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION,
					Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION, Character.CURRENCY_SYMBOL,
					Character.MATH_SYMBOL, Character.MODIFIER_SYMBOL ->
				true;
			default -> false;
		};
	}

	private static boolean isVariationSelector(int character) {
		return character >= 0xFE00 && character <= 0xFE0F || character >= 0xE0100 && character <= 0xE01EF;
	}

}
