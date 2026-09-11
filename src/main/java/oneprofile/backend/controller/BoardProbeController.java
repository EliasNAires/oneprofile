package oneprofile.backend.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import oneprofile.backend.service.GreenhouseBoardProbeService;
import oneprofile.backend.service.GreenhouseBoardProbeService.ProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers the board probe by hand. A run takes minutes, so the request cannot wait
 * for it: the answer is an immediate 202 and the outcome goes to the log.
 */
@RestController
public class BoardProbeController {

	private static final Logger logger = LoggerFactory.getLogger(BoardProbeController.class);

	private final GreenhouseBoardProbeService probeService;

	/** A single thread: two runs at once would fight over the same companies. */
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final AtomicBoolean running = new AtomicBoolean();

	public BoardProbeController(GreenhouseBoardProbeService probeService) {
		this.probeService = probeService;
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
		try {
			ProbeResult result = this.probeService.probeAll();
			logger.info("Greenhouse board probe finished: {} companies, {} not found, {} empty, {} active, {} failed",
					result.companies(), result.notFound(), result.empty(), result.active(), result.failed());
		}
		catch (RuntimeException ex) {
			logger.error("Greenhouse board probe failed", ex);
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
