package oneprofile.backend.workers.role;

import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.rolenearness.RoleNearness;
import oneprofile.backend.storage.rolenearness.RoleNearnessStore;
import oneprofile.backend.storage.vacancyskill.SkillProfile;
import oneprofile.backend.storage.vacancyskill.VacancySkillStore;
import org.springframework.stereotype.Component;

/**
 * Computes how near each pair of roles is from the skills their vacancies ask for, and replaces what the
 * last run recorded (ADR-0024). It reads the roles the role assignment gave and the skills extraction
 * found, so it runs after both, again on each snapshot.
 */
@Component
public class RoleNearnessRun {

	private final RoleNearnessRule rule;

	private final VacancySkillStore skills;

	private final RoleNearnessStore nearness;

	/**
	 * @param rule the rule that says how near two roles are
	 * @param skills where the skills of the subset and of each role are counted
	 * @param nearness where the nearness of each pair is recorded
	 */
	public RoleNearnessRun(RoleNearnessRule rule, VacancySkillStore skills, RoleNearnessStore nearness) {
		this.rule = rule;
		this.skills = skills;
		this.nearness = nearness;
	}

	/**
	 * Computes the nearness of every pair of the roles the subset holds and records it.
	 * @return what the run computed
	 */
	public Report computeNearness() {
		Map<String, SkillProfile> roles = this.skills.roleProfiles();
		List<RoleNearness> pairs = this.rule.nearness(this.skills.subsetProfile(), roles);
		return new Report(roles.size(), this.nearness.replace(pairs));
	}

	/**
	 * What one run computed.
	 *
	 * @param roles how many roles the subset's vacancies hold
	 * @param pairs how many pairs of them it recorded a nearness for
	 */
	public record Report(int roles, int pairs) {
	}

}
