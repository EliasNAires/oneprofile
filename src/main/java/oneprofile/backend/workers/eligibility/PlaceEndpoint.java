package oneprofile.backend.workers.eligibility;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reads the places of every location string of the engineering subset, replacing what the last run recorded. */
@RestController
class PlaceEndpoint {

	private final PlaceRun run;

	PlaceEndpoint(PlaceRun run) {
		this.run = run;
	}

	@PostMapping("/places")
	PlaceRun.Report read() {
		return this.run.readPlaces();
	}

}
