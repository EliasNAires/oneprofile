package oneprofile.backend.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import jakarta.annotation.PreDestroy;
import oneprofile.backend.service.VacancyNormalizationService;
import oneprofile.backend.service.VacancyNormalizationService.NormalizationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Normalizes the titles of the stored vacancies: all of them again, or only the ones
 * with no normalized row yet. Both share one run flag because they write the same table.
 */
@RestController
public class NormalizationController {

	private static final Logger logger = LoggerFactory.getLogger(NormalizationController.class);

	private final VacancyNormalizationService normalizationService;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final AtomicBoolean running = new AtomicBoolean();

	public NormalizationController(VacancyNormalizationService normalizationService) {
		this.normalizationService = normalizationService;
	}

	@PostMapping("/admin/normalization/vacancies")
	public ResponseEntity<Void> normalizeAll() {
		return start("all", this.normalizationService::normalizeAll);
	}

	@PostMapping("/admin/normalization/vacancies/missing")
	public ResponseEntity<Void> normalizeMissing() {
		return start("missing", this.normalizationService::normalizeMissing);
	}

	private ResponseEntity<Void> start(String scope, Supplier<NormalizationResult> run) {
		if (!this.running.compareAndSet(false, true)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		this.executor.execute(() -> run(scope, run));
		return ResponseEntity.accepted().build();
	}

	private void run(String scope, Supplier<NormalizationResult> run) {
		logger.info("Vacancy normalization ({}) started", scope);
		try {
			NormalizationResult result = run.get();
			logger.info("Vacancy normalization ({}) finished: {} inserted, {} updated", scope, result.inserted(),
					result.updated());
		}
		catch (RuntimeException ex) {
			logger.error("Vacancy normalization ({}) failed", scope, ex);
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
