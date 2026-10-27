package oneprofile.backend.workers.normalization;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.storage.normalizedvacancy.WorkModeVacancy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gives every vacancy of the engineering subset the work mode it declares, by the work-mode rule (#86).
 * It reads what the ATS published and nothing the other passes derive, so it can run any time after
 * classification. It clears what the last run recorded first, so a vacancy that has left the subset
 * keeps no work mode.
 */
@Component
public class WorkModeRun {

	/** A vacancy's title, location and field weigh little, so a batch is as large as the breakdown's. */
	private static final int BATCH = 5000;

	private final WorkModeRule rule;

	private final NormalizedVacancyStore normalized;

	private final int batch;

	/**
	 * Reads work modes in batches of five thousand.
	 * @param rule the rule that reads a vacancy's work mode
	 * @param normalized where the subset is read and the work modes recorded
	 */
	@Autowired
	public WorkModeRun(WorkModeRule rule, NormalizedVacancyStore normalized) {
		this(rule, normalized, BATCH);
	}

	/**
	 * @param rule the rule that reads a vacancy's work mode
	 * @param normalized where the subset is read and the work modes recorded
	 * @param batch how many vacancies are read and recorded at a time
	 */
	WorkModeRun(WorkModeRule rule, NormalizedVacancyStore normalized, int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.rule = rule;
		this.normalized = normalized;
		this.batch = batch;
	}

	/**
	 * Clears what the last run recorded, then gives every vacancy of the subset the work mode it declares.
	 * @return what the run read
	 */
	public Report readWorkModes() {
		this.normalized.clearWorkModes();
		int vacancies = 0;
		Map<AtsEnum, Map<WorkModeEvidenceEnum, Integer>> decidedBy = new EnumMap<>(AtsEnum.class);
		long after = 0;
		while (true) {
			List<WorkModeVacancy> read = this.normalized.workModeScopeAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			Map<Long, WorkModeEnum> declared = new HashMap<>();
			for (WorkModeVacancy vacancy : read) {
				WorkModeReading reading = this.rule.read(vacancy.location(), vacancy.workModeField(), vacancy.title());
				if (reading.mode() != null) {
					declared.put(vacancy.vacancyId(), reading.mode());
				}
				decidedBy.computeIfAbsent(vacancy.ats(), (ats) -> new EnumMap<>(WorkModeEvidenceEnum.class))
					.merge(reading.evidence(), 1, Integer::sum);
			}
			this.normalized.recordWorkModes(declared);
			vacancies += read.size();
			after = read.getLast().vacancyId();
		}
		return new Report(vacancies, decidedBy);
	}

	/**
	 * What one run read.
	 *
	 * @param vacancies how many vacancies of the subset it read
	 * @param decidedBy how many of them each step of the order of evidence decided, by ATS, with those no
	 * step decided under {@link WorkModeEvidenceEnum#NONE}
	 */
	public record Report(int vacancies, Map<AtsEnum, Map<WorkModeEvidenceEnum, Integer>> decidedBy) {

		public Report {
			decidedBy = Map.copyOf(decidedBy);
		}

	}

}
