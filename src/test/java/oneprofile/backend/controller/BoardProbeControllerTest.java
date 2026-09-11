package oneprofile.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oneprofile.backend.service.GreenhouseBoardProbeService;
import oneprofile.backend.service.GreenhouseBoardProbeService.ProbeResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(BoardProbeController.class)
class BoardProbeControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private GreenhouseBoardProbeService probeService;

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
