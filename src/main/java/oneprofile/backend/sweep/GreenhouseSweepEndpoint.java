package oneprofile.backend.sweep;

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

	private final BoardSweep sweep;

	GreenhouseSweepEndpoint(BoardSweep sweep) {
		this.sweep = sweep;
	}

	@PostMapping("/sweeps/greenhouse")
	BoardSweep.Report sweep() {
		return this.sweep.sweepAll();
	}

}
