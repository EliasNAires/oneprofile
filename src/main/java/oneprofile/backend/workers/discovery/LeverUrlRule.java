package oneprofile.backend.workers.discovery;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import oneprofile.backend.storage.company.RegionEnum;

/**
 * What slug discovery knows about Lever: where its boards are published, and how to read a slug and
 * its region out of a URL that captured one.
 * <p>
 * Lever publishes a board under the host of its region, {@code jobs.lever.co} or
 * {@code jobs.eu.lever.co}, and each region's API answers only for its own boards (ADR-0022). The
 * slug is the first path segment of every URL under either host, the board's own page as much as
 * each opening's and each application's.
 */
final class LeverUrlRule {

	private static final String US_PREFIX = "https://jobs.lever.co/";

	private static final String EU_PREFIX = "https://jobs.eu.lever.co/";

	/** Every URL prefix a board is published under, as a search of a crawl index has to ask for it. */
	static final List<String> BOARD_URL_PREFIXES = List.of(US_PREFIX, EU_PREFIX);

	private LeverUrlRule() {
	}

	/**
	 * Reads the slug out of a captured URL.
	 * @param url the URL a crawl captured
	 * @return the board's slug, or empty if the URL is not a board of ours
	 */
	static Optional<String> slugIn(String url) {
		for (String prefix : BOARD_URL_PREFIXES) {
			if (url.regionMatches(true, 0, prefix, 0, prefix.length())) {
				return slug(url.substring(prefix.length()));
			}
		}
		return Optional.empty();
	}

	/**
	 * The region of the host a board URL is under.
	 * @param url a URL {@link #slugIn} reads a slug from
	 * @return its region
	 */
	static RegionEnum regionIn(String url) {
		return url.regionMatches(true, 0, EU_PREFIX, 0, EU_PREFIX.length()) ? RegionEnum.EU : RegionEnum.US;
	}

	/**
	 * The first path segment as a slug. Lever's API asks for a slug in lowercase, which is how every
	 * board is published, so one captured in another case names the same board. Anything that is
	 * not spelt like a slug is no slug at all, rather than something to be cleaned up.
	 */
	private static Optional<String> slug(String path) {
		int end = 0;
		while (end < path.length() && "/?#".indexOf(path.charAt(end)) < 0) {
			end++;
		}
		String slug = path.substring(0, end).toLowerCase(Locale.ROOT);
		return slug.matches("[a-z0-9][a-z0-9._-]*") ? Optional.of(slug) : Optional.empty();
	}

}
