package oneprofile.backend.storage.vacancyskill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the skills extraction found in each vacancy (ADR-0018). After a run the table holds what that
 * run found and nothing else: a run clears it, then records each batch it reads, so a fix to the
 * taxonomy, or a vacancy that has left extraction's scope, leaves no stale row behind.
 */
@Component
public class VacancySkillStore {

	private final VacancySkillRepository repository;

	public VacancySkillStore(VacancySkillRepository repository) {
		this.repository = repository;
	}

	/**
	 * Lets go of every skill a run recorded before, so a new run starts from nothing.
	 * @return how many rows were let go of
	 */
	@Transactional
	public int clear() {
		return this.repository.deleteEveryRow();
	}

	/**
	 * Records what extraction found in a batch of vacancies, one row per vacancy and skill.
	 * @param found the skills found in each vacancy, by the id of its derived facts
	 * @return how many rows this recorded
	 */
	@Transactional
	public int record(Map<Long, List<FoundSkill>> found) {
		List<VacancySkillEntity> rows = new ArrayList<>();
		found.forEach((normalizedVacancyId, skills) -> skills
			.forEach((skill) -> rows.add(new VacancySkillEntity(normalizedVacancyId, skill))));
		this.repository.saveAll(rows);
		return rows.size();
	}

}
