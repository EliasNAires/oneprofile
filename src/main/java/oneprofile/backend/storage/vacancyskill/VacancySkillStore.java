package oneprofile.backend.storage.vacancyskill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	 * Reads where the last run found each skill in some vacancies, for the body pass to read with their
	 * segments (ADR-0018).
	 * @param normalizedVacancyIds the normalized vacancies
	 * @return by the id of each normalized vacancy, the indexes of the segments each skill was found
	 * in, by skill id; a vacancy with no skill found is left out
	 */
	@Transactional(readOnly = true)
	public Map<Long, Map<String, List<Integer>>> segmentsFoundIn(Collection<Long> normalizedVacancyIds) {
		Map<Long, Map<String, List<Integer>>> found = new HashMap<>();
		for (VacancySkillEntity row : this.repository.findByNormalizedVacancyIdIn(normalizedVacancyIds)) {
			found.computeIfAbsent(row.normalizedVacancyId(), (vacancy) -> new HashMap<>())
				.put(row.skillId(), row.segments());
		}
		return found;
	}

	/**
	 * Reads the skills the last run found in the titles of some vacancies, for the title breakdown to
	 * take out of them.
	 * @param normalizedVacancyIds the normalized vacancies
	 * @return by the id of each normalized vacancy, the ids of the skills its title names; a vacancy
	 * whose title names none is left out
	 */
	@Transactional(readOnly = true)
	public Map<Long, Set<String>> titleSkillIds(Collection<Long> normalizedVacancyIds) {
		Map<Long, Set<String>> named = new HashMap<>();
		for (VacancySkillEntity row : this.repository.findByNormalizedVacancyIdInAndInTitleTrue(normalizedVacancyIds)) {
			named.computeIfAbsent(row.normalizedVacancyId(), (vacancy) -> new HashSet<>()).add(row.skillId());
		}
		return named;
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
