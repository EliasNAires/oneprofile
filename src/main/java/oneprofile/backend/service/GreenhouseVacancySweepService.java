package oneprofile.backend.service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import oneprofile.backend.model.Ats;
import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.service.GreenhouseVacancySyncService.SyncResult;
import oneprofile.backend.util.ProgressLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Walks every Greenhouse company whose board answered with openings and brings them
 * all in. Only the active ones are asked: the rest already said they have nothing.
 */
@Service
public class GreenhouseVacancySweepService {

	private static final Logger logger = LoggerFactory.getLogger(GreenhouseVacancySweepService.class);

	/** Longer than the probe's 200 ms: here every answer weighs megabytes, not kilobytes. */
	private static final Duration PAUSE_BETWEEN_COMPANIES = Duration.ofMillis(500);

	/** ~1h50 at today's volume: often enough to follow along, rare enough not to flood. */
	private static final int PROGRESS_INTERVAL = 500;

	private final CompanyRepository companyRepository;

	private final GreenhouseVacancySyncService syncService;

	private final Duration pauseBetweenCompanies;

	private final int progressInterval;

	private final Clock clock;

	/** Marked so Spring picks this one: the other is there only for the test. */
	@Autowired
	public GreenhouseVacancySweepService(CompanyRepository companyRepository,
			GreenhouseVacancySyncService syncService) {
		this(companyRepository, syncService, PAUSE_BETWEEN_COMPANIES, PROGRESS_INTERVAL, Clock.systemUTC());
	}

	GreenhouseVacancySweepService(CompanyRepository companyRepository, GreenhouseVacancySyncService syncService,
			Duration pauseBetweenCompanies) {
		this(companyRepository, syncService, pauseBetweenCompanies, PROGRESS_INTERVAL, Clock.systemUTC());
	}

	GreenhouseVacancySweepService(CompanyRepository companyRepository, GreenhouseVacancySyncService syncService,
			Duration pauseBetweenCompanies, int progressInterval, Clock clock) {
		this.companyRepository = companyRepository;
		this.syncService = syncService;
		this.pauseBetweenCompanies = pauseBetweenCompanies;
		this.progressInterval = progressInterval;
		this.clock = clock;
	}

	/**
	 * Deliberately not transactional: a run takes hours, and one long transaction would
	 * hold a connection the whole time and lose everything if the run is cut short. Each
	 * company gets its own short transaction inside {@code syncCompany}, which is why
	 * that lives in another bean —a call within the same one would skip the proxy—.
	 */
	public SweepResult syncAllActive() {
		List<String> slugs = this.companyRepository.findSlugsByAtsAndBoardStatus(Ats.GREENHOUSE, BoardStatus.ACTIVE);
		int fetched = 0;
		int inserted = 0;
		int updated = 0;
		int deleted = 0;
		int failed = 0;
		ProgressLog progress = new ProgressLog("Greenhouse vacancy sweep", "companies", slugs.size(),
				this.progressInterval, this.clock);

		boolean first = true;
		for (String slug : slugs) {
			if (!first) {
				pause();
			}
			first = false;
			boolean itemFailed = false;
			try {
				SyncResult result = this.syncService.syncCompany(slug).orElseThrow();
				fetched += result.fetched();
				inserted += result.inserted();
				updated += result.updated();
				deleted += result.deleted();
			}
			catch (RuntimeException ex) {
				// A board that died since the probe answers 404, and one of those must not
				// throw away the whole run: the company keeps the openings it already had
				// and the next run picks it up again.
				logger.warn("Loading the Greenhouse openings of {} failed: {}", slug, ex.getMessage());
				failed++;
				itemFailed = true;
			}
			progress.itemDone(itemFailed);
		}

		return new SweepResult(slugs.size(), fetched, inserted, updated, deleted, failed);
	}

	private void pause() {
		if (this.pauseBetweenCompanies.isZero()) {
			return;
		}
		try {
			Thread.sleep(this.pauseBetweenCompanies);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while loading Greenhouse openings", ex);
		}
	}

	public record SweepResult(int companies, int fetched, int inserted, int updated, int deleted, int failed) {
	}
}
