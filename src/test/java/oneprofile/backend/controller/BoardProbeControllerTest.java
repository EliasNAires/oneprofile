package oneprofile.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oneprofile.backend.service.GreenhouseBoardProbeService;
import oneprofile.backend.service.GreenhouseBoardProbeService.ProbeResult;
import oneprofile.backend.service.GreenhouseTruncatedSlugCleanupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(BoardProbeController.class)
@ExtendWith(OutputCaptureExtension.class)
class BoardProbeControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private GreenhouseBoardProbeService probeService;

	@MockitoBean
	private GreenhouseTruncatedSlugCleanupService cleanupService;

	/** Holds the run started by the first request, so a second one finds it in progress. */
	private final CountDownLatch runBlocked = new CountDownLatch(1);

	private final CountDownLatch runStarted = new CountDownLatch(1);

	@AfterEach
	void releaseTheRun() {
		this.runBlocked.countDown();
	}

	@Test
	void acceptsTheRequestAndProbesInTheBackground() throws InterruptedException {
		blockTheRun();

		assertThat(post()).hasStatus(202);

		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();
		verify(this.probeService).probeAll();
	}

	@Test
	void rejectsASecondRunWhileOneIsInProgress() throws InterruptedException {
		blockTheRun();
		assertThat(post()).hasStatus(202);
		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(post()).hasStatus(409);
	}

	@Test
	void cleansUpTruncatedSlugsAfterASuccessfulProbe(CapturedOutput output) {
		given(this.probeService.probeAll()).willReturn(new ProbeResult(0, 0, 0, 0, 0));
		given(this.cleanupService.cleanup()).willReturn(3);

		assertThat(post()).hasStatus(202);

		// Polling with more requests can start further runs once the flag is free again,
		// same as the aborted case below: the log line is what a single run is checked on.
		await().atMost(Duration.ofSeconds(5)).until(() -> output.getOut().contains("truncated slug cleanup"));
		assertThat(output.getOut())
			.contains("Greenhouse truncated slug cleanup finished: 3 companies removed and blacklisted");
	}

	@Test
	void logsAbortedWhenTheCleanupThrowsAndStillReleasesTheRunFlag(CapturedOutput output) {
		given(this.probeService.probeAll()).willReturn(new ProbeResult(0, 0, 0, 0, 0));
		given(this.cleanupService.cleanup()).willThrow(new RuntimeException("boom"));

		assertThat(post()).hasStatus(202);

		// The run flag only clears after the catch block logs, so waiting for a second
		// request to be accepted proves the first run (and its log line) is done.
		await().atMost(Duration.ofSeconds(5)).until(() -> post().getResponse().getStatus() == 202);
		assertThat(output.getOut()).contains("Greenhouse truncated slug cleanup aborted");
	}

	private void blockTheRun() {
		given(this.probeService.probeAll()).willAnswer(invocation -> {
			this.runStarted.countDown();
			this.runBlocked.await();
			return new ProbeResult(0, 0, 0, 0, 0);
		});
	}

	private MvcTestResult post() {
		return this.mvc.post().uri("/admin/probe/greenhouse").exchange();
	}
}
