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
		assertThat(held.language()).isEqualTo("en");
		assertThat(held.url()).isEqualTo("https://job-boards.greenhouse.io/stripe/jobs/4001");
		assertThat(held.payMinCents()).isEqualTo(15000000L);
		assertThat(held.payMaxCents()).isEqualTo(20000000L);
		assertThat(held.payCurrency()).isEqualTo("USD");
		assertThat(held.payTitle()).isEqualTo("Annual Salary");
		assertThat(held.firstPublishedAt()).isEqualTo(FIRST_PUBLISHED_AT);
		assertThat(held.updatedAt()).isEqualTo(UPDATED_AT);
	}

	@Test
	void holdsWhatAnAtsPublishesAsStructuredFieldsAndAnIdThatIsNoNumber() {
		this.companies.record(AtsEnum.ASHBY, List.of("ramp"));
		PublishedVacancy published = new PublishedVacancy("34413f8d-26bf-4bbc-8ade-eb309a0e2245",
				"Security Engineer, Cloud", "New York, NY (HQ)", "Engineering", "Secure the cloud.", null,
				"https://jobs.ashbyhq.com/ramp/34413f8d-26bf-4bbc-8ade-eb309a0e2245", 21140000L, 29060000L, "USD", null,
				"1 YEAR", FIRST_PUBLISHED_AT, null, "Hybrid", "USA",
				List.of(new SecondaryLocation("Remote (Canada)", "Canada"), new SecondaryLocation("Anywhere", null)));

		this.vacancies.mirror(AtsEnum.ASHBY, "ramp", List.of(published));

		VacancyEntity held = this.repository.findAll().getFirst();
		assertThat(held.externalId()).isEqualTo("34413f8d-26bf-4bbc-8ade-eb309a0e2245");
		assertThat(held.payInterval()).isEqualTo("1 YEAR");
		assertThat(held.workplaceType()).isEqualTo("Hybrid");
		assertThat(held.addressCountry()).isEqualTo("USA");
		assertThat(held.secondaryLocations()).containsExactly(new SecondaryLocation("Remote (Canada)", "Canada"),
				new SecondaryLocation("Anywhere", null));
	}

	@Test
	void holdsNothingForTheFieldsAnAtsDoesNotProvide() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001)));

		VacancyEntity held = held(4001);
		assertThat(held.payInterval()).isNull();
		assertThat(held.workplaceType()).isNull();
		assertThat(held.addressCountry()).isNull();
		assertThat(held.secondaryLocations()).isNull();
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

		PublishedVacancy renamed = new PublishedVacancy("4001", "Staff Backend Engineer", "Remote - Americas",
				"Engineering", "Ship payments.", "en", "https://job-boards.greenhouse.io/stripe/jobs/4001", 15000000L,
				20000000L, "USD", "Annual Salary", null, FIRST_PUBLISHED_AT, UPDATED_AT.plusSeconds(3600), null, null, null);
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
			.isEqualTo("4002");
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
	void readsTheTitlesAndDescriptionsOfTheCorpusInIdOrderABatchAtATime() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001), published(4002)));

		List<VacancyText> first = this.vacancies.textsAfter(0, 1);

		assertThat(first).singleElement()
			.extracting(VacancyText::title, VacancyText::description)
			.containsExactly("Backend Engineer", "Ship payments.");
		assertThat(this.vacancies.textsAfter(first.getFirst().id(), 10)).hasSize(1);
	}

	@Test
	void readsNoTextsOnceThereAreNoneLeft() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001)));
		List<VacancyText> all = this.vacancies.textsAfter(0, 10);

		assertThat(this.vacancies.textsAfter(all.getLast().id(), 10)).isEmpty();
	}

	private VacancyEntity held(long externalId) {
		return this.repository.findAll()
			.stream()
			.filter((vacancy) -> vacancy.externalId().equals(String.valueOf(externalId)))
			.findFirst()
			.orElseThrow();
	}

	private PublishedVacancy published(long externalId) {
		return new PublishedVacancy(String.valueOf(externalId), "Backend Engineer", "Remote - Americas", "Engineering",
				"Ship payments.", "en", "https://job-boards.greenhouse.io/stripe/jobs/" + externalId, 15000000L, 20000000L,
				"USD", "Annual Salary", null, FIRST_PUBLISHED_AT, UPDATED_AT, null, null, null);
	}

}
