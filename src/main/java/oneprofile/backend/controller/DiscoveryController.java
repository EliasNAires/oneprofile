package oneprofile.backend.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import oneprofile.backend.service.GreenhouseDiscoveryService;
import oneprofile.backend.service.GreenhouseDiscoveryService.CommonCrawlResult;
import oneprofile.backend.service.GreenhouseDiscoveryService.IndexResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers the discovery by hand. A run takes long enough that the request cannot
 * wait for it: the answer is an immediate 202 and the outcome goes to the log.
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
		if (!this.running.compareAndSet(false, true)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		this.executor.execute(this::runCommonCrawl);
		return ResponseEntity.accepted().build();
	}

	private void runCommonCrawl() {
		logger.info("Greenhouse discovery started on the most recent CommonCrawl indexes");
		try {
			CommonCrawlResult result = this.discoveryService.discoverOnRecentCommonCrawl();
			int newCompanies = 0;
			for (IndexResult index : result.indexes()) {
				logger.info("CommonCrawl index {}: {} slugs found, {} new companies saved", index.indexId(),
						index.result().slugsFound(), index.result().newCompanies());
				newCompanies += index.result().newCompanies();
			}
			logger.info("Greenhouse discovery on CommonCrawl finished: {} indexes read, {} new companies saved, "
					+ "{} indexes failed {}", result.indexes().size(), newCompanies, result.failedIndexes().size(),
					result.failedIndexes());
		}
		catch (RuntimeException ex) {
			logger.error("Greenhouse discovery on CommonCrawl failed", ex);
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
