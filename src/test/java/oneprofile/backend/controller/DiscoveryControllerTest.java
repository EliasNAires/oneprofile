package oneprofile.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oneprofile.backend.service.GreenhouseDiscoveryService;
import oneprofile.backend.service.GreenhouseDiscoveryService.CommonCrawlResult;
import oneprofile.backend.service.GreenhouseDiscoveryService.DiscoveryResult;
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

@WebMvcTest(DiscoveryController.class)
@ExtendWith(OutputCaptureExtension.class)
class DiscoveryControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private GreenhouseDiscoveryService discoveryService;

	/** Holds the run started by the first request, so a second one finds it in progress. */
	private final CountDownLatch runBlocked = new CountDownLatch(1);

	private final CountDownLatch runStarted = new CountDownLatch(1);

	@AfterEach
	void releaseTheRun() {
		this.runBlocked.countDown();
	}

	@Test
	void acceptsTheRequestAndDiscoversInTheBackground() throws InterruptedException {
		blockTheCommonCrawlRun();

		assertThat(postCommonCrawl()).hasStatus(202);

		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();
		verify(this.discoveryService).discoverOnRecentCommonCrawl();
	}

	@Test
	void rejectsASecondRunWhileOneIsInProgress() throws InterruptedException {
		blockTheCommonCrawlRun();
		assertThat(postCommonCrawl()).hasStatus(202);
		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(postCommonCrawl()).hasStatus(409);
	}

	@Test
	void acceptsTheWaybackRequestAndDiscoversInTheBackground() throws InterruptedException {
		given(this.discoveryService.discoverOnWayback()).willAnswer(invocation -> {
			this.runStarted.countDown();
			this.runBlocked.await();
			return new DiscoveryResult(0, 0);
		});

		assertThat(postWayback()).hasStatus(202);

		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();
		verify(this.discoveryService).discoverOnWayback();
	}

	@Test
	void rejectsWaybackWhileACommonCrawlRunIsInProgress() throws InterruptedException {
		blockTheCommonCrawlRun();
		assertThat(postCommonCrawl()).hasStatus(202);
		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(postWayback()).hasStatus(409);
	}

	@Test
	void logsAbortedWhenTheServiceThrows(CapturedOutput output) {
		given(this.discoveryService.discoverOnRecentCommonCrawl()).willThrow(new RuntimeException("boom"));

		assertThat(postCommonCrawl()).hasStatus(202);

		// The run flag only clears after the catch block logs, so waiting for a second
		// request to be accepted proves the first run (and its log line) is done.
		await().atMost(Duration.ofSeconds(5)).until(() -> postCommonCrawl().getResponse().getStatus() == 202);
		assertThat(output.getOut()).contains("Greenhouse discovery on CommonCrawl aborted");
	}

	private void blockTheCommonCrawlRun() {
		given(this.discoveryService.discoverOnRecentCommonCrawl()).willAnswer(invocation -> {
			this.runStarted.countDown();
			this.runBlocked.await();
			return new CommonCrawlResult(List.of(), List.of());
		});
	}

	private MvcTestResult postCommonCrawl() {
		return this.mvc.post().uri("/admin/discovery/greenhouse/commoncrawl").exchange();
	}

	private MvcTestResult postWayback() {
		return this.mvc.post().uri("/admin/discovery/greenhouse/wayback").exchange();
	}
}
