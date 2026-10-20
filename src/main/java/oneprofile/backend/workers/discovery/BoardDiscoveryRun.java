package oneprofile.backend.workers.discovery;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
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
		};
		Function<String, Optional<String>> slugIn = switch (ats) {
			case GREENHOUSE -> GreenhouseUrlRule::slugIn;
			case ASHBY -> AshbyUrlRule::slugIn;
		};
		Set<CdxBlock> blocksToRead = new LinkedHashSet<>();
		for (String urlPrefix : urlPrefixes) {
			blocksToRead.addAll(this.clusterIndex.locate(crawl, urlPrefix));
		}
		SortedSet<String> slugs = new TreeSet<>();
		for (CdxBlock block : blocksToRead) {
			for (String url : this.blockReader.capturedUrls(crawl, block)) {
				slugIn.apply(url).ifPresent(slugs::add);
			}
		}
		return new Report(ats, crawl, slugs.size(), this.companies.record(ats, slugs));
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
