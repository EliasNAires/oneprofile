package oneprofile.backend.workers.deduplication;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Marks exact duplicates across the engineering subset, replacing the last run's marks. */
@RestController
class DuplicateEndpoint {

	private final DuplicateRun run;

	/** Triggers the duplicate run. */
	DuplicateEndpoint(DuplicateRun run) {
		this.run = run;
	}

	/** Returns how many vacancies the subset holds and how many are duplicates. */
	@PostMapping("/duplicates")
	DuplicateRun.Report markDuplicates() {
		return this.run.markDuplicates();
	}

}
