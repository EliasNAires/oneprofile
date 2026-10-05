package oneprofile.backend.workers.classification;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.PileVacancy;
import oneprofile.backend.workers.cleaning.DescriptionCleaningRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Decides from its description what the title left unknown for a corpus reason: the pile. It reads
 * the cleaned description, the same text the labeller it is scored against reads (ADR-0012).
 * <p>
 * It can be run again on its own: it first puts the pile back to what the title stage left it, so
 * a rule change is measured by running it again, without the title pass. What it decides it
 * records with the body's signal and the title's reason; what it leaves unknown keeps the title's.
 */
@Component
public class BodyClassificationRun {

	/** A description averages about six thousand characters, so a batch holds a few megabytes. */
	private static final int BATCH = 1000;

	private final BodyClassificationRule classification;

	private final DescriptionCleaningRule cleaning;

	private final NormalizedVacancyStore normalized;

	private final int batch;

	/**
	 * Classifies the pile in batches of a thousand.
	 * @param classification the rules to decide each vacancy with
	 * @param cleaning what cleans each description before the rules read it
	 * @param normalized where the pile is read and the decisions recorded
	 */
	@Autowired
	public BodyClassificationRun(BodyClassificationRule classification, DescriptionCleaningRule cleaning,
			NormalizedVacancyStore normalized) {
		this(classification, cleaning, normalized, BATCH);
	}

	/**
	 * @param classification the rules to decide each vacancy with
	 * @param cleaning what cleans each description before the rules read it
	 * @param normalized where the pile is read and the decisions recorded
	 * @param batch how many vacancies are read and recorded at a time
	 */
	BodyClassificationRun(BodyClassificationRule classification, DescriptionCleaningRule cleaning,
			NormalizedVacancyStore normalized, int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.classification = classification;
		this.cleaning = cleaning;
		this.normalized = normalized;
		this.batch = batch;
	}

	/**
	 * Puts the pile back to what the title left it, then classifies every vacancy of it.
	 * @return what the run decided
	 */
	public Report classifyPile() {
		this.normalized.resetPile();
		Map<ClassificationStateEnum, Integer> states = new EnumMap<>(ClassificationStateEnum.class);
		int pile = 0;
		long after = 0;
		while (true) {
			List<PileVacancy> read = this.normalized.pileAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			List<ClassificationStateEnum> classified = read.parallelStream().map(this::classify).toList();
			Map<Long, ClassificationStateEnum> decided = new LinkedHashMap<>();
			for (int i = 0; i < read.size(); i++) {
				ClassificationStateEnum state = classified.get(i);
				states.merge(state, 1, Integer::sum);
				if (state != ClassificationStateEnum.UNKNOWN) {
					decided.put(read.get(i).vacancyId(), state);
				}
			}
			pile += read.size();
			after = read.getLast().vacancyId();
			if (!decided.isEmpty()) {
				this.normalized.recordBodyDecisions(decided);
			}
		}
		return new Report(pile, states.getOrDefault(ClassificationStateEnum.IN, 0),
				states.getOrDefault(ClassificationStateEnum.OUT, 0),
				states.getOrDefault(ClassificationStateEnum.UNKNOWN, 0));
	}

	/**
	 * Decides one vacancy from its cleaned description. The vacancies of a batch are decided in
	 * parallel, since the rules read each on its own and are what a pass spends its time on.
	 */
	private ClassificationStateEnum classify(PileVacancy vacancy) {
		String description = (vacancy.description() != null) ? this.cleaning.clean(vacancy.description()) : "";
		return this.classification.classify(vacancy.cleanedTitle(), vacancy.titleReason(), description);
	}

	/**
	 * What one run decided of the pile.
	 *
	 * @param pile how many vacancies the pile holds
	 * @param in how many of them the body decided name an engineering role
	 * @param out how many it decided do not
	 * @param unknown how many it left as the title left them
	 */
	public record Report(int pile, int in, int out, int unknown) {
	}

}
