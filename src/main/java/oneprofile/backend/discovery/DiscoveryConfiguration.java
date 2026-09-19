package oneprofile.backend.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires slug discovery against Common Crawl. The components themselves know nothing about Spring,
 * so that a test can hand them an index of its own rather than the live service.
 */
@Configuration(proxyBeanMethods = false)
public class DiscoveryConfiguration {

	/** How this client identifies itself to Common Crawl, which asks to be able to tell who calls. */
	private static final String USER_AGENT = "oneprofile (+https://github.com/EliasNAires/oneprofile)";

	@Bean
	RangeReader commonCrawlRangeReader() {
		return new HttpRangeReader(USER_AGENT);
	}

	@Bean
	ClusterIndexReader clusterIndexReader(RangeReader rangeReader) {
		return new ClusterIndexReader(rangeReader);
	}

}
