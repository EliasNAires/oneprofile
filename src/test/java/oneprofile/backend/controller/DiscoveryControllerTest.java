package oneprofile.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oneprofile.backend.service.GreenhouseDiscoveryService;
import oneprofile.backend.service.GreenhouseDiscoveryService.CommonCrawlResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(DiscoveryController.class)
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
		blockTheRun();

		assertThat(postCommonCrawl()).hasStatus(202);

		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();
		verify(this.discoveryService).discoverOnRecentCommonCrawl();
	}

	@Test
	void rejectsASecondRunWhileOneIsInProgress() throws InterruptedException {
		blockTheRun();
		assertThat(postCommonCrawl()).hasStatus(202);
		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(postCommonCrawl()).hasStatus(409);
	}

	private void blockTheRun() {
		given(this.discoveryService.discoverOnRecentCommonCrawl()).willAnswer(invocation -> {
			this.runStarted.countDown();
			this.runBlocked.await();
			return new CommonCrawlResult(List.of(), List.of());
		});
	}

	private MvcTestResult postCommonCrawl() {
		return this.mvc.post().uri("/admin/discovery/greenhouse/commoncrawl").exchange();
	}
}
