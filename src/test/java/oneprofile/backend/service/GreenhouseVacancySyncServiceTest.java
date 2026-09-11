package oneprofile.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.List;

import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.Company;
import oneprofile.backend.model.Vacancy;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.repository.VacancyRepository;
import oneprofile.backend.service.GreenhouseBoardClient.BoardJob;
import oneprofile.backend.service.GreenhouseVacancySyncService.SyncResult;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class GreenhouseVacancySyncServiceTest {

	@Autowired
	private CompanyRepository companies;

	@Autowired
	private VacancyRepository vacancies;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void storesTheOpeningsOfTheCompany() {
		store("globant");

		SyncResult result = sync("globant", job(4001L, "Backend Engineer"), job(4002L, "Data Analyst"));

		assertThat(result).isEqualTo(new SyncResult(2, 2, 0, 0));
		assertThat(this.vacancies.findAll()).extracting(Vacancy::getExternalId, Vacancy::getTitle)
				.containsExactlyInAnyOrder(tuple(4001L, "Backend Engineer"), tuple(4002L, "Data Analyst"));
	}

	@Test
	void overwritesWhatTheBoardChangedInsteadOfDuplicatingIt() {
		store("globant");
		sync("globant", job(4001L, "Backend Engineer"));

		SyncResult result = sync("globant", job(4001L, "Senior Backend Engineer"));

		assertThat(result).isEqualTo(new SyncResult(1, 0, 1, 0));
		assertThat(this.vacancies.findAll()).extracting(Vacancy::getExternalId, Vacancy::getTitle)
				.containsExactly(tuple(4001L, "Senior Backend Engineer"));
	}

	@Test
	void removesAnOpeningThatIsNoLongerOnTheBoard() {
		// The table mirrors the board: disappearing from it is what says a search closed.
		store("globant");
		sync("globant", job(4001L, "Backend Engineer"), job(4002L, "Data Analyst"));

		SyncResult result = sync("globant", job(4001L, "Backend Engineer"));

		assertThat(result).isEqualTo(new SyncResult(1, 0, 1, 1));
		assertThat(this.vacancies.findAll()).extracting(Vacancy::getExternalId).containsExactly(4001L);
	}

	@Test
	void bringsNothingForASlugThatIsNotAKnownCompany() {
		assertThat(service().syncCompany("globant")).isEmpty();
		assertThat(this.vacancies.count()).isZero();
	}

	private void store(String... slugs) {
		for (String slug : slugs) {
			this.companies.save(new Company(Ats.GREENHOUSE, slug));
		}
		this.entityManager.flush();
		this.entityManager.clear();
	}

	private SyncResult sync(String slug, BoardJob... jobs) {
		SyncResult result = new GreenhouseVacancySyncService(new FakeBoardClient(List.of(jobs)), this.companies,
				this.vacancies).syncCompany(slug).orElseThrow();
		this.entityManager.flush();
		this.entityManager.clear();
		return result;
	}

	private GreenhouseVacancySyncService service() {
		return new GreenhouseVacancySyncService(new FakeBoardClient(List.of()), this.companies, this.vacancies);
	}

	private static BoardJob job(long externalId, String title) {
		return new BoardJob(externalId, title, "Buenos Aires, Argentina", "Engineering", "Java and Spring",
				"https://job-boards.greenhouse.io/globant/jobs/" + externalId, "en", 12000000L, 18000000L, "USD",
				"Annual base salary range:", Instant.parse("2026-09-09T14:50:29Z"),
				Instant.parse("2026-09-10T17:11:58Z"));
	}

	/** Answers with the same board whatever the slug: the test asks for one company. */
	private static final class FakeBoardClient extends GreenhouseBoardClient {

		private final List<BoardJob> jobs;

		private FakeBoardClient(List<BoardJob> jobs) {
			this.jobs = jobs;
		}

		@Override
		public List<BoardJob> jobs(String slug) {
			return this.jobs;
		}
	}
}
