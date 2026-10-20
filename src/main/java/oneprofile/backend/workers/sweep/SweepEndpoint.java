package oneprofile.backend.workers.sweep;

import oneprofile.backend.storage.company.AtsEnum;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sweeps every board of one ATS that has openings and mirrors what each one publishes.
 * <p>
 * A run asks the ATS for the whole of every active board and is paced, so a call takes as long as
 * those requests take.
 */
@RestController
class SweepEndpoint {

	private final BoardSweepRun sweep;

	SweepEndpoint(BoardSweepRun sweep) {
		this.sweep = sweep;
	}

	@PostMapping("/sweeps/{ats}")
	BoardSweepRun.Report sweep(@PathVariable String ats) {
		return this.sweep.sweepAll(AtsEnum.named(ats));
	}

}
