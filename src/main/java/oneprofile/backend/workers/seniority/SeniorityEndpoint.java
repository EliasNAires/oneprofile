package oneprofile.backend.workers.seniority;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Derives the seniority of every vacancy of the engineering subset, replacing what the last run recorded. */
@RestController
class SeniorityEndpoint {

	private final SeniorityRun run;

	SeniorityEndpoint(SeniorityRun run) {
		this.run = run;
	}

	@PostMapping("/seniorities")
	SeniorityRun.Report read() {
		return this.run.readSeniorities();
	}

}
