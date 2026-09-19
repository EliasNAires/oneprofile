package oneprofile.backend.discovery;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Discovers the Greenhouse slugs a crawl captured: the boards named by the board URLs that were
 * public web pages when the crawl was taken.
 * <p>
 * The four hosts sit next to each other in a crawl index, so their searches keep landing on the
 * same blocks — the block before one host's run is the block after another's. Every block is
 * therefore collected before any of them is read, and read once.
 */
public class GreenhouseSlugDiscovery {

	private final ClusterIndexReader clusterIndex;

	private final CdxBlockReader blockReader;

	public GreenhouseSlugDiscovery(ClusterIndexReader clusterIndex, CdxBlockReader blockReader) {
		this.clusterIndex = clusterIndex;
		this.blockReader = blockReader;
	}

	/**
	 * Reads the blocks of a crawl that can hold Greenhouse board URLs and returns the slugs they
	 * name.
	 * @param crawl the crawl to search, such as {@code CC-MAIN-2026-34}
	 * @return the distinct slugs found, in slug order
	 * @throws IOException if the crawl could not be read
	 */
	public SortedSet<String> discover(String crawl) throws IOException {
		Set<CdxBlock> blocksToRead = new LinkedHashSet<>();
		for (String urlPrefix : Greenhouse.BOARD_URL_PREFIXES) {
			blocksToRead.addAll(this.clusterIndex.locate(crawl, urlPrefix));
		}
		SortedSet<String> slugs = new TreeSet<>();
		for (CdxBlock block : blocksToRead) {
			for (String url : this.blockReader.capturedUrls(crawl, block)) {
				Greenhouse.slugIn(url).ifPresent(slugs::add);
			}
		}
		return slugs;
	}

}
