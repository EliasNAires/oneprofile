package oneprofile.backend.storage.normalizedvacancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.vacancy.PublishedAshbyVacancy;
import oneprofile.backend.storage.vacancy.PublishedGreenhouseVacancy;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.PublishedWorkdayVacancy;
import oneprofile.backend.storage.vacancy.VacancyStore;
import oneprofile.backend.storage.vacancy.VacancyText;
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
		this.companies.record(AtsEnum.GREENHOUSE, Map.of("stripe", RegionEnum.US));
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001), published(4002)));
	}

	@Test
	void holdsTheCleanedTitleAndEveryLevelItNames() {
		long vacancyId = heldTitles().getFirst().id();

		assertThat(this.normalized.recordCleanedVacancies(
				Map.of(vacancyId, cleaned("Backend Engineer", Set.of(SeniorityLevelEnum.SENIOR)))))
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

		this.normalized.recordCleanedVacancies(Map.of(vacancyId,
				cleaned("Engineer", Set.of(SeniorityLevelEnum.SENIOR, SeniorityLevelEnum.PRINCIPAL))));

		assertThat(this.repository.findAll()).singleElement()
			.extracting(NormalizedVacancyEntity::titleSeniorities)
			.isEqualTo(Set.of(SeniorityLevelEnum.SENIOR, SeniorityLevelEnum.PRINCIPAL));
	}

	@Test
	void replacesWhatTheLastRunRecordedRatherThanAddingToIt() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(
				Map.of(vacancyId, cleaned("Engineer", Set.of(SeniorityLevelEnum.SENIOR))));

		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Backend Engineer", Set.of())));

		assertThat(this.repository.findAll()).singleElement().satisfies((held) -> {
			assertThat(held.cleanedTitle()).isEqualTo("Backend Engineer");
			assertThat(held.titleSeniorities()).isEmpty();
		});
	}

	@Test
	void recordsOneRowPerVacancyOfABatch() {
		Map<Long, CleanedVacancy> batch = Map.of(heldTitles().get(0).id(), cleaned("Backend Engineer", Set.of()),
				heldTitles().get(1).id(), cleaned("Frontend Engineer", Set.of()));

		assertThat(this.normalized.recordCleanedVacancies(batch)).isEqualTo(2);
		assertThat(this.repository.count()).isEqualTo(2);
	}

	@Test
	void letsGoOfWhatWasDerivedFromAVacancyThatHasLeftItsBoard() {
		this.normalized.recordCleanedVacancies(Map.of(heldTitles().getFirst().id(),
				cleaned("Backend Engineer", Set.of(SeniorityLevelEnum.SENIOR))));

		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of());
		this.entityManager.flush();

		assertThat(this.repository.count()).isZero();
	}

	@Test
	void holdsTheSegmentsOfADescriptionForThePileToRead() {
		long vacancyId = heldTitles().getFirst().id();
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, "Requirements", false),
				new Segment(SegmentKindEnum.ITEM, "Requirements", "5+ years of Java", false),
				new Segment(SegmentKindEnum.SENTENCE, null, "We offer equity.", true));
		this.normalized.recordCleanedVacancies(
				Map.of(vacancyId, new CleanedVacancy(new CleanedTitle("Engineer", Set.of()), segments, null)));
		this.normalized.recordClassifications(
				Map.of(vacancyId, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));

		assertThat(this.normalized.pileAfter(0, 10)).singleElement()
			.extracting(PileVacancy::descriptionSegments)
			.isEqualTo(segments);
	}

	@Test
	void writesEachSegmentWithOnlyTheFieldsItHas() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId,
				new CleanedVacancy(new CleanedTitle("Engineer", Set.of()),
						List.of(new Segment(SegmentKindEnum.HEADING, null, "Requirements", false),
								new Segment(SegmentKindEnum.ITEM, "Requirements", "Java", true)),
						null)));
		this.entityManager.flush();

		assertThat(this.entityManager.getEntityManager()
			.createNativeQuery("select description_segments::text from normalized_vacancy")
			.getSingleResult()).isEqualTo("[{\"kind\": \"HEADING\", \"text\": \"Requirements\"}, "
					+ "{\"kind\": \"ITEM\", \"text\": \"Java\", \"under\": \"Requirements\", \"boilerplate\": true}]");
	}

	@Test
	void holdsWhatClassificationMadeOfACleanedTitle() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Backend Engineer", Set.of())));

		assertThat(this.normalized.recordClassifications(Map.of(vacancyId, Classification.in()))).isEqualTo(1);

		assertThat(this.repository.findAll()).singleElement()
			.extracting(NormalizedVacancyEntity::classification)
			.isEqualTo(Classification.in());
	}

	@Test
	void holdsWhyATitleWasLeftUndecided() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Engineer", Set.of())));

		this.normalized.recordClassifications(
				Map.of(vacancyId, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));

		assertThat(this.repository.findAll()).singleElement()
			.extracting(NormalizedVacancyEntity::classification)
			.isEqualTo(Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY));
	}

	@Test
	void replacesWhatTheLastClassificationRunDecided() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Backend Engineer", Set.of())));
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
	void recordsThatTheTitleDecided() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Backend Engineer", Set.of())));

		this.normalized.recordClassifications(Map.of(vacancyId, Classification.in()));

		assertThat(this.repository.findAll()).singleElement()
			.extracting((held) -> held.classification().signal())
			.isEqualTo(ClassificationSignalEnum.TITLE);
	}

	@Test
	void readsThePileWhateverItsStateWithEachDescription() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(Map.of(first, cleaned("Engineer", Set.of()), second,
				cleaned("Product Manager", Set.of())));
		this.normalized.recordClassifications(Map.of(first, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY),
				second, Classification.unknown(UnknownReasonEnum.SCOPE_AMBIGUITY)));
		this.normalized.recordBodyDecisions(Map.of(second, ClassificationStateEnum.IN));

		assertThat(this.normalized.pileAfter(0, 10)).containsExactly(
				new PileVacancy(normalizedId(first), first, "Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, "en", List.of()),
				new PileVacancy(normalizedId(second), second, "Product Manager", UnknownReasonEnum.SCOPE_AMBIGUITY, "en",
						List.of()));
		assertThat(this.normalized.pileAfter(first, 10)).extracting(PileVacancy::vacancyId).containsExactly(second);
	}

	@Test
	void leavesOutOfThePileWhatTheTitleDecidedOrNoRuleReached() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(Map.of(first, cleaned("Backend Engineer", Set.of()), second,
				cleaned("Roboticist", Set.of())));
		this.normalized.recordClassifications(
				Map.of(first, Classification.in(), second, Classification.unknown(UnknownReasonEnum.UNRULED)));

		assertThat(this.normalized.pileAfter(0, 10)).isEmpty();
	}

	@Test
	void recordsWhatTheBodyDecidedKeepingTheTitlesReason() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Engineer", Set.of())));
		this.normalized.recordClassifications(
				Map.of(vacancyId, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));

		assertThat(this.normalized.recordBodyDecisions(Map.of(vacancyId, ClassificationStateEnum.OUT))).isEqualTo(1);

		assertThat(this.repository.findAll()).singleElement()
			.extracting(NormalizedVacancyEntity::classification)
			.isEqualTo(Classification.byBody(ClassificationStateEnum.OUT, UnknownReasonEnum.DOMAIN_AMBIGUITY));
	}

	@Test
	void putsThePileBackToWhatTheTitleLeftItAndNothingElse() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(Map.of(first, cleaned("Engineer", Set.of()), second,
				cleaned("Backend Engineer", Set.of())));
		this.normalized.recordClassifications(Map.of(first,
				Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY), second, Classification.in()));
		this.normalized.recordBodyDecisions(Map.of(first, ClassificationStateEnum.IN));

		assertThat(this.normalized.resetPile()).isEqualTo(1);
		this.entityManager.clear();

		assertThat(this.repository.findByVacancyIdIn(List.of(first, second)))
			.extracting(NormalizedVacancyEntity::vacancyId, NormalizedVacancyEntity::classification)
			.containsExactlyInAnyOrder(
					tuple(first, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)),
					tuple(second, Classification.in()));
	}

	@Test
	void recordsThatTheBodySkippedAVacancyForItsLanguageAndKeepsReadingItWithThePile() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Engineer", Set.of())));
		this.normalized.recordClassifications(
				Map.of(vacancyId, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));

		assertThat(this.normalized.recordUnsupportedLanguage(Set.of(vacancyId))).isEqualTo(1);
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.repository.findAll()).singleElement()
			.extracting(NormalizedVacancyEntity::classification)
			.isEqualTo(Classification.unsupportedLanguage());
		assertThat(this.normalized.pileAfter(0, 10)).extracting(PileVacancy::vacancyId, PileVacancy::titleReason)
			.containsExactly(tuple(vacancyId, UnknownReasonEnum.UNSUPPORTED_LANGUAGE));
		assertThat(this.normalized.resetPile()).isZero();
	}

	@Test
	void readsTheCleanedTitlesHeldAfterOneVacancyInIdOrder() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(Map.of(first, cleaned("Backend Engineer", Set.of()), second,
				cleaned("Frontend Engineer", Set.of())));

		assertThat(this.normalized.cleanedTitlesAfter(0, 10)).containsExactly(
				new ClassificationVacancy(first, "Backend Engineer"), new ClassificationVacancy(second, "Frontend Engineer"));
		assertThat(this.normalized.cleanedTitlesAfter(first, 10))
			.containsExactly(new ClassificationVacancy(second, "Frontend Engineer"));
		assertThat(this.normalized.cleanedTitlesAfter(second, 10)).isEmpty();
	}

	@Test
	void readsForExtractionWhatTheTitleDecidedInAndThePileInALanguageTheBodyPassReads() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001), published(4002),
				published(4003), published(4004), published(4005), published(4006, "fr"), published(4007, "fr")));
		List<Long> ids = this.vacancies.textsAfter(0, 10).stream().map(VacancyText::id).toList();
		Map<Long, CleanedVacancy> cleaned = new HashMap<>();
		ids.forEach((id) -> cleaned.put(id, cleaned("Engineer " + id, Set.of())));
		this.normalized.recordCleanedVacancies(cleaned);
		this.normalized.recordClassifications(Map.of(ids.get(0), Classification.in(), ids.get(1), Classification.out(),
				ids.get(2), Classification.unknown(UnknownReasonEnum.UNRULED), ids.get(3),
				Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY), ids.get(4),
				Classification.unknown(UnknownReasonEnum.SCOPE_AMBIGUITY), ids.get(5), Classification.in(), ids.get(6),
				Classification.unknown(UnknownReasonEnum.SCOPE_AMBIGUITY)));
		this.normalized.recordBodyDecisions(Map.of(ids.get(4), ClassificationStateEnum.OUT));
		this.entityManager.flush();

		assertThat(this.normalized.extractionScopeAfter(0, 10)).extracting(ExtractionVacancy::cleanedTitle)
			.containsExactly("Engineer " + ids.get(0), "Engineer " + ids.get(3), "Engineer " + ids.get(4),
					"Engineer " + ids.get(5));
	}

	@Test
	void readsThePileInTheLanguageCleaningDetectedWhereTheBoardDeclaredNone() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001, null), published(4002, "en")));
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(Map.of(first, detectedIn("de"), second, detectedIn("de")));
		this.normalized.recordClassifications(Map.of(first, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY),
				second, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));

		assertThat(this.normalized.pileAfter(0, 10)).extracting(PileVacancy::language).containsExactly("de", "en");
	}

	@Test
	void readsAnAshbyVacancyOfThePileInTheLanguageCleaningDetected() {
		this.companies.record(AtsEnum.ASHBY, Map.of("ramp", RegionEnum.US));
		this.vacancies.mirror(AtsEnum.ASHBY, "ramp", List.of(new PublishedAshbyVacancy(
				"34413f8d-26bf-4bbc-8ade-eb309a0e2245", "Engineer", "Berlin", null, "Baue Dinge.",
				"https://jobs.ashbyhq.com/ramp/34413f8d-26bf-4bbc-8ade-eb309a0e2245", null, null, null,
				Instant.parse("2026-09-01T14:00:00Z"), "OnSite", "Germany", List.of(), null)));
		long ashby = heldTitles().get(2).id();
		this.normalized.recordCleanedVacancies(Map.of(ashby, detectedIn("de")));
		this.normalized.recordClassifications(Map.of(ashby, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));
		this.entityManager.flush();

		assertThat(this.normalized.pileAfter(0, 10)).extracting(PileVacancy::vacancyId, PileVacancy::language)
			.containsExactly(tuple(ashby, "de"));
		assertThat(this.normalized.extractionScopeAfter(0, 10)).isEmpty();
	}

	@Test
	void leavesOutOfExtractionThePileInALanguageCleaningDetectedTheBodyPassDoesNotRead() {
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001, null), published(4002, null)));
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(Map.of(first, detectedIn("de"), second, detectedIn(null)));
		this.normalized.recordClassifications(Map.of(first, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY),
				second, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));
		this.entityManager.flush();

		assertThat(this.normalized.extractionScopeAfter(0, 10)).extracting(ExtractionVacancy::vacancyId)
			.containsExactly(second);
	}

	@Test
	void leavesOutOfExtractionWhatTheBodyPassSkippedForItsLanguage() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Engineer", Set.of())));
		this.normalized.recordClassifications(
				Map.of(vacancyId, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));
		this.normalized.recordUnsupportedLanguage(Set.of(vacancyId));

		assertThat(this.normalized.extractionScopeAfter(0, 10)).isEmpty();
	}

	@Test
	void walksTheExtractionScopeABatchAtATimeWithEachDescription() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.ITEM, null, "Java", false));
		this.normalized.recordCleanedVacancies(Map.of(first,
				new CleanedVacancy(new CleanedTitle("Backend Engineer", Set.of()), segments, null), second,
				cleaned("Frontend Engineer", Set.of())));
		this.normalized.recordClassifications(Map.of(first, Classification.in(), second, Classification.in()));

		List<ExtractionVacancy> batch = this.normalized.extractionScopeAfter(0, 1);
		assertThat(batch).singleElement().satisfies((vacancy) -> {
			assertThat(vacancy.cleanedTitle()).isEqualTo("Backend Engineer");
			assertThat(vacancy.segments()).isEqualTo(segments);
		});
		assertThat(this.normalized.extractionScopeAfter(batch.getFirst().vacancyId(), 1))
			.extracting(ExtractionVacancy::cleanedTitle)
			.containsExactly("Frontend Engineer");
	}

	@Test
	void readsForTheTitleBreakdownTheEngineeringSubsetWithEachTitleAsPublishedAndItsLocation() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(
				Map.of(first, cleaned("Backend Engineer", Set.of()), second, cleaned("Backend Engineer", Set.of())));
		this.normalized.recordClassifications(Map.of(first, Classification.out(), second, Classification.in()));
		this.entityManager.flush();

		assertThat(this.normalized.breakdownScopeAfter(0, 10)).containsExactly(
				new BreakdownVacancy(normalizedId(second), second, "Senior Backend Engineer", "Remote - Americas"));
	}

	@Test
	void holdsATitleBreakdownBesideTheLevelsCleaningRead() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(
				Map.of(vacancyId, cleaned("Staff Engineering Manager", Set.of(SeniorityLevelEnum.SENIOR))));

		assertThat(this.normalized.recordTitleBreakdowns(Map.of(vacancyId, new TitleBreakdown(List.of("engineering"),
				Set.of(SeniorityLevelEnum.STAFF), ManagementLevelEnum.MANAGER))))
			.isEqualTo(1);

		assertThat(this.repository.findAll()).singleElement().satisfies((held) -> {
			assertThat(held.normalizedTitles()).containsExactly("engineering");
			assertThat(held.managementLevel()).isEqualTo(ManagementLevelEnum.MANAGER);
			assertThat(held.titleSeniorities()).containsExactlyInAnyOrder(SeniorityLevelEnum.SENIOR,
					SeniorityLevelEnum.STAFF);
		});
	}

	@Test
	void letsGoOfEveryTitleBreakdownAndKeepsTheLevelsCleaningRead() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(
				Map.of(vacancyId, cleaned("Staff Engineering Manager", Set.of(SeniorityLevelEnum.SENIOR))));
		this.normalized.recordTitleBreakdowns(Map.of(vacancyId, new TitleBreakdown(List.of("engineering"),
				Set.of(SeniorityLevelEnum.STAFF), ManagementLevelEnum.MANAGER)));
		this.entityManager.flush();

		assertThat(this.normalized.clearTitleBreakdowns()).isEqualTo(1);
		this.entityManager.clear();

		assertThat(this.repository.findAll()).singleElement().satisfies((held) -> {
			assertThat(held.normalizedTitles()).isNull();
			assertThat(held.managementLevel()).isNull();
			assertThat(held.titleSeniorities()).containsExactly(SeniorityLevelEnum.SENIOR);
		});
	}

	@Test
	void readsForTheRoleAssignmentTheNormalizedTitlesOfTheEngineeringSubset() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(
				Map.of(first, cleaned("Backend Engineer", Set.of()), second, cleaned("Backend Engineer", Set.of())));
		this.normalized.recordClassifications(Map.of(first, Classification.out(), second, Classification.in()));
		this.normalized.recordTitleBreakdowns(Map.of(first, breakdown("backend engineer"), second,
				breakdown("data scientist", "machine learning engineer")));
		this.entityManager.flush();

		assertThat(this.normalized.roleScopeAfter(0, 10))
			.containsExactly(new RoleVacancy(second, List.of("data scientist", "machine learning engineer")));
	}

	@Test
	void leavesOutOfTheRoleAssignmentAVacancyTheBreakdownHasNotReached() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Backend Engineer", Set.of())));
		this.normalized.recordClassifications(Map.of(vacancyId, Classification.in()));
		this.entityManager.flush();

		assertThat(this.normalized.roleScopeAfter(0, 10)).isEmpty();
	}

	@Test
	void holdsTheRolesOfAVacancyAndThatAnotherHasNone() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(
				Map.of(first, cleaned("Backend Engineer", Set.of()), second, cleaned("Game Developer", Set.of())));

		assertThat(this.normalized.recordRoles(
				Map.of(first, List.of("data-scientist", "machine-learning-engineer"), second, List.of())))
			.isEqualTo(2);
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.repository.findByVacancyIdIn(List.of(first, second)))
			.extracting(NormalizedVacancyEntity::vacancyId, NormalizedVacancyEntity::roles)
			.containsExactlyInAnyOrder(tuple(first, List.of("data-scientist", "machine-learning-engineer")),
					tuple(second, List.of()));
	}

	@Test
	void letsGoOfEveryVacancysRoles() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Backend Engineer", Set.of())));
		this.normalized.recordRoles(Map.of(vacancyId, List.of("backend-engineer")));
		this.entityManager.flush();

		assertThat(this.normalized.clearRoles()).isEqualTo(1);
		this.entityManager.clear();

		assertThat(this.repository.findAll()).singleElement().extracting(NormalizedVacancyEntity::roles).isNull();
	}

	@Test
	void readsForTheWorkModeTheEngineeringSubsetWithEachLocationAndWorkModeField() {
		this.companies.record(AtsEnum.ASHBY, Map.of("ramp", RegionEnum.US));
		this.vacancies.mirror(AtsEnum.ASHBY, "ramp", List.of(new PublishedAshbyVacancy(
				"34413f8d-26bf-4bbc-8ade-eb309a0e2245", "Engineer", "Berlin", null, "Baue Dinge.",
				"https://jobs.ashbyhq.com/ramp/34413f8d-26bf-4bbc-8ade-eb309a0e2245", null, null, null,
				Instant.parse("2026-09-01T14:00:00Z"), "OnSite", "Germany", List.of(), null)));
		this.companies.record(AtsEnum.WORKDAY, Map.of("nvidia.wd5/nvidiaexternalcareersite", RegionEnum.US));
		this.vacancies.mirror(AtsEnum.WORKDAY, "nvidia.wd5/nvidiaexternalcareersite",
				List.of(new PublishedWorkdayVacancy("RTL-Design-Engineer_JR1", "RTL Design Engineer", "Israel, Yokneam",
						null, "https://nvidia.wd5.myworkdayjobs.com/x/job/RTL-Design-Engineer_JR1", null, "Full time",
						"Hybrid Working", "IL", null, null)));
		List<VacancyText> held = heldTitles();
		Map<Long, CleanedVacancy> cleaned = new HashMap<>();
		Map<Long, Classification> classified = new HashMap<>();
		held.forEach((vacancy) -> {
			cleaned.put(vacancy.id(), cleaned("Engineer", Set.of()));
			classified.put(vacancy.id(), Classification.in());
		});
		classified.put(held.get(1).id(), Classification.out());
		this.normalized.recordCleanedVacancies(cleaned);
		this.normalized.recordClassifications(classified);
		this.entityManager.flush();

		assertThat(this.normalized.workModeScopeAfter(0, 10)).containsExactly(
				new WorkModeVacancy(held.get(0).id(), AtsEnum.GREENHOUSE, "Senior Backend Engineer", "Remote - Americas",
						null),
				new WorkModeVacancy(held.get(2).id(), AtsEnum.ASHBY, "Engineer", "Berlin", "OnSite"),
				new WorkModeVacancy(held.get(3).id(), AtsEnum.WORKDAY, "RTL Design Engineer", "Israel, Yokneam",
						"Hybrid Working"));
	}

	@Test
	void holdsTheWorkModeOfAVacancyAndLetsGoOfEveryWorkMode() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(
				Map.of(first, cleaned("Backend Engineer", Set.of()), second, cleaned("Backend Engineer", Set.of())));

		assertThat(this.normalized.recordWorkModes(Map.of(first, WorkModeEnum.REMOTE))).isEqualTo(1);
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.repository.findByVacancyIdIn(List.of(first, second)))
			.extracting(NormalizedVacancyEntity::vacancyId, NormalizedVacancyEntity::workMode)
			.containsExactlyInAnyOrder(tuple(first, WorkModeEnum.REMOTE), tuple(second, null));

		assertThat(this.normalized.clearWorkModes()).isEqualTo(1);
		this.entityManager.clear();

		assertThat(this.repository.findAll()).extracting(NormalizedVacancyEntity::workMode).containsOnlyNulls();
	}

	@Test
	void readsForTheSeniorityTheEngineeringSubsetWithEveryLevelItsTitleNamesAndItsSegments() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.ITEM, null, "5+ years of Java", false));
		this.normalized.recordCleanedVacancies(Map.of(first,
				new CleanedVacancy(new CleanedTitle("Engineer", Set.of(SeniorityLevelEnum.SENIOR)), segments, null),
				second, cleaned("Engineer", Set.of())));
		this.normalized.recordClassifications(Map.of(first, Classification.in(), second, Classification.out()));
		this.normalized.recordTitleBreakdowns(Map.of(first,
				new TitleBreakdown(List.of("engineer"), Set.of(SeniorityLevelEnum.II), null)));
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.normalized.seniorityScopeAfter(0, 10)).containsExactly(
				new SeniorityVacancy(first, Set.of(SeniorityLevelEnum.SENIOR, SeniorityLevelEnum.II), segments));
	}

	@Test
	void walksTheSeniorityScopeABatchAtATime() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(
				Map.of(first, cleaned("Engineer", Set.of()), second, cleaned("Engineer", Set.of())));
		this.normalized.recordClassifications(Map.of(first, Classification.in(), second, Classification.in()));
		this.entityManager.flush();

		assertThat(this.normalized.seniorityScopeAfter(0, 1)).extracting(SeniorityVacancy::vacancyId)
			.containsExactly(first);
		assertThat(this.normalized.seniorityScopeAfter(first, 1)).extracting(SeniorityVacancy::vacancyId)
			.containsExactly(second);
	}

	@Test
	void holdsTheSeniorityOfAVacancyAndLetsGoOfEverySeniority() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(
				Map.of(first, cleaned("Engineer", Set.of()), second, cleaned("Engineer", Set.of())));
		List<ExperienceRequirement> requirements = List.of(new ExperienceRequirement(0, 0, 3, 5, true),
				new ExperienceRequirement(2, 10, 2, null, false));

		assertThat(this.normalized.recordSeniorities(
				Map.of(first, new VacancySeniority(SeniorityLevelEnum.MID, 3, requirements))))
			.isEqualTo(1);
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.repository.findByVacancyIdIn(List.of(first, second)))
			.extracting(NormalizedVacancyEntity::vacancyId, NormalizedVacancyEntity::seniority)
			.containsExactlyInAnyOrder(tuple(first, new VacancySeniority(SeniorityLevelEnum.MID, 3, requirements)),
					tuple(second, new VacancySeniority(null, null, List.of())));

		assertThat(this.normalized.clearSeniorities()).isEqualTo(1);
		this.entityManager.clear();

		assertThat(this.repository.findAll()).extracting(NormalizedVacancyEntity::seniority)
			.containsOnly(new VacancySeniority(null, null, List.of()));
	}

	private static TitleBreakdown breakdown(String... normalizedTitles) {
		return new TitleBreakdown(List.of(normalizedTitles), Set.of(), null);
	}

	private long normalizedId(long vacancyId) {
		this.entityManager.flush();
		return ((Number) this.entityManager.getEntityManager()
			.createNativeQuery("select id from normalized_vacancy where vacancy_id = :vacancyId")
			.setParameter("vacancyId", vacancyId)
			.getSingleResult()).longValue();
	}

	private List<VacancyText> heldTitles() {
		return this.vacancies.textsAfter(0, 10);
	}

	private static CleanedVacancy cleaned(String title, Set<SeniorityLevelEnum> titleSeniorities) {
		return new CleanedVacancy(new CleanedTitle(title, titleSeniorities), List.of(), null);
	}

	private static CleanedVacancy detectedIn(String language) {
		return new CleanedVacancy(new CleanedTitle("Engineer", Set.of()), List.of(), language);
	}

	private PublishedVacancy published(long externalId) {
		return published(externalId, "en");
	}

	private PublishedVacancy published(long externalId, String language) {
		return new PublishedGreenhouseVacancy(String.valueOf(externalId), "Senior Backend Engineer", "Remote - Americas",
				"Engineering", "Ship payments.", "https://job-boards.greenhouse.io/stripe/jobs/" + externalId, null, null,
				null, Instant.parse("2026-09-01T14:00:00Z"), null, Instant.parse("2026-09-18T16:30:00Z"), language);
	}

}
