package oneprofile.backend.workers.discovery;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
import org.springframework.stereotype.Component;

/**
 * Discovers the Greenhouse slugs a crawl captured — the boards named by the board URLs that were
 * public web pages when the crawl was taken — and records them as companies.
 * <p>
 * The four hosts sit next to each other in a crawl index, so their searches keep landing on the
 * same blocks — the block before one host's run is the block after another's. Every block is
 * therefore collected before any of them is read, and read once.
 */
@Component
public class GreenhouseDiscoveryRun {

	private final ClusterIndexParser clusterIndex;

	private final CdxBlockParser blockReader;

	private final CompanyStore companies;

	public GreenhouseDiscoveryRun(ClusterIndexParser clusterIndex, CdxBlockParser blockReader, CompanyStore companies) {
		this.clusterIndex = clusterIndex;
		this.blockReader = blockReader;
		this.companies = companies;
	}

	/**
	 * Reads the blocks of a crawl that can hold Greenhouse board URLs and records the slugs they
	 * name. Nothing is recorded unless the whole crawl was read.
	 * @param crawl the crawl to search, such as {@code CC-MAIN-2026-34}
	 * @return what the run found and added
	 * @throws IOException if the crawl could not be read
	 */
	public Report discover(String crawl) throws IOException {
		Set<CdxBlock> blocksToRead = new LinkedHashSet<>();
		for (String urlPrefix : GreenhouseUrlRule.BOARD_URL_PREFIXES) {
			blocksToRead.addAll(this.clusterIndex.locate(crawl, urlPrefix));
		}
		SortedSet<String> slugs = new TreeSet<>();
		for (CdxBlock block : blocksToRead) {
			for (String url : this.blockReader.capturedUrls(crawl, block)) {
				GreenhouseUrlRule.slugIn(url).ifPresent(slugs::add);
			}
		}
		return new Report(crawl, slugs.size(), this.companies.record(AtsEnum.GREENHOUSE, slugs));
	}

	/**
	 * What one run found.
	 *
	 * @param crawl the crawl that was read
	 * @param slugs how many distinct slugs it named
	 * @param companiesAdded how many of them were not held yet
	 */
	public record Report(String crawl, int slugs, int companiesAdded) {
	}

}
