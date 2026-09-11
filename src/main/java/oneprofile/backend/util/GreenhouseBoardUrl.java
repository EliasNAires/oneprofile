package oneprofile.backend.util;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the company slug from a Greenhouse job board URL, as found in the
 * CommonCrawl index.
 */
public final class GreenhouseBoardUrl {

	/** Both board domains; the rest of the URL is captured as path + query + fragment. */
	private static final Pattern BOARD_URL = Pattern
			.compile("^https://(?:www\\.)?(?:job-)?boards\\.greenhouse\\.io/(.*)$");

	/** The board embedded as an iframe carries the slug in the {@code for} query parameter. */
	private static final String EMBED_SEGMENT = "embed";

	private static final Pattern SLUG = Pattern.compile("[A-Za-z0-9_-]+");

	/** Both board domains as CommonCrawl index prefixes. */
	private static final List<String> INDEX_PATTERNS = List.of("boards.greenhouse.io/", "job-boards.greenhouse.io/");

	private GreenhouseBoardUrl() {
	}

	public static List<String> indexPatterns() {
		return INDEX_PATTERNS;
	}

	public static Optional<String> slugFrom(String url) {
		Matcher matcher = BOARD_URL.matcher(url);
		if (!matcher.matches()) {
			return Optional.empty();
		}

		String rest = matcher.group(1);
		int fragment = rest.indexOf('#');
		if (fragment >= 0) {
			rest = rest.substring(0, fragment);
		}

		int questionMark = rest.indexOf('?');
		String path = questionMark >= 0 ? rest.substring(0, questionMark) : rest;
		String query = questionMark >= 0 ? rest.substring(questionMark + 1) : "";

		int slash = path.indexOf('/');
		String firstSegment = slash >= 0 ? path.substring(0, slash) : path;

		String candidate = EMBED_SEGMENT.equals(firstSegment) ? queryParameter(query, "for") : firstSegment;

		return SLUG.matcher(candidate).matches() ? Optional.of(candidate) : Optional.empty();
	}

	private static String queryParameter(String query, String name) {
		for (String parameter : query.split("&")) {
			int equals = parameter.indexOf('=');
			if (equals >= 0 && parameter.substring(0, equals).equals(name)) {
				return parameter.substring(equals + 1);
			}
		}
		return "";
	}
}
