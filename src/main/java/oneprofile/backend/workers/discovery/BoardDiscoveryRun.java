package oneprofile.backend.workers.discovery;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.company.RegionEnum;
import org.springframework.stereotype.Component;

/**
 * Discovers the slugs of one ATS a crawl captured — the boards named by the board URLs that were
 * public web pages when the crawl was taken — and records them as companies.
 * <p>
 * Greenhouse's four hosts sit next to each other in a crawl index, so their searches keep landing on
 * the same blocks — the block before one host's run is the block after another's. Every block is
 * therefore collected before any of them is read, and read once.
 */
@Component
public class BoardDiscoveryRun {

	private final ClusterIndexParser clusterIndex;

	private final CdxBlockParser blockReader;

	private final CompanyStore companies;

	public BoardDiscoveryRun(ClusterIndexParser clusterIndex, CdxBlockParser blockReader, CompanyStore companies) {
		this.clusterIndex = clusterIndex;
		this.blockReader = blockReader;
		this.companies = companies;
	}

	/**
	 * Reads the blocks of a crawl that can hold an ATS's board URLs and records the slugs they name.
	 * Nothing is recorded unless the whole crawl was read.
	 * @param ats the ATS whose boards to look for
	 * @param crawl the crawl to search, such as {@code CC-MAIN-2026-34}
	 * @return what the run found and added
	 * @throws IOException if the crawl could not be read
	 */
	public Report discover(AtsEnum ats, String crawl) throws IOException {
		List<String> urlPrefixes = switch (ats) {
			case GREENHOUSE -> GreenhouseUrlRule.BOARD_URL_PREFIXES;
			case ASHBY -> AshbyUrlRule.BOARD_URL_PREFIXES;
			case LEVER -> LeverUrlRule.BOARD_URL_PREFIXES;
			case WORKDAY -> WorkdayUrlRule.BOARD_URL_PREFIXES;
		};
		Function<String, Optional<String>> slugIn = switch (ats) {
			case GREENHOUSE -> GreenhouseUrlRule::slugIn;
			case ASHBY -> AshbyUrlRule::slugIn;
			case LEVER -> LeverUrlRule::slugIn;
			case WORKDAY -> WorkdayUrlRule::slugIn;
		};
		Function<String, RegionEnum> regionIn = switch (ats) {
			case GREENHOUSE -> GreenhouseUrlRule::regionIn;
			case ASHBY -> AshbyUrlRule::regionIn;
			case LEVER -> LeverUrlRule::regionIn;
			case WORKDAY -> WorkdayUrlRule::regionIn;
		};
		Set<CdxBlock> blocksToRead = new LinkedHashSet<>();
		for (String urlPrefix : urlPrefixes) {
			blocksToRead.addAll(this.clusterIndex.locate(crawl, urlPrefix));
		}
		SortedMap<String, RegionEnum> slugs = new TreeMap<>();
		for (CdxBlock block : blocksToRead) {
			for (String url : this.blockReader.capturedUrls(crawl, block)) {
				slugIn.apply(url)
					.ifPresent((slug) -> slugs.merge(slug, regionIn.apply(url), BoardDiscoveryRun::eitherRegion));
			}
		}
		return new Report(ats, crawl, slugs.size(), this.companies.record(ats, slugs));
	}

	/**
	 * The region of a slug captured under both regions' hosts in one run, by Common Crawl or Wayback:
	 * the EU, since only a board of the EU is published under an EU host (ADR-0022).
	 */
	static RegionEnum eitherRegion(RegionEnum one, RegionEnum other) {
		return (one == RegionEnum.EU || other == RegionEnum.EU) ? RegionEnum.EU : RegionEnum.US;
	}

	/**
	 * What one run found.
	 *
	 * @param ats the ATS whose boards were looked for
	 * @param crawl the crawl that was read
	 * @param slugs how many distinct slugs it named
	 * @param companiesAdded how many of them were not held yet
	 */
	public record Report(AtsEnum ats, String crawl, int slugs, int companiesAdded) {
	}

}
