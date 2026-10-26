package oneprofile.backend.workers.normalization;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Breaks down the title of every vacancy of the engineering subset, replacing what the last run recorded. */
@RestController
class TitleBreakdownEndpoint {

	private final TitleBreakdownRun breakdown;

	TitleBreakdownEndpoint(TitleBreakdownRun breakdown) {
		this.breakdown = breakdown;
	}

	@PostMapping("/title-breakdowns")
	TitleBreakdownRun.Report breakDown() {
		return this.breakdown.breakDownTitles();
	}

}
