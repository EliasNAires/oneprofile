package oneprofile.backend.storage.vacancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.company.CompanyStore;
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

	@Autowired
	private TestEntityManager entityManager;

	@BeforeEach
	void holdTheCompany() {
		this.companies.record(AtsEnum.GREENHOUSE, Map.of("stripe", RegionEnum.US, "notion", RegionEnum.US));
	}

	@Test
	void holdsWhatTheBoardPublishes() {
		assertThat(this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001), published(4002))))
			.isEqualTo(new VacancyStore.Reconciliation(2, 0, 0));

		GreenhouseVacancyEntity held = (GreenhouseVacancyEntity) held(4001);
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
	void holdsWhatAshbyPublishesOnAnAshbyVacancyUnderAnIdThatIsNoNumber() {
		this.companies.record(AtsEnum.ASHBY, Map.of("ramp", RegionEnum.US));
		PublishedAshbyVacancy published = new PublishedAshbyVacancy("34413f8d-26bf-4bbc-8ade-eb309a0e2245",
				"Security Engineer, Cloud", "New York, NY (HQ)", "Engineering", "Secure the cloud.",
				"https://jobs.ashbyhq.com/ramp/34413f8d-26bf-4bbc-8ade-eb309a0e2245", 21140000L, 29060000L, "USD",
				FIRST_PUBLISHED_AT, "Hybrid", "USA",
				List.of(new SecondaryLocation("Remote (Canada)", "Canada"), new SecondaryLocation("Anywhere", null)),
				"1 YEAR");

		this.vacancies.mirror(AtsEnum.ASHBY, "ramp", List.of(published));

		assertThat(this.repository.findAll()).singleElement().isInstanceOfSatisfying(AshbyVacancyEntity.class, (held) -> {
			assertThat(held.externalId()).isEqualTo("34413f8d-26bf-4bbc-8ade-eb309a0e2245");
			assertThat(held.payMinCents()).isEqualTo(21140000L);
			assertThat(held.payInterval()).isEqualTo("1 YEAR");
			assertThat(held.workplaceType()).isEqualTo("Hybrid");
			assertThat(held.addressCountry()).isEqualTo("USA");
			assertThat(held.secondaryLocations()).containsExactly(new SecondaryLocation("Remote (Canada)", "Canada"),
					new SecondaryLocation("Anywhere", null));
		});
		assertThat(atsHeld()).containsExactly("ASHBY");
	}

	@Test
	void holdsWhatLeverPublishesOnALeverVacancy() {
		this.companies.record(AtsEnum.LEVER, Map.of("finix", RegionEnum.US));
		PublishedLeverVacancy published = new PublishedLeverVacancy("d2907c20-a739-4c0b-a10e-94aaf1f72c78",
				"Account Executive, Direct Merchant", "San Francisco", "Revenue", "Finix is building payments.",
				"https://jobs.lever.co/finix/d2907c20-a739-4c0b-a10e-94aaf1f72c78", 14000000L, 18000000L, "USD",
				FIRST_PUBLISHED_AT, "remote", "US", "Full-time", "Sales", List.of("San Francisco", "Toronto"),
				"per-year-salary");

		this.vacancies.mirror(AtsEnum.LEVER, "finix", List.of(published));

		assertThat(this.repository.findAll()).singleElement()
			.isInstanceOfSatisfying(LeverVacancyEntity.class, (held) -> {
				assertThat(held.externalId()).isEqualTo("d2907c20-a739-4c0b-a10e-94aaf1f72c78");
				assertThat(held.department()).isEqualTo("Revenue");
				assertThat(held.payMinCents()).isEqualTo(14000000L);
				assertThat(held.workplaceType()).isEqualTo("remote");
				assertThat(held.country()).isEqualTo("US");
				assertThat(held.commitment()).isEqualTo("Full-time");
				assertThat(held.team()).isEqualTo("Sales");
				assertThat(held.allLocations()).containsExactly("San Francisco", "Toronto");
				assertThat(held.payInterval()).isEqualTo("per-year-salary");
			});
		assertThat(atsHeld()).containsExactly("LEVER");
	}

	@Test
	void holdsWhatWorkdayPublishesOnAWorkdayVacancy() {
		this.companies.record(AtsEnum.WORKDAY, Map.of("nvidia.wd5/nvidiaexternalcareersite", RegionEnum.US));
		PublishedWorkdayVacancy published = new PublishedWorkdayVacancy("RTL-Design-Engineer--DFT_JR2028303",
				"RTL Design Engineer, DFT", "Israel, Yokneam", "Design the DFT logic.",
				"https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite/job/Israel-Yokneam/RTL-Design-Engineer--DFT_JR2028303",
				FIRST_PUBLISHED_AT, "Full time", "IL", List.of("Israel, Yokneam", "Israel, Tel Aviv"), "NVIDIA Israel");

		this.vacancies.mirror(AtsEnum.WORKDAY, "nvidia.wd5/nvidiaexternalcareersite", List.of(published));

		assertThat(this.repository.findAll()).singleElement()
			.isInstanceOfSatisfying(WorkdayVacancyEntity.class, (held) -> {
				assertThat(held.externalId()).isEqualTo("RTL-Design-Engineer--DFT_JR2028303");
				assertThat(held.location()).isEqualTo("Israel, Yokneam");
				assertThat(held.department()).isNull();
				assertThat(held.payMinCents()).isNull();
				assertThat(held.timeType()).isEqualTo("Full time");
				assertThat(held.country()).isEqualTo("IL");
				assertThat(held.allLocations()).containsExactly("Israel, Yokneam", "Israel, Tel Aviv");
			});
		assertThat(atsHeld()).containsExactly("WORKDAY");
	}

	@Test
	void holdsAGreenhouseVacancyUnderItsAts() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001)));

		assertThat(held(4001)).isInstanceOf(GreenhouseVacancyEntity.class);
		assertThat(atsHeld()).containsExactly("GREENHOUSE");
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

		PublishedVacancy renamed = new PublishedGreenhouseVacancy("4001", "Staff Backend Engineer", "Remote - Americas",
				"Engineering", "Ship payments.", "https://job-boards.greenhouse.io/stripe/jobs/4001", 15000000L, 20000000L,
				"USD", FIRST_PUBLISHED_AT, "Annual Salary", UPDATED_AT.plusSeconds(3600), "en");
		assertThat(this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(renamed)))
			.isEqualTo(new VacancyStore.Reconciliation(0, 1, 0));

		GreenhouseVacancyEntity held = (GreenhouseVacancyEntity) held(4001);
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
	void readsTheTextsOfEveryAtssVacanciesWithTheLanguageOnlyGreenhouseDeclares() {
		this.companies.record(AtsEnum.ASHBY, Map.of("ramp", RegionEnum.US));
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001)));
		this.vacancies.mirror(AtsEnum.ASHBY, "ramp", List.of(new PublishedAshbyVacancy(
				"34413f8d-26bf-4bbc-8ade-eb309a0e2245", "Security Engineer", "New York, NY (HQ)", null,
				"Secure the cloud.", "https://jobs.ashbyhq.com/ramp/34413f8d-26bf-4bbc-8ade-eb309a0e2245", null, null,
				null, FIRST_PUBLISHED_AT, null, null, List.of(), null)));

		assertThat(this.vacancies.textsAfter(0, 10)).extracting(VacancyText::title, VacancyText::language)
			.containsExactly(tuple("Backend Engineer", "en"), tuple("Security Engineer", null));
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

	private List<String> atsHeld() {
		return this.entityManager.getEntityManager()
			.createNativeQuery("select ats from vacancy", String.class)
			.getResultList();
	}

	private PublishedVacancy published(long externalId) {
		return new PublishedGreenhouseVacancy(String.valueOf(externalId), "Backend Engineer", "Remote - Americas",
				"Engineering", "Ship payments.", "https://job-boards.greenhouse.io/stripe/jobs/" + externalId, 15000000L,
				20000000L, "USD", FIRST_PUBLISHED_AT, "Annual Salary", UPDATED_AT, "en");
	}

}
