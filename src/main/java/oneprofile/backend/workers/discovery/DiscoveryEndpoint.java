package oneprofile.backend.workers.discovery;

import java.io.IOException;
import oneprofile.backend.storage.company.AtsEnum;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runs slug discovery for one ATS over one crawl and records what it names as companies.
 * <p>
 * The crawl is named by the caller rather than looked up, so that a run is always against a crawl
 * someone chose. Discovery reads tens of megabytes over range requests, so a call takes as long as
 * the crawl takes to read.
 */
@RestController
class DiscoveryEndpoint {

	private final BoardDiscoveryRun discovery;

	DiscoveryEndpoint(BoardDiscoveryRun discovery) {
		this.discovery = discovery;
	}

	@PostMapping("/discoveries/{ats}/{crawl}")
	BoardDiscoveryRun.Report discover(@PathVariable String ats, @PathVariable String crawl) throws IOException {
		return this.discovery.discover(AtsEnum.named(ats), crawl);
	}

}
