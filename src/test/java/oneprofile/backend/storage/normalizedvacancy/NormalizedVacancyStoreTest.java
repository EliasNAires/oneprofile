package oneprofile.backend.storage.normalizedvacancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
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
		this.companies.record(AtsEnum.GREENHOUSE, List.of("stripe"));
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
				Map.of(vacancyId, new CleanedVacancy(new CleanedTitle("Engineer", Set.of()), segments)));
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
								new Segment(SegmentKindEnum.ITEM, "Requirements", "Java", true)))));
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
				new PileVacancy(first, "Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, "en", List.of()),
				new PileVacancy(second, "Product Manager", UnknownReasonEnum.SCOPE_AMBIGUITY, "en", List.of()));
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
	void readsTheSegmentsOfWhatTheTitleKeptInAndOfThePileWhateverTheBodyDecided() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.ITEM, null, "Build APIs in Go.", false));
		this.normalized.recordCleanedVacancies(Map.of(first, new CleanedVacancy(new CleanedTitle("Backend Engineer",
				Set.of()), segments), second, cleaned("Engineer", Set.of())));
		this.normalized.recordClassifications(Map.of(first, Classification.in(), second,
				Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));
		this.normalized.recordBodyDecisions(Map.of(second, ClassificationStateEnum.OUT));

		assertThat(this.normalized.inAndPileAfter(0, 10)).containsExactly(new SegmentedVacancy(first, segments),
				new SegmentedVacancy(second, List.of()));
		assertThat(this.normalized.inAndPileAfter(first, 10)).extracting(SegmentedVacancy::vacancyId)
			.containsExactly(second);
	}

	@Test
	void readsNoSegmentsOfWhatTheTitleKeptOutOrNoRuleReached() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(Map.of(first, cleaned("Account Executive", Set.of()), second,
				cleaned("Roboticist", Set.of())));
		this.normalized.recordClassifications(
				Map.of(first, Classification.out(), second, Classification.unknown(UnknownReasonEnum.UNRULED)));

		assertThat(this.normalized.inAndPileAfter(0, 10)).isEmpty();
	}

	@Test
	void readsTheSegmentsOfAVacancyOfThePileTheBodySkippedForItsLanguage() {
		long vacancyId = heldTitles().getFirst().id();
		this.normalized.recordCleanedVacancies(Map.of(vacancyId, cleaned("Engineer", Set.of())));
		this.normalized.recordClassifications(
				Map.of(vacancyId, Classification.unknown(UnknownReasonEnum.DOMAIN_AMBIGUITY)));
		this.normalized.recordUnsupportedLanguage(Set.of(vacancyId));

		assertThat(this.normalized.inAndPileAfter(0, 10)).extracting(SegmentedVacancy::vacancyId)
			.containsExactly(vacancyId);
	}

	@Test
	void readsTheCleanedTitlesHeldAfterOneVacancyInIdOrder() {
		long first = heldTitles().get(0).id();
		long second = heldTitles().get(1).id();
		this.normalized.recordCleanedVacancies(Map.of(first, cleaned("Backend Engineer", Set.of()), second,
				cleaned("Frontend Engineer", Set.of())));

		assertThat(this.normalized.cleanedTitlesAfter(0, 10)).containsExactly(
				new NormalizedTitle(first, "Backend Engineer"), new NormalizedTitle(second, "Frontend Engineer"));
		assertThat(this.normalized.cleanedTitlesAfter(first, 10))
			.containsExactly(new NormalizedTitle(second, "Frontend Engineer"));
		assertThat(this.normalized.cleanedTitlesAfter(second, 10)).isEmpty();
	}

	private List<VacancyText> heldTitles() {
		return this.vacancies.textsAfter(0, 10);
	}

	private static CleanedVacancy cleaned(String title, Set<SeniorityLevelEnum> titleSeniorities) {
		return new CleanedVacancy(new CleanedTitle(title, titleSeniorities), List.of());
	}

	private PublishedVacancy published(long externalId) {
		return new PublishedVacancy(externalId, "Senior Backend Engineer", "Remote - Americas", "Engineering",
				"Ship payments.", "en", "https://job-boards.greenhouse.io/stripe/jobs/" + externalId, null, null, null, null,
				Instant.parse("2026-09-01T14:00:00Z"), Instant.parse("2026-09-18T16:30:00Z"));
	}

}
