package oneprofile.backend.storage.normalizedvacancy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One piece of a description as cleaning cut it: a heading, a list item or a sentence. Every
 * pass that reads descriptions reads these, so none of them has to guess again where an item starts
 * or which heading it sits under.
 *
 * @param kind what the piece is
 * @param under the heading it sits under, null if there is none, and always null for a heading
 * @param text its text, without the marks that said what it was
 * @param boilerplate whether it is text the ATS adds to every vacancy of the board, such as the
 * company's introduction, rather than text written for this one
 */
public record Segment(SegmentKindEnum kind, @JsonInclude(Include.NON_NULL) String under, String text,
		@JsonInclude(Include.NON_DEFAULT) boolean boilerplate) {

	/**
	 * A word: letters and digits of any script, holding together across the apostrophes, full stops
	 * and hyphens inside it, and keeping the {@code +} and {@code #} that end {@code C++}, {@code C#}
	 * or {@code 5+}.
	 */
	private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}](?:[\\p{L}\\p{N}'’.+#-]*[\\p{L}\\p{N}+#])?");

	/**
	 * Its tokens: the words of its text, lowercased. Derived rather than stored, because they are
	 * quicker to read off the text than to read from the database.
	 * @return its words, in the order they are written
	 */
	public List<String> tokens() {
		return WORD.matcher(this.text).results().map((word) -> word.group().toLowerCase(Locale.ROOT)).toList();
	}

}
