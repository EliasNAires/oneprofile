package oneprofile.backend.workers.discovery;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import oneprofile.backend.storage.company.RegionEnum;

/**
 * What slug discovery knows about Ashby: where its boards are published, and how to read a slug out
 * of a URL that captured one.
 * <p>
 * Ashby publishes every board under one host, and the slug is the first path segment of every URL
 * under it, the board's own page as much as each opening's and each application's.
 */
final class AshbyUrlRule {

	/** The one URL prefix every board is published under. */
	private static final String BOARD_URL_PREFIX = "https://jobs.ashbyhq.com/";

	/** Every URL prefix a board is published under, as a search of a crawl index has to ask for it. */
	static final List<String> BOARD_URL_PREFIXES = List.of(BOARD_URL_PREFIX);

	private AshbyUrlRule() {
	}

	/**
	 * Reads the slug out of a captured URL.
	 * @param url the URL a crawl captured
	 * @return the board's slug, or empty if the URL is not a board of ours
	 */
	static Optional<String> slugIn(String url) {
		if (!url.regionMatches(true, 0, BOARD_URL_PREFIX, 0, BOARD_URL_PREFIX.length())) {
			return Optional.empty();
		}
		String rest = url.substring(BOARD_URL_PREFIX.length());
		int end = 0;
		while (end < rest.length() && "/?#".indexOf(rest.charAt(end)) < 0) {
			end++;
		}
		// Ashby serves the same board whatever case its URL was written in. Anything that is not
		// spelt like a slug is no slug at all, rather than something to be cleaned up.
		String slug = rest.substring(0, end).toLowerCase(Locale.ROOT);
		return slug.matches("[a-z0-9][a-z0-9._-]*") ? Optional.of(slug) : Optional.empty();
	}

	/**
	 * The region of the host a board URL is under: Ashby has one host, so every board is in the US.
	 * @param url a URL {@link #slugIn} reads a slug from
	 * @return its region
	 */
	static RegionEnum regionIn(String url) {
		return RegionEnum.US;
	}

}
