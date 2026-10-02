package oneprofile.backend.storage.normalizedvacancy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the facts the rules derive from the corpus. A pass over the corpus is re-runnable, so what
 * it records for a vacancy replaces what the last run recorded rather than adding a second row.
 */
@Component
public class NormalizedVacancyStore {

	private final NormalizedVacancyRepository repository;

	public NormalizedVacancyStore(NormalizedVacancyRepository repository) {
		this.repository = repository;
	}

	/**
	 * The cleaned titles held after one vacancy id, in id order. A pass that reads titles rather
	 * than bodies walks the corpus with this, batch by batch, resuming from the last id it read.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return their vacancy ids and cleaned titles, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<NormalizedTitle> cleanedTitlesAfter(long after, int batch) {
		return this.repository.cleanedTitlesAfter(after, Limit.of(batch));
	}

	/**
	 * Records what classification made of a batch of cleaned titles. A vacancy cleaning has not
	 * reached has nothing to classify, so it is not here and nothing is written for it.
	 * @param classified what the rules decided for each vacancy, by vacancy id
	 * @return how many vacancies this recorded a classification for
	 */
	@Transactional
	public int recordClassifications(Map<Long, Classification> classified) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(classified.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(classified.keySet())) {
			normalized.classifiedAs(classified.get(normalized.vacancyId()));
			recorded.add(normalized);
		}
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * The pile held after one vacancy id, in id order, each with its description: the vacancies the
	 * body pass reads, walked batch by batch like the titles are.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return the pile's vacancies, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<PileVacancy> pileAfter(long after, int batch) {
		return this.repository.pileAfter(Classification.PILE_REASONS, after, Limit.of(batch));
	}

	/**
	 * Puts the pile back to what the title stage left it, so the body pass can be run again without
	 * the title pass: every vacancy of it unknown, decided by the title, with its reason kept.
	 * @return how many vacancies the pile holds
	 */
	@Transactional
	public int resetPile() {
		return this.repository.resetPile(Classification.PILE_REASONS);
	}

	/**
	 * Records what the body decided of a batch of the pile. Only decided vacancies are given; one
	 * the body leaves unknown keeps what the title left it.
	 * @param decided what the body decided, {@code IN} or {@code OUT}, by vacancy id
	 * @return how many vacancies this recorded a decision for
	 */
	@Transactional
	public int recordBodyDecisions(Map<Long, ClassificationStateEnum> decided) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(decided.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(decided.keySet())) {
			normalized.decidedByBody(decided.get(normalized.vacancyId()));
			recorded.add(normalized);
		}
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * Records what cleaning made of a batch of titles. A batch is one transaction, because a pass
	 * over the corpus is too large to be one and is re-runnable anyway.
	 * @param cleaned the cleaned title of each vacancy, by vacancy id
	 * @return how many vacancies this recorded a cleaned title for
	 */
	@Transactional
	public int recordCleanedTitles(Map<Long, CleanedTitle> cleaned) {
		Map<Long, NormalizedVacancyEntity> held = new HashMap<>();
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(cleaned.keySet())) {
			held.put(normalized.vacancyId(), normalized);
		}
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(cleaned.size());
		cleaned.forEach((vacancyId, title) -> {
			NormalizedVacancyEntity normalized = held.computeIfAbsent(vacancyId, NormalizedVacancyEntity::new);
			normalized.cleanedAs(title);
			recorded.add(normalized);
		});
		this.repository.saveAll(recorded);
		return recorded.size();
	}

}
