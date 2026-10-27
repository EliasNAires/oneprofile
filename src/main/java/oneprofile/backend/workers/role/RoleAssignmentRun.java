package oneprofile.backend.workers.role;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.RoleVacancy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gives every vacancy of the engineering subset the role of each of its normalized titles, by the role
 * rule (ADR-0024). It reads the normalized titles the title breakdown wrote, so it runs after the
 * breakdown, and again after any change to the role list. It clears what the last run recorded first,
 * so a vacancy that has left the subset keeps no role.
 */
@Component
public class RoleAssignmentRun {

	/** A vacancy's normalized titles weigh little, so a batch is as large as the breakdown's. */
	private static final int BATCH = 5000;

	private final RoleRule rule;

	private final NormalizedVacancyStore normalized;

	private final int batch;

	/**
	 * Assigns roles in batches of five thousand.
	 * @param rule the rule that gives a normalized title its role
	 * @param normalized where the subset is read and the roles recorded
	 */
	@Autowired
	public RoleAssignmentRun(RoleRule rule, NormalizedVacancyStore normalized) {
		this(rule, normalized, BATCH);
	}

	/**
	 * @param rule the rule that gives a normalized title its role
	 * @param normalized where the subset is read and the roles recorded
	 * @param batch how many vacancies are read and recorded at a time
	 */
	RoleAssignmentRun(RoleRule rule, NormalizedVacancyStore normalized, int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.rule = rule;
		this.normalized = normalized;
		this.batch = batch;
	}

	/**
	 * Clears what the last run recorded, then gives every vacancy of the subset its roles.
	 * @return what the run read
	 */
	public Report assignRoles() {
		this.normalized.clearRoles();
		int vacancies = 0;
		int withoutRole = 0;
		long after = 0;
		while (true) {
			List<RoleVacancy> read = this.normalized.roleScopeAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			Map<Long, List<String>> assigned = new HashMap<>();
			for (RoleVacancy vacancy : read) {
				List<String> roles = this.rule.roles(vacancy.normalizedTitles());
				if (roles.isEmpty()) {
					withoutRole++;
				}
				assigned.put(vacancy.vacancyId(), roles);
			}
			this.normalized.recordRoles(assigned);
			vacancies += read.size();
			after = read.getLast().vacancyId();
		}
		return new Report(vacancies, withoutRole);
	}

	/**
	 * What one run read.
	 *
	 * @param vacancies how many vacancies it gave roles to, or found none for
	 * @param withoutRole how many of them were left without a role: their normalized titles fit none, or
	 * they have none, as a bare {@code Manager}
	 */
	public record Report(int vacancies, int withoutRole) {
	}

}
