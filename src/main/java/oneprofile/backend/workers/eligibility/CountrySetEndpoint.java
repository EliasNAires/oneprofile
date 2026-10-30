package oneprofile.backend.workers.eligibility;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reads the country set of every vacancy of the engineering subset, replacing what the last run recorded. */
@RestController
class CountrySetEndpoint {

	private final CountrySetRun run;

	CountrySetEndpoint(CountrySetRun run) {
		this.run = run;
	}

	@PostMapping("/country-sets")
	CountrySetRun.Report read() {
		return this.run.readCountrySets();
	}

}
