package oneprofile.backend.workers.discovery;

import java.io.IOException;
import oneprofile.backend.storage.company.AtsEnum;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runs slug discovery for one ATS over one archive and records what it names as companies: over a
 * Common Crawl crawl, or over the whole Wayback Machine.
 * <p>
 * A crawl is named by the caller rather than looked up, so that a run is always against a crawl
 * someone chose. Discovery reads tens of megabytes over range requests from a crawl, and millions
 * of captures from Wayback, so a call takes as long as the archive takes to read: about 25 minutes
 * for Greenhouse's Wayback captures. {@code wayback} is matched before {@code {crawl}}, a literal
 * segment winning over a variable one, and no crawl is named so.
 */
@RestController
class DiscoveryEndpoint {

	private final BoardDiscoveryRun discovery;

	private final WaybackDiscoveryRun waybackDiscovery;

	DiscoveryEndpoint(BoardDiscoveryRun discovery, WaybackDiscoveryRun waybackDiscovery) {
		this.discovery = discovery;
		this.waybackDiscovery = waybackDiscovery;
	}

	@PostMapping("/discoveries/{ats}/wayback")
	WaybackDiscoveryRun.Report discoverInWayback(@PathVariable String ats) throws IOException {
		return this.waybackDiscovery.discover(AtsEnum.named(ats));
	}

	@PostMapping("/discoveries/{ats}/{crawl}")
	BoardDiscoveryRun.Report discover(@PathVariable String ats, @PathVariable String crawl) throws IOException {
		return this.discovery.discover(AtsEnum.named(ats), crawl);
	}

}
