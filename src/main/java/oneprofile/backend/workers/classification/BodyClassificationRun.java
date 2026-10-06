package oneprofile.backend.workers.classification;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.PileVacancy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Decides from its description what the title left unknown for a corpus reason: the pile. It reads
 * the segments cleaning stored, the same segments the labeller it is scored against reads one a line
 * (ADR-0012), and cleans nothing itself.
 * <p>
 * It can be run again on its own: it first puts the pile back to what the title stage left it, so
 * a rule change is measured by running it again, without the title pass. What it decides it
 * records with the body's signal and the title's reason; what it leaves unknown keeps the title's.
 * A vacancy in a language other than English or Spanish it does not read: that stays unknown, with
 * the reason {@code unsupported_language}.
 */
@Component
public class BodyClassificationRun {

	/** A description averages about fifty segments, so a batch holds a few megabytes. */
	private static final int BATCH = 1000;

	private final BodyClassificationRule classification;

	private final NormalizedVacancyStore normalized;

	private final int batch;

	/**
	 * Classifies the pile in batches of a thousand.
	 * @param classification the rules to decide each vacancy with
	 * @param normalized where the pile is read and the decisions recorded
	 */
	@Autowired
	public BodyClassificationRun(BodyClassificationRule classification, NormalizedVacancyStore normalized) {
		this(classification, normalized, BATCH);
	}

	/**
	 * @param classification the rules to decide each vacancy with
	 * @param normalized where the pile is read and the decisions recorded
	 * @param batch how many vacancies are read and recorded at a time
	 */
	BodyClassificationRun(BodyClassificationRule classification, NormalizedVacancyStore normalized, int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.classification = classification;
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
		int unsupportedLanguage = 0;
		long after = 0;
		while (true) {
			List<PileVacancy> read = this.normalized.pileAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			Set<Long> skipped = new LinkedHashSet<>();
			List<PileVacancy> readable = new ArrayList<>(read.size());
			for (PileVacancy vacancy : read) {
				if (vacancy.inSupportedLanguage()) {
					readable.add(vacancy);
				}
				else {
					skipped.add(vacancy.vacancyId());
				}
			}
			List<ClassificationStateEnum> classified = readable.parallelStream().map(this::classify).toList();
			Map<Long, ClassificationStateEnum> decided = new LinkedHashMap<>();
			for (int i = 0; i < readable.size(); i++) {
				ClassificationStateEnum state = classified.get(i);
				states.merge(state, 1, Integer::sum);
				if (state != ClassificationStateEnum.UNKNOWN) {
					decided.put(readable.get(i).vacancyId(), state);
				}
			}
			pile += read.size();
			unsupportedLanguage += skipped.size();
			after = read.getLast().vacancyId();
			if (!decided.isEmpty()) {
				this.normalized.recordBodyDecisions(decided);
			}
			if (!skipped.isEmpty()) {
				this.normalized.recordUnsupportedLanguage(skipped);
			}
		}
		return new Report(pile, states.getOrDefault(ClassificationStateEnum.IN, 0),
				states.getOrDefault(ClassificationStateEnum.OUT, 0),
				states.getOrDefault(ClassificationStateEnum.UNKNOWN, 0), unsupportedLanguage);
	}

	/**
	 * Decides one vacancy from its segments. The vacancies of a batch are decided in parallel, since
	 * the rules read each on its own and are what a pass spends its time on.
	 */
	private ClassificationStateEnum classify(PileVacancy vacancy) {
		return this.classification.classify(vacancy.cleanedTitle(), vacancy.titleReason(), vacancy.segments());
	}

	/**
	 * What one run decided of the pile.
	 *
	 * @param pile how many vacancies the pile holds
	 * @param in how many of them the body decided name an engineering role
	 * @param out how many it decided do not
	 * @param unknown how many it read and left as the title left them
	 * @param unsupportedLanguage how many it did not read, since they are in a language other than
	 * English or Spanish
	 */
	public record Report(int pile, int in, int out, int unknown, int unsupportedLanguage) {
	}

}
