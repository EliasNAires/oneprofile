package oneprofile.backend.workers.cleaning;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.CleanedTitle;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.vacancy.VacancyStore;
import oneprofile.backend.storage.vacancy.VacancyTitle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Cleans the title of every vacancy in the corpus. Over the whole of it, not over a subset: a clean
 * title is what classification reads, and classification is what decides which vacancies are in
 * scope, so nothing has been ruled out yet.
 * <p>
 * The run is walked in batches and each batch is its own transaction, because the corpus is far too
 * large to hold one. A run that dies half way through leaves the vacancies it reached cleaned and
 * is simply started again: the rules are pure, so a second run over the same corpus writes exactly
 * what the first did.
 * <p>
 * The run also counts what each rule collapses on its own, applying it to the raw title with the
 * other two out of the way. That count is the whole measurement of the step — a rule earns its
 * place by collapsing distinct titles — and it is free here, where every title is read anyway.
 */
@Component
public class CorpusCleaningRun {

	/** Large enough that the corpus is walked in a couple of hundred transactions. */
	private static final int BATCH = 1000;

	private final TitleCleaningRule cleaning;

	private final VacancyStore vacancies;

	private final NormalizedVacancyStore normalized;

	private final int batch;

	/**
	 * Cleans the corpus in batches of a thousand.
	 * @param cleaning the rules to clean each title with
	 * @param vacancies the corpus to read the titles from
	 * @param normalized where the cleaned titles are recorded
	 */
	@Autowired
	public CorpusCleaningRun(TitleCleaningRule cleaning, VacancyStore vacancies, NormalizedVacancyStore normalized) {
		this(cleaning, vacancies, normalized, BATCH);
	}

	/**
	 * @param cleaning the rules to clean each title with
	 * @param vacancies the corpus to read the titles from
	 * @param normalized where the cleaned titles are recorded
	 * @param batch how many vacancies are read and recorded at a time
	 */
	CorpusCleaningRun(TitleCleaningRule cleaning, VacancyStore vacancies, NormalizedVacancyStore normalized, int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.cleaning = cleaning;
		this.vacancies = vacancies;
		this.normalized = normalized;
		this.batch = batch;
	}

	/**
	 * Cleans the title of every vacancy held.
	 * @return what the run cleaned, and what each rule collapsed
	 */
	public Report cleanAll() {
		Set<String> titles = new HashSet<>();
		Set<String> evenlySpaced = new HashSet<>();
		Set<String> cleaned = new HashSet<>();
		Set<String> withoutGenderMarkers = new HashSet<>();
		Set<String> whitelisted = new HashSet<>();
		Set<String> withoutSeniorityWords = new HashSet<>();
		int recorded = 0;
		long after = 0;
		while (true) {
			List<VacancyTitle> read = this.vacancies.titlesAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			Map<Long, CleanedTitle> cleanedTitles = new LinkedHashMap<>(read.size());
			for (VacancyTitle vacancy : read) {
				String title = vacancy.title();
				CleanedTitle cleanedTitle = this.cleaning.clean(title);
				titles.add(title);
				evenlySpaced.add(this.cleaning.evenlySpaced(title));
				cleaned.add(cleanedTitle.title());
				withoutGenderMarkers.add(this.cleaning.withoutGenderMarkers(title));
				whitelisted.add(this.cleaning.whitelisted(title));
				withoutSeniorityWords.add(this.cleaning.withoutSeniorityWords(title).title());
				cleanedTitles.put(vacancy.id(), cleanedTitle);
				after = vacancy.id();
			}
			recorded += this.normalized.recordCleanedTitles(cleanedTitles);
		}
		return new Report(recorded, titles.size(), cleaned.size(), titles.size() - evenlySpaced.size(),
				evenlySpaced.size() - withoutGenderMarkers.size(), evenlySpaced.size() - whitelisted.size(),
				evenlySpaced.size() - withoutSeniorityWords.size());
	}

	/**
	 * What one run cleaned. The three collapse counts are each rule measured on its own against the
	 * raw titles, which is what makes them comparable with one another; cleaning them together
	 * collapses more than their sum, because a rule reaches titles the ones before it brought
	 * together.
	 *
	 * @param vacancies how many vacancies were cleaned
	 * @param distinctTitles how many distinct titles they were written under
	 * @param distinctCleanedTitles how many distinct titles are left once all three rules have run
	 * @param collapsedBySpacing how many distinct titles differed from another only in their spacing
	 * @param collapsedByGenderMarkers how many distinct titles removing gender markers alone collapses
	 * @param collapsedByWhitelist how many distinct titles the character whitelist alone collapses,
	 * over and above the spacing it makes even
	 * @param collapsedBySeniorityWords how many distinct titles removing the seniority words alone
	 * collapses
	 */
	public record Report(int vacancies, int distinctTitles, int distinctCleanedTitles, int collapsedBySpacing,
			int collapsedByGenderMarkers, int collapsedByWhitelist, int collapsedBySeniorityWords) {
	}

}
