package oneprofile.backend.workers.sweep;

import oneprofile.backend.storage.company.AtsEnum;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sweeps every board of one ATS that has openings and mirrors what each one publishes.
 * <p>
 * A run asks the ATS for the whole of every active board and is paced, so a call takes as long as
 * those requests take. Asked by its ATS, Workday is swept long; it also has a short sweep (ADR-0032).
 */
@RestController
class SweepEndpoint {

	private final BoardSweepRun sweep;

	SweepEndpoint(BoardSweepRun sweep) {
		this.sweep = sweep;
	}

	/** Sweeps the selected boards and returns what the run found. */
	@PostMapping("/sweeps/{ats}")
	BoardSweepRun.Report sweep(@PathVariable String ats, @RequestParam(required = false) String after,
			@RequestParam(required = false) Integer limit) {
		return this.sweep.sweep(AtsEnum.named(ats), after, limit);
	}

	/** Sweeps the selected Workday boards short and returns what the run found. */
	@PostMapping("/sweeps/workday/short")
	BoardSweepRun.Report shortSweep(@RequestParam(required = false) String after,
			@RequestParam(required = false) Integer limit) {
		return this.sweep.shortSweep(after, limit);
	}

}
