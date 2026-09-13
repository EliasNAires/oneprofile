package oneprofile.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oneprofile.backend.service.VacancyNormalizationService;
import oneprofile.backend.service.VacancyNormalizationService.NormalizationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(NormalizationController.class)
class NormalizationControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private VacancyNormalizationService normalizationService;

	/** Holds the run started by the first request, so a second one finds it in progress. */
	private final CountDownLatch runBlocked = new CountDownLatch(1);

	private final CountDownLatch runStarted = new CountDownLatch(1);

	@AfterEach
	void releaseTheRun() {
		this.runBlocked.countDown();
	}

	@Test
	void acceptsTheRequestAndNormalizesEverythingInTheBackground() throws InterruptedException {
		given(this.normalizationService.normalizeAll()).willAnswer(invocation -> blockedRun());

		assertThat(this.mvc.post().uri("/admin/normalization/vacancies")).hasStatus(202);

		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();
		verify(this.normalizationService).normalizeAll();
	}

	@Test
	void acceptsTheRequestAndNormalizesTheMissingOnesInTheBackground() throws InterruptedException {
		given(this.normalizationService.normalizeMissing()).willAnswer(invocation -> blockedRun());

		assertThat(this.mvc.post().uri("/admin/normalization/vacancies/missing")).hasStatus(202);

		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();
		verify(this.normalizationService).normalizeMissing();
	}

	@Test
	void rejectsEitherRunWhileOneIsInProgress() throws InterruptedException {
		given(this.normalizationService.normalizeAll()).willAnswer(invocation -> blockedRun());
		assertThat(this.mvc.post().uri("/admin/normalization/vacancies")).hasStatus(202);
		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(this.mvc.post().uri("/admin/normalization/vacancies")).hasStatus(409);
		assertThat(this.mvc.post().uri("/admin/normalization/vacancies/missing")).hasStatus(409);
	}

	private NormalizationResult blockedRun() throws InterruptedException {
		this.runStarted.countDown();
		this.runBlocked.await();
		return new NormalizationResult(0, 0);
	}
}
