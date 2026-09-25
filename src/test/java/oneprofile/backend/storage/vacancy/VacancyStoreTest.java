package oneprofile.backend.storage.vacancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, CompanyStore.class, VacancyStore.class })
class VacancyStoreTest {

	private static final Instant FIRST_PUBLISHED_AT = Instant.parse("2026-09-01T14:00:00Z");

	private static final Instant UPDATED_AT = Instant.parse("2026-09-18T16:30:00Z");

	@Autowired
	private VacancyStore vacancies;

	@Autowired
	private CompanyStore companies;

	@Autowired
	private VacancyRepository repository;

	@BeforeEach
	void holdTheCompany() {
		this.companies.record(AtsEnum.GREENHOUSE, List.of("stripe", "notion"));
	}

	@Test
	void holdsWhatTheBoardPublishes() {
		assertThat(this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001), published(4002))))
			.isEqualTo(new VacancyStore.Reconciliation(2, 0, 0));

		VacancyEntity held = held(4001);
		assertThat(held.title()).isEqualTo("Backend Engineer");
		assertThat(held.location()).isEqualTo("Remote - Americas");
		assertThat(held.department()).isEqualTo("Engineering");
		assertThat(held.description()).isEqualTo("Ship payments.");
		assertThat(held.url()).isEqualTo("https://job-boards.greenhouse.io/stripe/jobs/4001");
		assertThat(held.payMinCents()).isEqualTo(15000000L);
		assertThat(held.payMaxCents()).isEqualTo(20000000L);
		assertThat(held.payCurrency()).isEqualTo("USD");
		assertThat(held.payTitle()).isEqualTo("Annual Salary");
		assertThat(held.firstPublishedAt()).isEqualTo(FIRST_PUBLISHED_AT);
		assertThat(held.updatedAt()).isEqualTo(UPDATED_AT);
	}

	@Test
	void addsNothingWhenTheBoardHasNotChanged() {
		List<PublishedVacancy> board = List.of(published(4001), published(4002));
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", board);

		assertThat(this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", board))
			.isEqualTo(new VacancyStore.Reconciliation(0, 2, 0));
		assertThat(this.repository.count()).isEqualTo(2);
	}

	@Test
	void overwritesAVacancyTheBoardHasChanged() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001)));

		PublishedVacancy renamed = new PublishedVacancy(4001, "Staff Backend Engineer", "Remote - Americas",
				"Engineering", "Ship payments.", "https://job-boards.greenhouse.io/stripe/jobs/4001", 15000000L,
				20000000L, "USD", "Annual Salary", FIRST_PUBLISHED_AT, UPDATED_AT.plusSeconds(3600));
		assertThat(this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(renamed)))
			.isEqualTo(new VacancyStore.Reconciliation(0, 1, 0));

		VacancyEntity held = held(4001);
		assertThat(held.title()).isEqualTo("Staff Backend Engineer");
		assertThat(held.updatedAt()).isEqualTo(UPDATED_AT.plusSeconds(3600));
	}

	@Test
	void dropsAVacancyTheBoardNoLongerPublishes() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001), published(4002)));

		assertThat(this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4002))))
			.isEqualTo(new VacancyStore.Reconciliation(0, 1, 1));
		assertThat(this.repository.findAll()).singleElement()
			.extracting(VacancyEntity::externalId)
			.isEqualTo(4002L);
	}

	@Test
	void dropsEverythingWhenTheBoardPublishesNothing() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001)));

		assertThat(this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of()))
			.isEqualTo(new VacancyStore.Reconciliation(0, 0, 1));
		assertThat(this.repository.count()).isZero();
	}

	@Test
	void leavesTheVacanciesOfEveryOtherCompanyAlone() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "notion", List.of(published(4001)));

		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4002)));

		assertThat(this.repository.count()).isEqualTo(2);
	}

	@Test
	void refusesToMirrorABoardOfASlugItDoesNotHold() {
		assertThatThrownBy(() -> this.vacancies.mirror(AtsEnum.GREENHOUSE, "figma", List.of(published(4001))))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("figma");
	}

	@Test
	void readsTheTitlesOfTheCorpusInIdOrderABatchAtATime() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001), published(4002)));

		List<VacancyTitle> first = this.vacancies.titlesAfter(0, 1);

		assertThat(first).singleElement().extracting(VacancyTitle::title).isEqualTo("Backend Engineer");
		assertThat(this.vacancies.titlesAfter(first.getFirst().id(), 10)).hasSize(1);
	}

	@Test
	void readsNoTitlesOnceThereAreNoneLeft() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001)));
		List<VacancyTitle> all = this.vacancies.titlesAfter(0, 10);

		assertThat(this.vacancies.titlesAfter(all.getLast().id(), 10)).isEmpty();
	}

	private VacancyEntity held(long externalId) {
		return this.repository.findAll()
			.stream()
			.filter((vacancy) -> vacancy.externalId() == externalId)
			.findFirst()
			.orElseThrow();
	}

	private PublishedVacancy published(long externalId) {
		return new PublishedVacancy(externalId, "Backend Engineer", "Remote - Americas", "Engineering",
				"Ship payments.", "https://job-boards.greenhouse.io/stripe/jobs/" + externalId, 15000000L, 20000000L,
				"USD", "Annual Salary", FIRST_PUBLISHED_AT, UPDATED_AT);
	}

}
