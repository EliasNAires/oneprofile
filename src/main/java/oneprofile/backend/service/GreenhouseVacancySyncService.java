package oneprofile.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import oneprofile.backend.client.GreenhouseBoardClient;
import oneprofile.backend.client.GreenhouseBoardClient.BoardJob;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.Company;
import oneprofile.backend.model.Vacancy;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.repository.VacancyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Brings the openings of one Greenhouse company into the database. The table mirrors
 * the board: what the board changed is overwritten, and what the board no longer has
 * is removed.
 */
@Service
public class GreenhouseVacancySyncService {

	private final GreenhouseBoardClient boardClient;

	private final CompanyRepository companyRepository;

	private final VacancyRepository vacancyRepository;

	public GreenhouseVacancySyncService(GreenhouseBoardClient boardClient, CompanyRepository companyRepository,
			VacancyRepository vacancyRepository) {
		this.boardClient = boardClient;
		this.companyRepository = companyRepository;
		this.vacancyRepository = vacancyRepository;
	}

	/**
	 * Empty when the slug is not a company we know about. One board takes seconds, so
	 * the whole thing fits in a single transaction.
	 */
	@Transactional
	public Optional<SyncResult> syncCompany(String slug) {
		return this.companyRepository.findByAtsAndSlug(Ats.GREENHOUSE, slug).map(this::sync);
	}

	private SyncResult sync(Company company) {
		// Whatever is left in the map once every job of the board was taken out of it is
		// exactly what the board no longer has.
		Map<Long, Vacancy> stored = new HashMap<>();
		for (Vacancy vacancy : this.vacancyRepository.findByCompany(company)) {
			stored.put(vacancy.getExternalId(), vacancy);
		}

		List<BoardJob> jobs = this.boardClient.jobs(company.getSlug());
		int inserted = 0;
		int updated = 0;
		for (BoardJob job : jobs) {
			Vacancy vacancy = stored.remove(job.externalId());
			if (vacancy == null) {
				vacancy = new Vacancy(company, job.externalId());
				inserted++;
			}
			else {
				updated++;
			}
			vacancy.describe(job.title(), job.location(), job.department(), job.description(), job.url(),
					job.language(), job.payMinCents(), job.payMaxCents(), job.payCurrency(), job.payTitle(),
					job.firstPublished(), job.updatedAt());
			this.vacancyRepository.save(vacancy);
		}

		this.vacancyRepository.deleteAll(stored.values());

		return new SyncResult(jobs.size(), inserted, updated, stored.size());
	}

	public record SyncResult(int fetched, int inserted, int updated, int deleted) {
	}
}
