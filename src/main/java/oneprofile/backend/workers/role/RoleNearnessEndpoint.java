package oneprofile.backend.workers.role;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Computes how near each pair of roles is, replacing what the last run recorded. */
@RestController
class RoleNearnessEndpoint {

	private final RoleNearnessRun nearness;

	RoleNearnessEndpoint(RoleNearnessRun nearness) {
		this.nearness = nearness;
	}

	@PostMapping("/role-nearness")
	RoleNearnessRun.Report compute() {
		return this.nearness.computeNearness();
	}

}
