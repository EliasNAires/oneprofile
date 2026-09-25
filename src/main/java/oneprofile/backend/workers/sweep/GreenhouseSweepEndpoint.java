package oneprofile.backend.workers.sweep;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sweeps every Greenhouse board that has openings and mirrors what each one publishes.
 * <p>
 * A run asks the ATS for the whole of every active board and is paced, so a call takes as long as
 * those requests take.
 */
@RestController
class GreenhouseSweepEndpoint {

	private final BoardSweepRun sweep;

	GreenhouseSweepEndpoint(BoardSweepRun sweep) {
		this.sweep = sweep;
	}

	@PostMapping("/sweeps/greenhouse")
	BoardSweepRun.Report sweep() {
		return this.sweep.sweepAll();
	}

}
