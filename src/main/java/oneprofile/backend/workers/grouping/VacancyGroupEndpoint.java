package oneprofile.backend.workers.grouping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Groups every vacancy of the engineering subset, replacing what the last run recorded. */
@RestController
class VacancyGroupEndpoint {

	private final VacancyGroupRun run;

	/** Triggers the vacancy-group run. */
	VacancyGroupEndpoint(VacancyGroupRun run) {
		this.run = run;
	}

	/** Returns what grouping the engineering subset found. */
	@PostMapping("/vacancy-groups")
	VacancyGroupRun.Report group() {
		return this.run.groupVacancies();
	}

}
