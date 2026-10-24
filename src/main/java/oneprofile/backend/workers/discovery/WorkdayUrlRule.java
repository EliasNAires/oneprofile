package oneprofile.backend.workers.discovery;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oneprofile.backend.storage.company.RegionEnum;

/**
 * What slug discovery knows about Workday: where its boards are published, and how to read a slug out
 * of a URL that captured one.
 * <p>
 * A Workday board is a tenant, a host and a site, published at
 * {@code <tenant>.<host>.myworkdayjobs.com/[<locale>/]<site>} and again at
 * {@code <host>.myworkdaysite.com/[<locale>/]recruiting/<tenant>/<site>}, its API under
 * {@code <tenant>.<host>.myworkdayjobs.com/wday/cxs/<tenant>/<site>}
 * (docs/measurements/2026-10-workday.md). Its slug is the three of them as {@code <tenant>.<host>/<site>},
 * lowercased, since Workday answers a site in any case. Every tenant has a subdomain of its own, so
 * an archive is asked for every subdomain of the two domains, written {@code *.} as the Wayback
 * Machine's CDX server writes it.
 */
final class WorkdayUrlRule {

	/** Every URL prefix a board is published under, as a search of a crawl index has to ask for it. */
	static final List<String> BOARD_URL_PREFIXES = List.of("https://*.myworkdayjobs.com/", "https://*.myworkdaysite.com/");

	private static final String NAME = "[a-z0-9][a-z0-9-]*";

	private static final String SITE = "[A-Za-z0-9_-]+";

	/** A locale the site may sit behind, which is never a site of its own. */
	private static final String LOCALE = "(?:[A-Za-z]{2}-[A-Za-z]{2}/)?(?![A-Za-z]{2}-[A-Za-z]{2}(?:[/?#]|$))";

	private static final String END = "(?:[/?#].*)?";

	private static final Pattern JOBS_SITE = Pattern.compile(
			"https://(?<tenant>%s)\\.(?<host>%s)\\.myworkdayjobs\\.com/(?:wday/cxs/\\k<tenant>/|(?!wday/)%s)(?<site>%s)%s"
				.formatted(NAME, NAME, LOCALE, SITE, END),
			Pattern.CASE_INSENSITIVE);

	private static final Pattern WORKDAY_SITE = Pattern.compile(
			"https://(?<host>%s)\\.myworkdaysite\\.com/%srecruiting/(?<tenant>%s)/(?<site>%s)%s".formatted(NAME, LOCALE,
					NAME, SITE, END),
			Pattern.CASE_INSENSITIVE);

	private WorkdayUrlRule() {
	}

	/**
	 * Reads the slug out of a captured URL.
	 * @param url the URL a crawl captured
	 * @return the board's slug, or empty if the URL is not a board of ours
	 */
	static Optional<String> slugIn(String url) {
		for (Pattern board : List.of(JOBS_SITE, WORKDAY_SITE)) {
			Matcher matcher = board.matcher(url);
			if (matcher.matches()) {
				return Optional.of("%s.%s/%s".formatted(matcher.group("tenant"), matcher.group("host"),
						matcher.group("site"))
					.toLowerCase(Locale.ROOT));
			}
		}
		return Optional.empty();
	}

	/**
	 * The region of a board. Workday's {@code wdN} hosts share one limit and the host is part of the
	 * slug, so they are not regions (docs/measurements/2026-10-workday.md), and every board is held
	 * in the US.
	 * @param url a URL {@link #slugIn} reads a slug from
	 * @return its region
	 */
	static RegionEnum regionIn(String url) {
		return RegionEnum.US;
	}

}
