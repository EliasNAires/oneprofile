package oneprofile.backend.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import oneprofile.backend.service.GreenhouseVacancySweepService;
import oneprofile.backend.service.GreenhouseVacancySweepService.SweepResult;
import oneprofile.backend.service.GreenhouseVacancySyncService;
import oneprofile.backend.service.GreenhouseVacancySyncService.SyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Loads the openings of Greenhouse: one company by hand, or every active board at once.
 * The two answer differently on purpose — a single board takes seconds and a whole run
 * takes hours.
 */
@RestController
public class VacancyController {

	private static final Logger logger = LoggerFactory.getLogger(VacancyController.class);

	private final GreenhouseVacancySyncService syncService;

	private final GreenhouseVacancySweepService sweepService;

	/** A single thread: two runs at once would fight over the same companies. */
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final AtomicBoolean running = new AtomicBoolean();

	public VacancyController(GreenhouseVacancySyncService syncService, GreenhouseVacancySweepService sweepService) {
		this.syncService = syncService;
		this.sweepService = sweepService;
	}

	@PostMapping("/admin/vacancies/greenhouse/{slug}")
	public ResponseEntity<SyncResult> syncGreenhouse(@PathVariable String slug) {
		return ResponseEntity.of(this.syncService.syncCompany(slug));
	}

	@PostMapping("/admin/vacancies/greenhouse")
	public ResponseEntity<Void> sweepGreenhouse() {
		if (!this.running.compareAndSet(false, true)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		this.executor.execute(this::run);
		return ResponseEntity.accepted().build();
	}

	private void run() {
		logger.info("Greenhouse vacancy sweep started");
		try {
			SweepResult result = this.sweepService.syncAllActive();
			logger.info("Greenhouse vacancy sweep finished: {} companies, {} fetched, {} inserted, {} updated, "
					+ "{} deleted, {} failed", result.companies(), result.fetched(), result.inserted(),
					result.updated(), result.deleted(), result.failed());
		}
		catch (RuntimeException ex) {
			logger.error("Greenhouse vacancy sweep aborted", ex);
		}
		finally {
			this.running.set(false);
		}
	}

	@PreDestroy
	void shutdown() {
		this.executor.shutdown();
	}
}
