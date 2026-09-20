package oneprofile.backend.normalizedvacancy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
