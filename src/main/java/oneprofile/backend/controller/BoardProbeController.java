package oneprofile.backend.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import oneprofile.backend.service.GreenhouseBoardProbeService;
import oneprofile.backend.service.GreenhouseBoardProbeService.ProbeResult;
import oneprofile.backend.service.GreenhouseTruncatedSlugCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers the board probe by hand. A run takes minutes, so the request cannot wait
 * for it: the answer is an immediate 202 and the outcome goes to the log. Once the
 * probe is done, the truncated-slug cleanup runs on its own on the same thread.
 */
@RestController
public class BoardProbeController {

	private static final Logger logger = LoggerFactory.getLogger(BoardProbeController.class);

	private final GreenhouseBoardProbeService probeService;

	private final GreenhouseTruncatedSlugCleanupService cleanupService;

	/** A single thread: two runs at once would fight over the same companies. */
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final AtomicBoolean running = new AtomicBoolean();

	public BoardProbeController(GreenhouseBoardProbeService probeService,
			GreenhouseTruncatedSlugCleanupService cleanupService) {
		this.probeService = probeService;
		this.cleanupService = cleanupService;
	}

	@PostMapping("/admin/probe/greenhouse")
	public ResponseEntity<Void> probeGreenhouse() {
		if (!this.running.compareAndSet(false, true)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		this.executor.execute(this::run);
		return ResponseEntity.accepted().build();
	}

	private void run() {
		logger.info("Greenhouse board probe started");
		boolean probeFinished;
		try {
			ProbeResult result = this.probeService.probeAll();
			logger.info("Greenhouse board probe finished: {} companies, {} not found, {} empty, {} active, {} failed",
					result.companies(), result.notFound(), result.empty(), result.active(), result.failed());
			probeFinished = true;
		}
		catch (RuntimeException ex) {
			logger.error("Greenhouse board probe aborted", ex);
			probeFinished = false;
		}
		if (probeFinished) {
			runCleanup();
		}
		this.running.set(false);
	}

	private void runCleanup() {
		try {
			int removed = this.cleanupService.cleanup();
			logger.info("Greenhouse truncated slug cleanup finished: {} companies removed and blacklisted", removed);
		}
		catch (RuntimeException ex) {
			logger.error("Greenhouse truncated slug cleanup aborted", ex);
		}
	}

	@PreDestroy
	void shutdown() {
		this.executor.shutdown();
	}
}
