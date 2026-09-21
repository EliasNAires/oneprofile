package oneprofile.backend.normalizedvacancy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the facts the rules derive from the corpus. A pass over the corpus is re-runnable, so what
 * it records for a vacancy replaces what the last run recorded rather than adding a second row.
 */
public class NormalizedVacancies {

	private final NormalizedVacancyRepository repository;

	public NormalizedVacancies(NormalizedVacancyRepository repository) {
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
		List<NormalizedVacancy> recorded = new ArrayList<>(classified.size());
		for (NormalizedVacancy normalized : this.repository.findByVacancyIdIn(classified.keySet())) {
			normalized.classifiedAs(classified.get(normalized.vacancyId()));
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
		Map<Long, NormalizedVacancy> held = new HashMap<>();
		for (NormalizedVacancy normalized : this.repository.findByVacancyIdIn(cleaned.keySet())) {
			held.put(normalized.vacancyId(), normalized);
		}
		List<NormalizedVacancy> recorded = new ArrayList<>(cleaned.size());
		cleaned.forEach((vacancyId, title) -> {
			NormalizedVacancy normalized = held.computeIfAbsent(vacancyId, NormalizedVacancy::new);
			normalized.cleanedAs(title);
			recorded.add(normalized);
		});
		this.repository.saveAll(recorded);
		return recorded.size();
	}

}
