package oneprofile.backend.workers.normalization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.BreakdownVacancy;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.TitleBreakdown;
import oneprofile.backend.storage.vacancyskill.VacancySkillStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Breaks the title of every vacancy of the engineering subset down into its normalized title, the
 * levels its guarded seniority words name and its management level (ADR-0024). It reads the skills
 * extraction found in each title, so it runs after extraction. It clears what the last run recorded
 * first, so a vacancy that has left the subset keeps no breakdown.
 */
@Component
public class TitleBreakdownRun {

	/** A title and a location weigh little, so a batch is as large as the cleaning's. */
	private static final int BATCH = 5000;

	private final TitleBreakdownRule breakdown;

	private final NormalizedVacancyStore normalized;

	private final VacancySkillStore skills;

	private final int batch;

	/**
	 * Breaks titles down in batches of five thousand.
	 * @param breakdown the rule that breaks a title down
	 * @param normalized where the subset is read and the breakdowns recorded
	 * @param skills where the skills found in each title are read
	 */
	@Autowired
	public TitleBreakdownRun(TitleBreakdownRule breakdown, NormalizedVacancyStore normalized,
			VacancySkillStore skills) {
		this(breakdown, normalized, skills, BATCH);
	}

	/**
	 * @param breakdown the rule that breaks a title down
	 * @param normalized where the subset is read and the breakdowns recorded
	 * @param skills where the skills found in each title are read
	 * @param batch how many vacancies are read and recorded at a time
	 */
	TitleBreakdownRun(TitleBreakdownRule breakdown, NormalizedVacancyStore normalized, VacancySkillStore skills,
			int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.breakdown = breakdown;
		this.normalized = normalized;
		this.skills = skills;
		this.batch = batch;
	}

	/**
	 * Clears what the last run recorded, then breaks down the title of every vacancy of the subset.
	 * @return what the run read
	 */
	public Report breakDownTitles() {
		this.normalized.clearTitleBreakdowns();
		int vacancies = 0;
		int withoutNormalizedTitle = 0;
		long after = 0;
		while (true) {
			List<BreakdownVacancy> read = this.normalized.breakdownScopeAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			Map<Long, Set<String>> titleSkills = this.skills
				.titleSkillIds(read.stream().map(BreakdownVacancy::normalizedVacancyId).toList());
			Map<Long, TitleBreakdown> brokenDown = new HashMap<>();
			for (BreakdownVacancy vacancy : read) {
				TitleBreakdown breakdown = this.breakdown.breakDown(vacancy.title(),
						(vacancy.location() != null) ? vacancy.location() : "",
						titleSkills.getOrDefault(vacancy.normalizedVacancyId(), Set.of()));
				if (breakdown.normalizedTitles().isEmpty()) {
					withoutNormalizedTitle++;
				}
				brokenDown.put(vacancy.vacancyId(), breakdown);
			}
			this.normalized.recordTitleBreakdowns(brokenDown);
			vacancies += read.size();
			after = read.getLast().vacancyId();
		}
		return new Report(vacancies, withoutNormalizedTitle);
	}

	/**
	 * What one run read.
	 *
	 * @param vacancies how many titles it broke down
	 * @param withoutNormalizedTitle how many of them were left with no normalized title, as a bare
	 * {@code Manager} is
	 */
	public record Report(int vacancies, int withoutNormalizedTitle) {
	}

}
