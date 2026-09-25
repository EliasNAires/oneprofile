package oneprofile.backend.storage.normalizedvacancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.VacancyStore;
import oneprofile.backend.storage.vacancy.VacancyTitle;
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
@Import({ TestcontainersConfiguration.class, CompanyStore.class, VacancyStore.class,
		NormalizedVacancyStore.class })
class NormalizedVacancyStoreTest {

	@Autowired
	private NormalizedVacancyStore normalized;

	@Autowired
	private NormalizedVacancyRepository repository;

	@Autowired
	private CompanyStore companies;

	@Autowired
	private VacancyStore vacancies;

	@Autowired
	private TestEntityManager entityManager;

	@BeforeEach
	void holdTheCorpus() {
		this.companies.record(AtsEnum.GREENHOUSE, List.of("stripe"));
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001), published(4002)));
	}

	@Test
	void holdsTheCleanedTitleAndEveryLevelItNames() {
		long vacancyId = heldTitles().getFirst().id();

		assertThat(this.normalized.recordCleanedTitles(
				Map.of(vacancyId, new CleanedTitle("Backend Engineer", Set.of(SeniorityLevelEnum.SENIOR)))))
			.isEqualTo(1);

		assertThat(this.repository.findAll()).singleElement().satisfies((held) -> {
			assertThat(held.vacancyId()).isEqualTo(vacancyId);
			assertThat(held.cleanedTitle()).isEqualTo("Backend Engineer");
			assertThat(held.titleSeniorities()).containsExactly(SeniorityLevelEnum.SENIOR);
		});
	}

	@Test
	void holdsEveryLevelATitleNames() {
		long vacancyId = heldTitles().getFirst().id();

		this.normalized.recordCleanedTitles(Map.of(vacancyId,
				new CleanedTitle("Engineer", Set.of(SeniorityLevelEnum.SENIOR, SeniorityLevelEnum.PRINCIPAL))));

		assertThat(this.repository.findAll()).singleElement()
			.extracting(NormalizedVacancyEntity::titleSeniorities)
			.isEqualTo(Set.of(SeniorityLevelEnum.SENIOR, SeniorityLevelEnum.PRINCIPAL));
	}

	@Test
	void replacesWhatTheLastRunRecordedRatherThanAddingToIt() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedTitles(
				Map.of(vacancyId, new CleanedTitle("Engineer", Set.of(SeniorityLevelEnum.SENIOR))));

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
				new CleanedTitle("Backend Engineer", Set.of(SeniorityLevelEnum.SENIOR))));

		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of());
		this.entityManager.flush();

		assertThat(this.repository.count()).isZero();
	}

	@Test
	void holdsWhatClassificationMadeOfACleanedTitle() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedTitles(Map.of(vacancyId, new CleanedTitle("Backend Engineer", Set.of())));

		assertThat(this.normalized.recordClassifications(Map.of(vacancyId, Classification.in()))).isEqualTo(1);

		assertThat(this.repository.findAll()).singleElement()
			.extracting(NormalizedVacancyEntity::classification)
			.isEqualTo(Classification.in());
	}

	@Test
	void holdsWhyATitleWasLeftUndecided() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedTitles(Map.of(vacancyId, new CleanedTitle("Engineer", Set.of())));

		this.normalized.recordClassifications(
				Map.of(vacancyId, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));

		assertThat(this.repository.findAll()).singleElement()
			.extracting(NormalizedVacancyEntity::classification)
			.isEqualTo(Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY));
	}

	@Test
	void replacesWhatTheLastClassificationRunDecided() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedTitles(Map.of(vacancyId, new CleanedTitle("Backend Engineer", Set.of())));
		this.normalized.recordClassifications(Map.of(vacancyId, Classification.unknown(UnknownReasonEnum.UNRULED)));

		this.normalized.recordClassifications(Map.of(vacancyId, Classification.in()));

		assertThat(this.repository.findAll()).singleElement()
			.extracting(NormalizedVacancyEntity::classification)
			.isEqualTo(Classification.in());
	}

	@Test
	void classifiesNothingForAVacancyCleaningHasNotReached() {
		assertThat(this.normalized.recordClassifications(Map.of(heldTitles().getFirst().id(), Classification.in())))
			.isZero();
		assertThat(this.repository.count()).isZero();
	}

	@Test
	void readsTheCleanedTitlesHeldAfterOneVacancyInIdOrder() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedTitles(Map.of(first, new CleanedTitle("Backend Engineer", Set.of()), second,
				new CleanedTitle("Frontend Engineer", Set.of())));

		assertThat(this.normalized.cleanedTitlesAfter(0, 10)).containsExactly(
				new NormalizedTitle(first, "Backend Engineer"), new NormalizedTitle(second, "Frontend Engineer"));
		assertThat(this.normalized.cleanedTitlesAfter(first, 10))
			.containsExactly(new NormalizedTitle(second, "Frontend Engineer"));
		assertThat(this.normalized.cleanedTitlesAfter(second, 10)).isEmpty();
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
