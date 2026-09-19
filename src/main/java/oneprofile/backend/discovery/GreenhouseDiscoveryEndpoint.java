package oneprofile.backend.discovery;

import java.io.IOException;
import java.util.SortedSet;
import oneprofile.backend.company.Ats;
import oneprofile.backend.company.Companies;
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

	private final GreenhouseSlugDiscovery discovery;

	private final Companies companies;

	GreenhouseDiscoveryEndpoint(GreenhouseSlugDiscovery discovery, Companies companies) {
		this.discovery = discovery;
		this.companies = companies;
	}

	@PostMapping("/discoveries/{crawl}")
	Report discover(@PathVariable String crawl) throws IOException {
		SortedSet<String> slugs = this.discovery.discover(crawl);
		return new Report(crawl, slugs.size(), this.companies.record(Ats.GREENHOUSE, slugs));
	}

	/**
	 * What one run found.
	 *
	 * @param crawl the crawl that was read
	 * @param slugs how many distinct slugs it named
	 * @param companiesAdded how many of them were not held yet
	 */
	record Report(String crawl, int slugs, int companiesAdded) {
	}

}
