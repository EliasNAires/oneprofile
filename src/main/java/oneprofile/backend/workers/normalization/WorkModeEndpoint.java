package oneprofile.backend.workers.normalization;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reads the work mode of every vacancy of the engineering subset, replacing what the last run recorded. */
@RestController
class WorkModeEndpoint {

	private final WorkModeRun run;

	WorkModeEndpoint(WorkModeRun run) {
		this.run = run;
	}

	@PostMapping("/work-modes")
	WorkModeRun.Report read() {
		return this.run.readWorkModes();
	}

}
