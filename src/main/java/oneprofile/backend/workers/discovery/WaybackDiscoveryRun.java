package oneprofile.backend.workers.discovery;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

/**
 * Discovers the slugs of one ATS the Wayback Machine captured, every capture back to its first, and
 * records them as companies.
 * <p>
 * Wayback cannot be asked for only what it captured since the last run (ADR-0023), so every run
 * reads the whole archive under each of the ATS's prefixes, and the companies already held are
 * left as they are. Wayback holds {@code http://} captures, which the URL rules reject as they
 * reject them from Common Crawl.
 */
@Component
public class WaybackDiscoveryRun {

	private static final Log logger = LogFactory.getLog(WaybackDiscoveryRun.class);

	private static final String SCHEME = "https://";

	private final WaybackCdxParser cdx;

	private final CompanyStore companies;

	public WaybackDiscoveryRun(WaybackCdxParser cdx, CompanyStore companies) {
		this.cdx = cdx;
		this.companies = companies;
	}

	/**
	 * Reads every capture under an ATS's board URL prefixes and records the slugs they name. Nothing
	 * is recorded unless every prefix was read whole.
	 * @param ats the ATS whose boards to look for
	 * @return what the run found and added
	 * @throws IOException if the archive could not be read
	 */
	public Report discover(AtsEnum ats) throws IOException {
		List<String> urlPrefixes = switch (ats) {
			case GREENHOUSE -> GreenhouseUrlRule.BOARD_URL_PREFIXES;
			case ASHBY -> AshbyUrlRule.BOARD_URL_PREFIXES;
		};
		Function<String, Optional<String>> slugIn = switch (ats) {
			case GREENHOUSE -> GreenhouseUrlRule::slugIn;
			case ASHBY -> AshbyUrlRule::slugIn;
		};
		SortedSet<String> slugs = new TreeSet<>();
		long captures = 0;
		for (String urlPrefix : urlPrefixes) {
			// The CDX server matches a prefix across both schemes, and is asked for it without one.
			String withoutScheme = urlPrefix.substring(SCHEME.length());
			captures += this.cdx.capturedUrls(withoutScheme, (url) -> slugIn.apply(url).ifPresent(slugs::add));
			logger.info("Read %s from Wayback: %d captures, %d distinct slugs so far".formatted(withoutScheme,
					captures, slugs.size()));
		}
		return new Report(ats, captures, slugs.size(), this.companies.record(ats, slugs));
	}

	/**
	 * What one run found.
	 *
	 * @param ats the ATS whose boards were looked for
	 * @param captures how many captures were read under its prefixes, {@code http://} ones included
	 * @param slugs how many distinct slugs they named
	 * @param companiesAdded how many of them were not held yet
	 */
	public record Report(AtsEnum ats, long captures, int slugs, int companiesAdded) {
	}

}
