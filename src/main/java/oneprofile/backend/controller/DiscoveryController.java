package oneprofile.backend.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import oneprofile.backend.service.GreenhouseDiscoveryService;
import oneprofile.backend.service.GreenhouseDiscoveryService.DiscoveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	@PostMapping("/admin/discovery/greenhouse")
	public ResponseEntity<Void> discoverGreenhouse(@RequestParam("index") String index) {
		if (!this.running.compareAndSet(false, true)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		this.executor.execute(() -> run(index));
		return ResponseEntity.accepted().build();
	}

	private void run(String index) {
		logger.info("Greenhouse discovery started on CommonCrawl index {}", index);
		try {
			DiscoveryResult result = this.discoveryService.discover(index);
			logger.info("Greenhouse discovery finished: {} slugs found, {} new companies saved",
					result.slugsFound(), result.newCompanies());
		}
		catch (RuntimeException ex) {
			logger.error("Greenhouse discovery on index {} failed", index, ex);
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
