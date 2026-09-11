package oneprofile.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.model.Company;
import oneprofile.backend.model.Vacancy;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.repository.VacancyRepository;
import oneprofile.backend.service.GreenhouseBoardClient.BoardJob;
import oneprofile.backend.service.GreenhouseVacancySweepService.SweepResult;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClientException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class GreenhouseVacancySweepServiceTest {

	@Autowired
	private CompanyRepository companies;

	@Autowired
	private VacancyRepository vacancies;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void asksForOpeningsOnlyWhereTheBoardHadThem() {
		store("globant", BoardStatus.ACTIVE);
		store("auth0", BoardStatus.EMPTY);
		store("mercadolibre", BoardStatus.NOT_FOUND);
		store("splice", null);

		SweepResult result = sweep(Map.of(
				"globant", List.of(job(4001L), job(4002L)),
				"auth0", List.of(job(4003L)),
				"mercadolibre", List.of(job(4004L)),
				"splice", List.of(job(4005L))));

		assertThat(result).isEqualTo(new SweepResult(1, 2, 2, 0, 0, 0));
		assertThat(this.vacancies.findAll()).extracting(Vacancy::getExternalId)
				.containsExactlyInAnyOrder(4001L, 4002L);
	}

	@Test
	void addsUpWhatEveryCompanyInsertedUpdatedAndDeleted() {
		store("globant", BoardStatus.ACTIVE);
		store("auth0", BoardStatus.ACTIVE);
		sweep(Map.of("globant", List.of(job(4001L), job(4002L)), "auth0", List.of(job(4003L))));

		SweepResult result = sweep(Map.of(
				"globant", List.of(job(4001L)),
				"auth0", List.of(job(4003L), job(4004L))));

		assertThat(result).isEqualTo(new SweepResult(2, 3, 1, 2, 1, 0));
		assertThat(this.vacancies.findAll()).extracting(Vacancy::getExternalId)
				.containsExactlyInAnyOrder(4001L, 4003L, 4004L);
	}

	@Test
	void keepsGoingWhenOneBoardCannotBeReached() {
		store("globant", BoardStatus.ACTIVE);
		store("auth0", BoardStatus.ACTIVE);
		sweep(Map.of("globant", List.of(job(4001L)), "auth0", List.of(job(4003L))));

		// globant is missing from the canned boards, so the fake client fails on it.
		SweepResult result = sweep(Map.of("auth0", List.of(job(4003L), job(4004L))));

		assertThat(result).isEqualTo(new SweepResult(2, 2, 1, 1, 0, 1));
		assertThat(this.vacancies.findAll()).extracting(Vacancy::getExternalId)
				.containsExactlyInAnyOrder(4001L, 4003L, 4004L);
	}

	private void store(String slug, BoardStatus status) {
		Company company = new Company(Ats.GREENHOUSE, slug);
		company.recordProbe(status, null, Instant.parse("2026-09-10T12:00:00Z"));
		this.companies.save(company);
		this.entityManager.flush();
		this.entityManager.clear();
	}

	private SweepResult sweep(Map<String, List<BoardJob>> boardsBySlug) {
		GreenhouseVacancySyncService syncService = new GreenhouseVacancySyncService(new FakeBoardClient(boardsBySlug),
				this.companies, this.vacancies);
		SweepResult result = new GreenhouseVacancySweepService(this.companies, syncService, Duration.ZERO)
				.syncAllActive();
		this.entityManager.flush();
		this.entityManager.clear();
		return result;
	}

	private static BoardJob job(long externalId) {
		return new BoardJob(externalId, "Backend Engineer", "Buenos Aires, Argentina", "Engineering",
				"Java and Spring", "https://job-boards.greenhouse.io/jobs/" + externalId, "en", null, null, null, null,
				Instant.parse("2026-09-09T14:50:29Z"), Instant.parse("2026-09-10T17:11:58Z"));
	}

	/** Hands back a canned board per slug; a slug with none stands for an unreachable board. */
	private static final class FakeBoardClient extends GreenhouseBoardClient {

		private final Map<String, List<BoardJob>> boardsBySlug;

		private FakeBoardClient(Map<String, List<BoardJob>> boardsBySlug) {
			this.boardsBySlug = boardsBySlug;
		}

		@Override
		public List<BoardJob> jobs(String slug) {
			List<BoardJob> jobs = this.boardsBySlug.get(slug);
			if (jobs == null) {
				throw new RestClientException("no route to the board of " + slug);
			}
			return jobs;
		}
	}
}
