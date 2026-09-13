package oneprofile.backend.service;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import oneprofile.backend.model.NormalizedVacancy;
import oneprofile.backend.model.Vacancy;
import oneprofile.backend.repository.NormalizedVacancyRepository;
import oneprofile.backend.repository.VacancyRepository;
import oneprofile.backend.util.SeniorityExtractor;
import oneprofile.backend.util.TitleCleaner;
import oneprofile.backend.util.WorkModeExtractor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the title extractors over the stored vacancies and keeps what they say in
 * {@code normalized_vacancy}. There is no network in between, so no pause is needed;
 * what is needed is not to bring every vacancy into memory at once.
 */
@Service
public class VacancyNormalizationService {

	private static final int PAGE_SIZE = 1000;

	private final VacancyRepository vacancyRepository;

	private final NormalizedVacancyRepository normalizedRepository;

	private final TransactionTemplate transaction;

	private final int pageSize;

	/** Marked so Spring picks this one: the other is there only for the test. */
	@Autowired
	public VacancyNormalizationService(VacancyRepository vacancyRepository,
			NormalizedVacancyRepository normalizedRepository, PlatformTransactionManager transactionManager) {
		this(vacancyRepository, normalizedRepository, transactionManager, PAGE_SIZE);
	}

	VacancyNormalizationService(VacancyRepository vacancyRepository, NormalizedVacancyRepository normalizedRepository,
			PlatformTransactionManager transactionManager, int pageSize) {
		this.vacancyRepository = vacancyRepository;
		this.normalizedRepository = normalizedRepository;
		this.transaction = new TransactionTemplate(transactionManager);
		this.pageSize = pageSize;
	}

	/** Every vacancy again, updating the rows already there: what to run after a rule changes. */
	public NormalizationResult normalizeAll() {
		return walk(this.vacancyRepository::findByIdGreaterThanOrderById);
	}

	/** Only the vacancies that have no row yet; the rows already there are left as they are. */
	public NormalizationResult normalizeMissing() {
		return walk(this.vacancyRepository::findNotNormalizedByIdGreaterThan);
	}

	/**
	 * Deliberately not transactional as a whole: each page gets its own short
	 * transaction, so a run cut short keeps what it already did.
	 */
	private NormalizationResult walk(BiFunction<Long, Limit, List<Vacancy>> nextPage) {
		int inserted = 0;
		int updated = 0;
		long after = 0;
		while (true) {
			long from = after;
			PageResult page = this.transaction.execute(status -> normalize(nextPage.apply(from, Limit.of(this.pageSize))));
			if (page.lastId() == null) {
				return new NormalizationResult(inserted, updated);
			}
			inserted += page.inserted();
			updated += page.updated();
			after = page.lastId();
		}
	}

	private PageResult normalize(List<Vacancy> vacancies) {
		if (vacancies.isEmpty()) {
			return new PageResult(0, 0, null);
		}
		Map<Long, NormalizedVacancy> stored = this.normalizedRepository.findByVacancyIn(vacancies)
			.stream()
			.collect(Collectors.toMap(normalized -> normalized.getVacancy().getId(), Function.identity()));

		int inserted = 0;
		int updated = 0;
		for (Vacancy vacancy : vacancies) {
			NormalizedVacancy normalized = stored.get(vacancy.getId());
			if (normalized == null) {
				normalized = new NormalizedVacancy(vacancy);
				stored.put(vacancy.getId(), normalized);
				inserted++;
			}
			else {
				updated++;
			}
			String clean = TitleCleaner.clean(vacancy.getTitle());
			WorkModeExtractor.Extracted mode = WorkModeExtractor.extract(clean, vacancy.getLocation());
			SeniorityExtractor.Extracted level = SeniorityExtractor.extract(mode.title());
			normalized.describe(level.title(), level.seniority(), mode.mode());
		}
		this.normalizedRepository.saveAll(stored.values());
		return new PageResult(inserted, updated, vacancies.getLast().getId());
	}

	private record PageResult(int inserted, int updated, Long lastId) {
	}

	public record NormalizationResult(int inserted, int updated) {
	}
}
