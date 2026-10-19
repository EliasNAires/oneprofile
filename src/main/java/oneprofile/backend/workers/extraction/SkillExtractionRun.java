package oneprofile.backend.workers.extraction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import oneprofile.backend.storage.normalizedvacancy.ExtractionVacancy;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.vacancyskill.FoundSkill;
import oneprofile.backend.storage.vacancyskill.VacancySkillStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Finds the skills of every vacancy in extraction's scope, the title's in and the pile (ADR-0017),
 * and records them for the body pass to read. It clears what the last run found first, so after a
 * run the table holds what that run found and nothing else (ADR-0018).
 */
@Component
public class SkillExtractionRun {

	/** A description averages about fifty segments, so a batch holds a few megabytes. */
	private static final int BATCH = 1000;

	private final SkillExtractionRule extraction;

	private final NormalizedVacancyStore normalized;

	private final VacancySkillStore skills;

	private final int batch;

	/**
	 * Extracts in batches of a thousand.
	 * @param extraction the rule that finds a vacancy's skills
	 * @param normalized where the scope is read
	 * @param skills where what is found is recorded
	 */
	@Autowired
	public SkillExtractionRun(SkillExtractionRule extraction, NormalizedVacancyStore normalized,
			VacancySkillStore skills) {
		this(extraction, normalized, skills, BATCH);
	}

	/**
	 * @param extraction the rule that finds a vacancy's skills
	 * @param normalized where the scope is read
	 * @param skills where what is found is recorded
	 * @param batch how many vacancies are read and recorded at a time
	 */
	SkillExtractionRun(SkillExtractionRule extraction, NormalizedVacancyStore normalized, VacancySkillStore skills,
			int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.extraction = extraction;
		this.normalized = normalized;
		this.skills = skills;
		this.batch = batch;
	}

	/**
	 * Clears what the last run found, then extracts the skills of every vacancy in scope.
	 * @return what the run found
	 */
	public Report extractSkills() {
		this.skills.clear();
		SortedMap<Integer, Integer> skillsPerVacancy = new TreeMap<>();
		int vacancies = 0;
		int rows = 0;
		int rowsFromContextKeys = 0;
		long after = 0;
		while (true) {
			List<ExtractionVacancy> read = this.normalized.extractionScopeAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			List<List<FoundSkill>> extracted = read.parallelStream()
				.map((vacancy) -> this.extraction.extract(vacancy.cleanedTitle(), vacancy.segments()))
				.toList();
			Map<Long, List<FoundSkill>> found = new LinkedHashMap<>();
			for (int i = 0; i < read.size(); i++) {
				List<FoundSkill> ofVacancy = extracted.get(i);
				skillsPerVacancy.merge(ofVacancy.size(), 1, Integer::sum);
				rows += ofVacancy.size();
				rowsFromContextKeys += (int) ofVacancy.stream().filter(FoundSkill::fromContextKeys).count();
				if (!ofVacancy.isEmpty()) {
					found.put(read.get(i).normalizedVacancyId(), ofVacancy);
				}
			}
			vacancies += read.size();
			after = read.getLast().vacancyId();
			if (!found.isEmpty()) {
				this.skills.record(found);
			}
		}
		return new Report(vacancies, rows, rowsFromContextKeys, skillsPerVacancy);
	}

	/**
	 * What one run found.
	 *
	 * @param vacancies how many vacancies it read
	 * @param rows how many (vacancy, skill) pairs it found
	 * @param rowsFromContextKeys how many of those pairs only context keys found
	 * @param skillsPerVacancy how many vacancies it found each number of skills in, by that number
	 */
	public record Report(int vacancies, int rows, int rowsFromContextKeys, SortedMap<Integer, Integer> skillsPerVacancy) {
	}

}
