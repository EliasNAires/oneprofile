package oneprofile.backend.workers.discovery;

import java.io.IOException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runs slug discovery over one crawl and records what it names as companies.
 * <p>
 * The crawl is named by the caller rather than looked up, so that a run is always against a crawl
 * someone chose. Discovery reads tens of megabytes over range requests, so a call takes as long as
 * the crawl takes to read.
 */
@RestController
class GreenhouseDiscoveryEndpoint {

	private final GreenhouseDiscoveryRun discovery;

	GreenhouseDiscoveryEndpoint(GreenhouseDiscoveryRun discovery) {
		this.discovery = discovery;
	}

	@PostMapping("/discoveries/{crawl}")
	GreenhouseDiscoveryRun.Report discover(@PathVariable String crawl) throws IOException {
		return this.discovery.discover(crawl);
	}

}
