package oneprofile.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oneprofile.backend.service.GreenhouseVacancySweepService;
import oneprofile.backend.service.GreenhouseVacancySweepService.SweepResult;
import oneprofile.backend.service.GreenhouseVacancySyncService;
import oneprofile.backend.service.GreenhouseVacancySyncService.SyncResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(VacancyController.class)
class VacancyControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private GreenhouseVacancySyncService syncService;

	@MockitoBean
	private GreenhouseVacancySweepService sweepService;

	/** Holds the run started by the first request, so a second one finds it in progress. */
	private final CountDownLatch runBlocked = new CountDownLatch(1);

	private final CountDownLatch runStarted = new CountDownLatch(1);

	@AfterEach
	void releaseTheRun() {
		this.runBlocked.countDown();
	}

	@Test
	void answersInLineWithWhatWasLoaded() {
		given(this.syncService.syncCompany("globant")).willReturn(Optional.of(new SyncResult(45, 40, 5, 2)));

		assertThat(this.mvc.post().uri("/admin/vacancies/greenhouse/globant"))
				.hasStatusOk()
				.bodyJson()
				.isEqualTo("{\"fetched\":45,\"inserted\":40,\"updated\":5,\"deleted\":2}");
	}

	@Test
	void answersNotFoundForASlugThatIsNotAKnownCompany() {
		given(this.syncService.syncCompany("no-existe")).willReturn(Optional.empty());

		assertThat(this.mvc.post().uri("/admin/vacancies/greenhouse/no-existe")).hasStatus(404);
	}

	@Test
	void acceptsTheRequestAndSweepsInTheBackground() throws InterruptedException {
		blockTheRun();

		assertThat(this.mvc.post().uri("/admin/vacancies/greenhouse")).hasStatus(202);

		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();
		verify(this.sweepService).syncAllActive();
	}

	@Test
	void rejectsASecondSweepWhileOneIsInProgress() throws InterruptedException {
		blockTheRun();
		assertThat(this.mvc.post().uri("/admin/vacancies/greenhouse")).hasStatus(202);
		assertThat(this.runStarted.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(this.mvc.post().uri("/admin/vacancies/greenhouse")).hasStatus(409);
	}

	private void blockTheRun() {
		given(this.sweepService.syncAllActive()).willAnswer(invocation -> {
			this.runStarted.countDown();
			this.runBlocked.await();
			return new SweepResult(0, 0, 0, 0, 0, 0);
		});
	}
}
