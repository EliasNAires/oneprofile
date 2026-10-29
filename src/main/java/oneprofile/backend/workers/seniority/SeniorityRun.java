package oneprofile.backend.workers.seniority;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.SeniorityVacancy;
import oneprofile.backend.storage.normalizedvacancy.VacancySeniority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gives every vacancy of the engineering subset its one seniority level and its required years (#8, PRD
 * §6.1), from the levels its title names and the experience requirements its description states. It reads
 * the title breakdown's levels, so it runs after the breakdown. It clears what the last run recorded first,
 * so a vacancy that has left the subset keeps no seniority.
 */
@Component
public class SeniorityRun {

	/** A vacancy's segments weigh as much as extraction reads, so a batch is as large as its. */
	private static final int BATCH = 1000;

	private final ExperienceRequirementRule requirements;

	private final SeniorityRule seniority;

	private final NormalizedVacancyStore normalized;

	private final int batch;

	/**
	 * Derives seniority in batches of a thousand.
	 * @param requirements the rule that reads a description's experience requirements
	 * @param seniority the rule that gives a vacancy its level
	 * @param normalized where the subset is read and the seniority recorded
	 */
	@Autowired
	public SeniorityRun(ExperienceRequirementRule requirements, SeniorityRule seniority,
			NormalizedVacancyStore normalized) {
		this(requirements, seniority, normalized, BATCH);
	}

	/**
	 * @param requirements the rule that reads a description's experience requirements
	 * @param seniority the rule that gives a vacancy its level
	 * @param normalized where the subset is read and the seniority recorded
	 * @param batch how many vacancies are read and recorded at a time
	 */
	SeniorityRun(ExperienceRequirementRule requirements, SeniorityRule seniority, NormalizedVacancyStore normalized,
			int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.requirements = requirements;
		this.seniority = seniority;
		this.normalized = normalized;
		this.batch = batch;
	}

	/**
	 * Clears what the last run recorded, then gives every vacancy of the subset its seniority.
	 * @return what the run derived
	 */
	public Report readSeniorities() {
		this.normalized.clearSeniorities();
		Tally tally = new Tally();
		long after = 0;
		while (true) {
			List<SeniorityVacancy> read = this.normalized.seniorityScopeAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			Map<Long, VacancySeniority> derived = new HashMap<>();
			for (SeniorityVacancy vacancy : read) {
				ExperienceReading reading = this.requirements.read(vacancy.segments());
				SeniorityLevelEnum named = this.seniority.namedSeniorityLevel(vacancy.titleSeniorities());
				SeniorityLevelEnum seniorityLevel = this.seniority.seniorityLevel(vacancy.titleSeniorities(), reading.requiredYears());
				if (seniorityLevel != null || reading.requiredYears() != null) {
					derived.put(vacancy.vacancyId(),
							new VacancySeniority(seniorityLevel, reading.requiredYears(), reading.requirements()));
				}
				tally.count(vacancy, named, reading.requiredYears(), seniorityLevel);
			}
			this.normalized.recordSeniorities(derived);
			after = read.getLast().vacancyId();
		}
		return tally.report();
	}

	/** What a run counts as it goes. */
	private final class Tally {

		private int vacancies;

		private int namedByTitle;

		private int withRequiredYears;

		private int withSeniorityLevel;

		private int numberedOnly;

		private int numberedResolved;

		private int titleAndYears;

		private int yearsAbove;

		private int yearsBelow;

		private final Map<SeniorityLevelEnum, Integer> bySeniorityLevel = new EnumMap<>(SeniorityLevelEnum.class);

		void count(SeniorityVacancy vacancy, SeniorityLevelEnum named, Integer requiredYears,
				SeniorityLevelEnum seniorityLevel) {
			this.vacancies++;
			if (named != null) {
				this.namedByTitle++;
			}
			if (requiredYears != null) {
				this.withRequiredYears++;
			}
			if (seniorityLevel != null) {
				this.withSeniorityLevel++;
				this.bySeniorityLevel.merge(seniorityLevel, 1, Integer::sum);
			}
			if (SeniorityRun.this.seniority.namesOnlyNumberedLevels(vacancy.titleSeniorities())) {
				this.numberedOnly++;
				if (requiredYears != null) {
					this.numberedResolved++;
				}
			}
			if (named != null && requiredYears != null) {
				this.titleAndYears++;
				int compared = SeniorityRun.this.seniority.seniorityLevelOf(requiredYears).compareTo(named);
				if (compared > 0) {
					this.yearsAbove++;
				}
				else if (compared < 0) {
					this.yearsBelow++;
				}
			}
		}

		Report report() {
			return new Report(this.vacancies, this.namedByTitle, this.withRequiredYears, this.withSeniorityLevel,
					this.numberedOnly, this.numberedResolved,
					new Disagreement(this.titleAndYears, this.yearsAbove, this.yearsBelow), this.bySeniorityLevel);
		}

	}

	/**
	 * What one run derived.
	 *
	 * @param vacancies how many vacancies of the subset it read
	 * @param namedByTitle how many of them a level word in the title places: the coverage the title gives
	 * alone
	 * @param withRequiredYears how many of them have required years
	 * @param withSeniorityLevel how many of them it gave a level: the coverage after the years
	 * @param numberedOnly how many titles name a numbered level and no level on the ladder
	 * @param numberedResolved how many of those the required years placed
	 * @param disagreement how often the title's level and the required years' level differ
	 * @param bySeniorityLevel how many vacancies it gave each level
	 */
	public record Report(int vacancies, int namedByTitle, int withRequiredYears, int withSeniorityLevel, int numberedOnly,
			int numberedResolved, Disagreement disagreement, Map<SeniorityLevelEnum, Integer> bySeniorityLevel) {

		public Report {
			bySeniorityLevel = Map.copyOf(bySeniorityLevel);
		}

	}

	/**
	 * Where a title names a level and the description states required years, whether the years place it
	 * at the same level. The title's level is the one kept.
	 *
	 * @param both how many vacancies have both
	 * @param yearsAbove how many of them the years place above the title's level
	 * @param yearsBelow how many of them the years place below it
	 */
	public record Disagreement(int both, int yearsAbove, int yearsBelow) {
	}

}
