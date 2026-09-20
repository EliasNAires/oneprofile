package oneprofile.backend.normalizedvacancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.company.Ats;
import oneprofile.backend.company.Companies;
import oneprofile.backend.company.CompanyConfiguration;
import oneprofile.backend.vacancy.PublishedVacancy;
import oneprofile.backend.vacancy.Vacancies;
import oneprofile.backend.vacancy.VacancyConfiguration;
import oneprofile.backend.vacancy.VacancyTitle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, CompanyConfiguration.class, VacancyConfiguration.class,
		NormalizedVacancyConfiguration.class })
class NormalizedVacanciesTest {

	@Autowired
	private NormalizedVacancies normalized;

	@Autowired
	private NormalizedVacancyRepository repository;

	@Autowired
	private Companies companies;

	@Autowired
	private Vacancies vacancies;

	@Autowired
	private TestEntityManager entityManager;

	@BeforeEach
	void holdTheCorpus() {
		this.companies.record(Ats.GREENHOUSE, List.of("stripe"));
		this.vacancies.mirror(Ats.GREENHOUSE, "stripe", List.of(published(4001), published(4002)));
	}

	@Test
	void holdsTheCleanedTitleAndEveryLevelItNames() {
		long vacancyId = heldTitles().getFirst().id();

		assertThat(this.normalized.recordCleanedTitles(
				Map.of(vacancyId, new CleanedTitle("Backend Engineer", Set.of(SeniorityLevel.SENIOR)))))
			.isEqualTo(1);

		assertThat(this.repository.findAll()).singleElement().satisfies((held) -> {
			assertThat(held.vacancyId()).isEqualTo(vacancyId);
			assertThat(held.cleanedTitle()).isEqualTo("Backend Engineer");
			assertThat(held.titleSeniorities()).containsExactly(SeniorityLevel.SENIOR);
		});
	}

	@Test
	void holdsEveryLevelATitleNames() {
		long vacancyId = heldTitles().getFirst().id();

		this.normalized.recordCleanedTitles(Map.of(vacancyId,
				new CleanedTitle("Engineer", Set.of(SeniorityLevel.SENIOR, SeniorityLevel.PRINCIPAL))));

		assertThat(this.repository.findAll()).singleElement()
			.extracting(NormalizedVacancy::titleSeniorities)
			.isEqualTo(Set.of(SeniorityLevel.SENIOR, SeniorityLevel.PRINCIPAL));
	}

	@Test
	void replacesWhatTheLastRunRecordedRatherThanAddingToIt() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedTitles(
				Map.of(vacancyId, new CleanedTitle("Engineer", Set.of(SeniorityLevel.SENIOR))));

		this.normalized.recordCleanedTitles(Map.of(vacancyId, new CleanedTitle("Backend Engineer", Set.of())));

		assertThat(this.repository.findAll()).singleElement().satisfies((held) -> {
			assertThat(held.cleanedTitle()).isEqualTo("Backend Engineer");
			assertThat(held.titleSeniorities()).isEmpty();
		});
	}

	@Test
	void recordsOneRowPerVacancyOfABatch() {
		Map<Long, CleanedTitle> batch = Map.of(heldTitles().get(0).id(), new CleanedTitle("Backend Engineer", Set.of()),
				heldTitles().get(1).id(), new CleanedTitle("Frontend Engineer", Set.of()));

		assertThat(this.normalized.recordCleanedTitles(batch)).isEqualTo(2);
		assertThat(this.repository.count()).isEqualTo(2);
	}

	@Test
	void letsGoOfWhatWasDerivedFromAVacancyThatHasLeftItsBoard() {
		this.normalized.recordCleanedTitles(Map.of(heldTitles().getFirst().id(),
				new CleanedTitle("Backend Engineer", Set.of(SeniorityLevel.SENIOR))));

		this.vacancies.mirror(Ats.GREENHOUSE, "stripe", List.of());
		this.entityManager.flush();

		assertThat(this.repository.count()).isZero();
	}

	private List<VacancyTitle> heldTitles() {
		return this.vacancies.titlesAfter(0, 10);
	}

	private PublishedVacancy published(long externalId) {
		return new PublishedVacancy(externalId, "Senior Backend Engineer", "Remote - Americas", "Engineering",
				"Ship payments.", "https://job-boards.greenhouse.io/stripe/jobs/" + externalId, null, null, null, null,
				Instant.parse("2026-09-01T14:00:00Z"), Instant.parse("2026-09-18T16:30:00Z"));
	}

}
