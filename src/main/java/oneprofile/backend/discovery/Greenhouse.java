package oneprofile.backend.discovery;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * What slug discovery knows about Greenhouse: where its boards are published, and how to read a
 * slug out of a URL that captured one.
 * <p>
 * Greenhouse publishes the same board under four hosts — the original {@code boards} and the
 * current {@code job-boards}, each with a European sibling — and the slug is the first path
 * segment, except on an embedded board, where the path names the embed and the {@code for}
 * parameter names the board.
 */
final class Greenhouse {

	/** Every host a board is served from: the original two and their European siblings. */
	private static final List<String> BOARD_HOSTS = List.of("boards.greenhouse.io", "job-boards.greenhouse.io",
			"boards.eu.greenhouse.io", "job-boards.eu.greenhouse.io");

	/** Every URL prefix a board is published under, as a search of a crawl index has to ask for it. */
	static final List<String> BOARD_URL_PREFIXES = BOARD_HOSTS.stream().map((host) -> "https://" + host + "/").toList();

	private static final String EMBED = "embed";

	private Greenhouse() {
	}

	/**
	 * Reads the slug out of a captured URL.
	 * @param url the URL a crawl captured
	 * @return the board's slug, or empty if the URL is not a board of ours
	 */
	static Optional<String> slugIn(String url) {
		String host = hostIn(url);
		if (!BOARD_HOSTS.contains(host)) {
			return Optional.empty();
		}
		String rest = url.substring("https://".length() + host.length());
		String path = upTo(rest.startsWith("/") ? rest.substring(1) : rest, "?#");
		String query = rest.contains("?") ? rest.substring(rest.indexOf('?') + 1) : "";
		String segment = upTo(path, "/");
		return EMBED.equals(segment) ? slug(parameter(query, "for")) : slug(segment);
	}

	/**
	 * The host of an https URL, lowercased because a host names the same server in either case, or
	 * empty for anything else: boards are only ever served over https.
	 */
	private static String hostIn(String url) {
		if (!url.startsWith("https://")) {
			return "";
		}
		return upTo(url.substring("https://".length()), "/?#").toLowerCase(Locale.ROOT);
	}

	/** The value of a query parameter, or empty if the query does not carry it. */
	private static String parameter(String query, String name) {
		for (String pair : query.split("&")) {
			if (pair.startsWith(name + "=")) {
				return pair.substring(name.length() + 1);
			}
		}
		return "";
	}

	/**
	 * A slug as it is held: lowercased, because Greenhouse serves the same board whatever case its
	 * URL was written in. Anything that is not spelt like a slug is no slug at all, rather than
	 * something to be cleaned up.
	 */
	private static Optional<String> slug(String candidate) {
		String slug = candidate.toLowerCase(Locale.ROOT);
		return slug.matches("[a-z0-9][a-z0-9._-]*") ? Optional.of(slug) : Optional.empty();
	}

	/** The part of {@code text} before the first of the {@code delimiters}, or all of it. */
	private static String upTo(String text, String delimiters) {
		for (int i = 0; i < text.length(); i++) {
			if (delimiters.indexOf(text.charAt(i)) >= 0) {
				return text.substring(0, i);
			}
		}
		return text;
	}

}
