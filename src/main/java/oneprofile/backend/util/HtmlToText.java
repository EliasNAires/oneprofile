package oneprofile.backend.util;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

/**
 * Turns the description of a Greenhouse opening into plain text. The field arrives
 * HTML-escaped inside the JSON —its literal value starts with {@code &lt;div}— so
 * getting to readable text takes two unescapes: one to recover the real HTML, and
 * another for the entities of the text itself, since a {@code &nbsp;} of the original
 * reaches us as {@code &amp;nbsp;}.
 */
public final class HtmlToText {

	private HtmlToText() {
	}

	/** Null for nothing to read, so an opening without a description stays null. */
	public static String plainText(String escapedHtml) {
		if (escapedHtml == null || escapedHtml.isBlank()) {
			return null;
		}
		// The second unescape is Jsoup's own: text() decodes the entities it parses.
		String text = Jsoup.parse(Parser.unescapeEntities(escapedHtml, false)).text();
		return text.isBlank() ? null : text;
	}
}
