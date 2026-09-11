package oneprofile.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oneprofile.backend.service.GreenhouseDiscoveryService;
import oneprofile.backend.service.GreenhouseDiscoveryService.DiscoveryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(DiscoveryController.class)
class DiscoveryControllerTest {

	private static final String INDEX_ID = "CC-MAIN-2026-34";

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

		assertThat(post(INDEX_ID)).hasStatus(202);

		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();
		verify(this.discoveryService).discover(INDEX_ID);
	}

	@Test
	void rejectsASecondRunWhileOneIsInProgress() throws InterruptedException {
		blockTheRun();
		assertThat(post(INDEX_ID)).hasStatus(202);
		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(post(INDEX_ID)).hasStatus(409);
	}

	private void blockTheRun() {
		given(this.discoveryService.discover(any())).willAnswer(invocation -> {
			this.runStarted.countDown();
			this.runBlocked.await();
			return new DiscoveryResult(0, 0);
		});
	}

	private MvcTestResult post(String index) {
		return this.mvc.post().uri("/admin/discovery/greenhouse").param("index", index).exchange();
	}
}
