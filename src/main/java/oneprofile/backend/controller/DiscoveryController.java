package oneprofile.backend.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import oneprofile.backend.service.GreenhouseDiscoveryService;
import oneprofile.backend.service.GreenhouseDiscoveryService.CommonCrawlResult;
import oneprofile.backend.service.GreenhouseDiscoveryService.DiscoveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers the discovery by hand, on CommonCrawl or on Wayback. A run takes long enough
 * that the request cannot wait for it: the answer is an immediate 202 and the outcome
 * goes to the log. Both share one run flag because they write the same table.
 */
@RestController
public class DiscoveryController {

	private static final Logger logger = LoggerFactory.getLogger(DiscoveryController.class);

	private final GreenhouseDiscoveryService discoveryService;

	/** A single thread: two runs at once would fight over the same companies. */
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final AtomicBoolean running = new AtomicBoolean();

	public DiscoveryController(GreenhouseDiscoveryService discoveryService) {
		this.discoveryService = discoveryService;
	}

	@PostMapping("/admin/discovery/greenhouse/commoncrawl")
	public ResponseEntity<Void> discoverOnCommonCrawl() {
		return start("CommonCrawl", this::runCommonCrawl);
	}

	@PostMapping("/admin/discovery/greenhouse/wayback")
	public ResponseEntity<Void> discoverOnWayback() {
		return start("Wayback", this::runWayback);
	}

	private ResponseEntity<Void> start(String source, Runnable run) {
		if (!this.running.compareAndSet(false, true)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		this.executor.execute(() -> run(source, run));
		return ResponseEntity.accepted().build();
	}

	private void run(String source, Runnable run) {
		logger.info("Greenhouse discovery on {} started", source);
		try {
			run.run();
		}
		catch (RuntimeException ex) {
			logger.error("Greenhouse discovery on {} aborted", source, ex);
		}
		finally {
			this.running.set(false);
		}
	}

	private void runCommonCrawl() {
		CommonCrawlResult result = this.discoveryService.discoverOnRecentCommonCrawl();
		int newCompanies = result.indexes().stream().mapToInt(index -> index.result().newCompanies()).sum();
		logger.info("Greenhouse discovery on CommonCrawl finished: {} indexes read, {} new companies saved, "
				+ "{} indexes failed {}", result.indexes().size(), newCompanies, result.failedIndexes().size(),
				result.failedIndexes());
	}

	private void runWayback() {
		DiscoveryResult result = this.discoveryService.discoverOnWayback();
		logger.info("Greenhouse discovery on Wayback finished: {} slugs found, {} new companies saved",
				result.slugsFound(), result.newCompanies());
	}

	@PreDestroy
	void shutdown() {
		this.executor.shutdown();
	}
}
