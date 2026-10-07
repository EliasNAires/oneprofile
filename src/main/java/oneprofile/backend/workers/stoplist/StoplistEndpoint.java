package oneprofile.backend.workers.stoplist;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Builds the stoplist of skill discovery's recall, and reports what it is built from.
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
