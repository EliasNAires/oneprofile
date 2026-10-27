package oneprofile.backend.workers.role;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gives every vacancy of the engineering subset its roles, replacing what the last run recorded. */
@RestController
class RoleAssignmentEndpoint {

	private final RoleAssignmentRun assignment;

	RoleAssignmentEndpoint(RoleAssignmentRun assignment) {
		this.assignment = assignment;
	}

	@PostMapping("/roles")
	RoleAssignmentRun.Report assign() {
		return this.assignment.assignRoles();
	}

}
