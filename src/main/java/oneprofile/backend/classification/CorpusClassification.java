package oneprofile.backend.classification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.normalizedvacancy.Classification;
import oneprofile.backend.normalizedvacancy.ClassificationState;
import oneprofile.backend.normalizedvacancy.NormalizedTitle;
import oneprofile.backend.normalizedvacancy.NormalizedVacancies;
import oneprofile.backend.normalizedvacancy.UnknownReason;

/**
 * Decides of every cleaned title in the corpus whether it names an engineering role. It reads what
 * cleaning left rather than the titles the boards published, because the cleaned title is what the
 * criterion is written about.
 * <p>
 * The run is walked in batches and each batch is its own transaction, for the same reason cleaning
 * is: the corpus is far too large to hold one, and the rules are pure, so a run that dies half way
 * through is simply started again.
 * <p>
 * The run counts what it decided and, of what it did not, why. The split by reason is the whole
 * measurement of the step: only {@code unruled} is the rules' backlog, so it is the one share an
 * iteration of the loop is expected to move.
 */
public class CorpusClassification {

	private final TitleClassification classification;

	private final NormalizedVacancies normalized;

	private final int batch;

	/**
	 * @param classification the rules to decide each title with
	 * @param normalized where the cleaned titles are read and the states recorded
	 * @param batch how many vacancies are read and recorded at a time
	 */
	public CorpusClassification(TitleClassification classification, NormalizedVacancies normalized, int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.classification = classification;
		this.normalized = normalized;
		this.batch = batch;
	}

	/**
	 * Classifies every cleaned title held.
	 * @return what the run decided, and why it left the rest undecided
	 */
	public Report classifyAll() {
		Map<ClassificationState, Integer> states = new LinkedHashMap<>();
		Map<UnknownReason, Integer> reasons = new LinkedHashMap<>();
		int recorded = 0;
		long after = 0;
		while (true) {
			List<NormalizedTitle> read = this.normalized.cleanedTitlesAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			Map<Long, Classification> classified = new LinkedHashMap<>(read.size());
			for (NormalizedTitle title : read) {
				Classification decided = this.classification.classify(title.cleanedTitle());
				states.merge(decided.state(), 1, Integer::sum);
				if (decided.reason() != null) {
					reasons.merge(decided.reason(), 1, Integer::sum);
				}
				classified.put(title.vacancyId(), decided);
				after = title.vacancyId();
			}
			recorded += this.normalized.recordClassifications(classified);
		}
		return new Report(recorded, states.getOrDefault(ClassificationState.IN, 0),
				states.getOrDefault(ClassificationState.OUT, 0), states.getOrDefault(ClassificationState.UNKNOWN, 0),
				reasons.getOrDefault(UnknownReason.UNRULED, 0),
				reasons.getOrDefault(UnknownReason.DOMAIN_AMBIGUITY, 0),
				reasons.getOrDefault(UnknownReason.SCOPE_AMBIGUITY, 0));
	}

	/**
	 * What one run decided. The three reasons add up to the unknowns, and of them only
	 * {@code unruled} says the rules failed to reach a title rather than that the corpus is
	 * genuinely ambiguous.
	 *
	 * @param vacancies how many vacancies were classified
	 * @param in how many of them name an engineering role
	 * @param out how many of them do not, where the title says so
	 * @param unknown how many titles carried too little to decide
	 * @param unruled how many of those no rule reached
	 * @param domainAmbiguity how many of those named a known head and no domain
	 * @param scopeAmbiguity how many of those named a ruled phrase whose variants split
	 */
	public record Report(int vacancies, int in, int out, int unknown, int unruled, int domainAmbiguity,
			int scopeAmbiguity) {
	}

}
