package oneprofile.backend.workers.stoplist;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Measures the piece occurrences the stoplist of skill discovery's recall is built from.
 */
@RestController
class StoplistEndpoint {

	private final StoplistRun stoplist;

	StoplistEndpoint(StoplistRun stoplist) {
		this.stoplist = stoplist;
	}

	@PostMapping("/stoplists")
	StoplistRun.Report measure() {
		return this.stoplist.measure();
	}

}
