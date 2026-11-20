package oneprofile.backend.workers.sweep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.PublishedWorkdayVacancy;
import oneprofile.backend.storage.vacancy.VacancyStore;
import oneprofile.backend.workers.WorkdayApiPort;
import oneprofile.backend.workers.classification.TitleClassificationRule;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockMakers;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Workday is a miniature that pages 2 vacancies at a time and stops paging at 4, as the real one pages
 * 20 at a time and stops at 2000: past it, it answers page one again, and gives a {@code total} no
 * larger than 4, on the first page only.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorkdayVacancyReaderAdapterTest {

	private static final String SLUG = "nvidia.wd5/nvidiaexternalcareersite";

	private static final String DETAIL = """
			{"jobPostingInfo":{"title":"Senior Software Engineer",
			"jobDescription":"<p><b>What you'll be doing:</b></p><ul><li>Build the NVLink firmware.</li></ul>",
			"location":"US, CA, Santa Clara","additionalLocations":["Israel, Tel Aviv"],"startDate":"2026-10-23",
			"timeType":"Full time","remoteType":"Hybrid","jobReqId":"JR1",
			"jobRequisitionLocation":{"descriptor":"US, CA, Santa Clara","country":{"alpha2Code":"US"}},
			"externalUrl":"https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite/job/US-CA-Santa-Clara/Senior-Software-Engineer_JR1"},
			"hiringOrganization":{"name":"2100 NVIDIA USA"}}""";

	private final JsonMapper json = JsonMapper.builder().build();

	private final List<String> asked = new ArrayList<>();

	private final VacancyStore vacancies = mock(VacancyStore.class, withSettings().mockMaker(MockMakers.SUBCLASS));

	/** In a short sweep, a described vacancy with the same identity and title is returned with everything held. */
	@Test
	void returnsAHeldVacancyWithoutReadingItsPageInAShortSweep(CapturedOutput output) throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(posting("Senior Software Engineer", "Santa-Clara", "JR1")));
		PublishedWorkdayVacancy held = new PublishedWorkdayVacancy("Senior-Software-Engineer_JR1",
				"Senior Software Engineer", "Held location", "Held description", "https://example.com/held",
				Instant.parse("2026-10-20T00:00:00Z"), "Full time", "Remote", "US", List.of("Held location"), null);
		given(this.vacancies.describedWorkdayVacancies(SLUG)).willAnswer((invocation) -> Map.of(held.externalId(), held));

		assertThat(reader(workday).readShort(SLUG, RegionEnum.US, () -> {
		})).containsExactly(held);
		assertThat(this.asked).containsExactly("jobs {} 0");
		assertThat(output.getOut().lines().filter((line) -> line.contains("WORKDAY_BOARD "))).singleElement()
			.asString()
			.endsWith("WORKDAY_BOARD slug=" + SLUG
					+ " list_pages=1 description_pages=0 descriptions_held=1 parts=1 read=1 size=1 failed=false");
	}

	/** In a short sweep, a matching identity does not reuse a description when the listed title differs. */
	@Test
	void readsThePageWhenTheHeldTitleDiffersInAShortSweep() throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(posting("Senior Software Engineer", "Santa-Clara", "JR1")));
		workday.details.put("/job/Santa-Clara/Senior-Software-Engineer_JR1", DETAIL);
		PublishedWorkdayVacancy held = new PublishedWorkdayVacancy("Senior-Software-Engineer_JR1",
				"Software Engineer", "Held location", "Held description", "https://example.com/held",
				null, null, null, null, null, null);
		given(this.vacancies.describedWorkdayVacancies(SLUG)).willAnswer((invocation) -> Map.of(held.externalId(), held));

		assertThat(reader(workday).readShort(SLUG, RegionEnum.US, () -> {
		})).singleElement().extracting(PublishedVacancy::description).isEqualTo("""
				# What you'll be doing:
				- Build the NVLink firmware.""");
		assertThat(this.asked).containsExactly("jobs {} 0", "job /job/Santa-Clara/Senior-Software-Engineer_JR1");
	}

	/** A long sweep reads the page of a vacancy held with its description, without asking what is held. */
	@Test
	void readsThePageOfAHeldVacancyInALongSweep(CapturedOutput output) throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(posting("Senior Software Engineer", "Santa-Clara", "JR1")));
		workday.details.put("/job/Santa-Clara/Senior-Software-Engineer_JR1", DETAIL);

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).singleElement().extracting(PublishedVacancy::description).isEqualTo("""
				# What you'll be doing:
				- Build the NVLink firmware.""");
		assertThat(this.asked).containsExactly("jobs {} 0", "job /job/Santa-Clara/Senior-Software-Engineer_JR1");
		then(this.vacancies).shouldHaveNoInteractions();
		assertThat(output.getOut()).contains("list_pages=1 description_pages=1 descriptions_held=0 parts=1 read=1 size=1 failed=false");
	}

	/** An unnarrowed board reports its list and description requests once. */
	@Test
	void readsTheDescriptionOfAVacancyWhoseTitleIsAnEngineeringRole(CapturedOutput output) throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(posting("Senior Software Engineer", "US-CA-Santa-Clara", "JR1")));
		workday.details.put("/job/US-CA-Santa-Clara/Senior-Software-Engineer_JR1", DETAIL);

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).containsExactly(new PublishedWorkdayVacancy("Senior-Software-Engineer_JR1", "Senior Software Engineer",
				"US, CA, Santa Clara", """
						# What you'll be doing:
						- Build the NVLink firmware.""",
				"https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite/job/US-CA-Santa-Clara/Senior-Software-Engineer_JR1",
				Instant.parse("2026-10-23T00:00:00Z"), "Full time", "Hybrid", "US",
				List.of("US, CA, Santa Clara", "Israel, Tel Aviv"), "2100 NVIDIA USA"));
		assertThat(this.asked).containsExactly("jobs {} 0", "job /job/US-CA-Santa-Clara/Senior-Software-Engineer_JR1");
		assertThat(output.getOut().lines().filter((line) -> line.contains("WORKDAY_BOARD "))).singleElement()
			.asString()
			.endsWith("WORKDAY_BOARD slug=" + SLUG
					+ " list_pages=1 description_pages=1 descriptions_held=0 parts=1 read=1 size=1 failed=false");
	}

	@Test
	void readsTheDescriptionOnlyWhereTheTitleIsInOrLeftUnknownForTheBodyPassToRead() throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(posting("Senior Software Engineer", "Santa-Clara", "JR1"),
				posting("Product Manager", "Santa-Clara", "JR2"), posting("Systems Engineer", "Santa-Clara", "JR3"),
				posting("Registered Nurse", "Santa-Clara", "JR4")));
		for (String path : List.of("/job/Santa-Clara/Senior-Software-Engineer_JR1", "/job/Santa-Clara/Product-Manager_JR2",
				"/job/Santa-Clara/Systems-Engineer_JR3", "/job/Santa-Clara/Registered-Nurse_JR4")) {
			workday.details.put(path, DETAIL);
		}

		List<PublishedVacancy> read = reader(workday).read(SLUG, RegionEnum.US, () -> {
		});

		assertThat(this.asked).filteredOn((request) -> request.startsWith("job "))
			.containsExactly("job /job/Santa-Clara/Senior-Software-Engineer_JR1",
					"job /job/Santa-Clara/Product-Manager_JR2", "job /job/Santa-Clara/Systems-Engineer_JR3");
		assertThat(read).extracting(PublishedVacancy::externalId)
			.containsExactly("Senior-Software-Engineer_JR1", "Product-Manager_JR2", "Systems-Engineer_JR3",
					"Registered-Nurse_JR4");
	}

	@Test
	void holdsAVacancyWhoseDescriptionIsNotReadWithWhatTheBoardsListSays() throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(posting("PCT Med Surg Unit", "Santa-Clara", "JR9")));

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).containsExactly(new PublishedWorkdayVacancy("PCT-Med-Surg-Unit_JR9", "PCT Med Surg Unit", "Santa-Clara",
				null, "https://nvidia.wd5.myworkdayjobs.com/nvidiaexternalcareersite/job/Santa-Clara/PCT-Med-Surg-Unit_JR9",
				null, null, null, null, null, null));
		assertThat(this.asked).noneMatch((request) -> request.startsWith("job "));
	}

	@Test
	void pagesThroughABoardThatGivesItsTotalOnTheFirstPageOnly() throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(posting("Registered Nurse", "A", "JR1"),
				posting("Registered Nurse", "B", "JR2"), posting("Registered Nurse", "C", "JR3")));

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(3);
		assertThat(this.asked).containsExactly("jobs {} 0", "jobs {} 2");
	}

	/** Only the parts actually paged count, while their discovery page is still a list request. */
	@Test
	void readsABoardTooLargeToPageWholeInPartsByTheFacetThatSplitsItUnderTheLimit(CapturedOutput output)
			throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(
				posting("Registered Nurse", "A", "JR1", Map.of("jobFamilyGroup", Set.of("care"), "locations", Set.of("a", "b"))),
				posting("Registered Nurse", "B", "JR2", Map.of("jobFamilyGroup", Set.of("care"), "locations", Set.of("a", "b"))),
				posting("Registered Nurse", "C", "JR3", Map.of("jobFamilyGroup", Set.of("care"), "locations", Set.of("a"))),
				posting("Store Associate", "D", "JR4", Map.of("jobFamilyGroup", Set.of("retail"), "locations", Set.of("a"))),
				posting("Store Associate", "E", "JR5", Map.of("jobFamilyGroup", Set.of("retail"), "locations", Set.of("b")))));

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).extracting(PublishedVacancy::externalId)
			.containsExactlyInAnyOrder("Registered-Nurse_JR1", "Registered-Nurse_JR2", "Registered-Nurse_JR3",
					"Store-Associate_JR4", "Store-Associate_JR5");
		assertThat(this.asked).contains("jobs {jobFamilyGroup=[care]} 0", "jobs {jobFamilyGroup=[care]} 2",
				"jobs {jobFamilyGroup=[retail]} 0");
		assertThat(this.asked).noneMatch((request) -> request.contains("locations"));
		assertThat(output.getOut().lines().filter((line) -> line.contains("WORKDAY_BOARD "))).singleElement()
			.asString()
			.endsWith("WORKDAY_BOARD slug=" + SLUG
					+ " list_pages=4 description_pages=0 descriptions_held=0 parts=2 read=5 size=5 failed=false");
	}

	/** Among covering facets, three pages beat five single-vacancy pages. */
	@Test
	void narrowsByTheFacetThatTakesTheFewestPages() throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(
				posting("Registered Nurse", "A", "JR1", Map.of("timeType", Set.of("full"), "locations", Set.of("a"))),
				posting("Registered Nurse", "B", "JR2", Map.of("timeType", Set.of("full"), "locations", Set.of("b"))),
				posting("Registered Nurse", "C", "JR3", Map.of("timeType", Set.of("full"), "locations", Set.of("c"))),
				posting("Registered Nurse", "D", "JR4", Map.of("timeType", Set.of("part"), "locations", Set.of("d"))),
				posting("Registered Nurse", "E", "JR5", Map.of("timeType", Set.of("part"), "locations", Set.of("e")))));

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(5);
		assertThat(this.asked).containsExactly("jobs {} 0", "jobs {timeType=[full]} 0",
				"jobs {timeType=[full]} 2", "jobs {timeType=[part]} 0");
	}

	@Test
	void narrowsAPartStillTooLargeByAFurtherFacetAndHoldsAVacancyInTwoPartsOnce() throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(
				posting("Registered Nurse", "A", "JR1", Map.of("timeType", Set.of("full"), "locations", Set.of("a"))),
				posting("Registered Nurse", "B", "JR2", Map.of("timeType", Set.of("full"), "locations", Set.of("a"))),
				posting("Registered Nurse", "C", "JR3", Map.of("timeType", Set.of("full"), "locations", Set.of("a", "b"))),
				posting("Registered Nurse", "D", "JR4", Map.of("timeType", Set.of("full"), "locations", Set.of("b"))),
				posting("Registered Nurse", "E", "JR5", Map.of("timeType", Set.of("part"), "locations", Set.of("a")))));

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).extracting(PublishedVacancy::externalId)
			.containsExactly("Registered-Nurse_JR1", "Registered-Nurse_JR2", "Registered-Nurse_JR3",
					"Registered-Nurse_JR4", "Registered-Nurse_JR5");
		assertThat(this.asked).contains("jobs {timeType=[full], locations=[a]} 0",
				"jobs {timeType=[full], locations=[b]} 0", "jobs {timeType=[part]} 0");
	}

	/** A closed vacancy is left out while the rest of its board is still read. */
	@Test
	void leavesOutAVacancyThatClosedBetweenTheListAndItsPage() throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(posting("Senior Software Engineer", "Santa-Clara", "JR1"),
				posting("Registered Nurse", "Santa-Clara", "JR2")));

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).extracting(PublishedVacancy::externalId).containsExactly("Registered-Nurse_JR2");
	}

	@Test
	void holdsAVacancyWhosePageWorkdayWillNotShowWithWhatTheBoardsListSays() throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(posting("Senior Software Engineer", "Santa-Clara", "JR1")));
		workday.detailStatus = 403;

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).containsExactly(new PublishedWorkdayVacancy("Senior-Software-Engineer_JR1", "Senior Software Engineer",
				"Santa-Clara", null,
				"https://nvidia.wd5.myworkdayjobs.com/nvidiaexternalcareersite/job/Santa-Clara/Senior-Software-Engineer_JR1",
				null, null, null, null, null, null));
	}

	/** These answers on the first unfaceted page mean no public board is published. */
	@ParameterizedTest
	@ValueSource(ints = { 400, 401, 403, 404, 422 })
	void reportsABoardThatIsNotPublished(int status, CapturedOutput output) {
		FakeWorkday workday = new FakeWorkday(List.of());
		workday.listStatus = status;

		assertThatIOException().isThrownBy(() -> reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).isInstanceOf(BoardNotFoundException.class);
		assertThat(output.getOut().lines().filter((line) -> line.contains("WORKDAY_BOARD "))).singleElement()
			.asString()
			.endsWith("WORKDAY_BOARD slug=" + SLUG
					+ " list_pages=1 description_pages=0 descriptions_held=0 parts=1 read=0 size=- failed=true");
	}

	/** A server error on the first page says nothing about whether a board is published. */
	@Test
	void failsOnABoardThatCannotBeRead(CapturedOutput output) {
		FakeWorkday workday = new FakeWorkday(List.of());
		workday.listStatus = 500;

		assertThatIOException().isThrownBy(() -> reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).isNotInstanceOf(BoardNotFoundException.class);
		assertThat(output.getOut().lines().filter((line) -> line.contains("WORKDAY_BOARD "))).singleElement()
			.asString()
			.endsWith("WORKDAY_BOARD slug=" + SLUG
					+ " list_pages=1 description_pages=0 descriptions_held=0 parts=1 read=0 size=- failed=true");
	}

	/** A later page cannot establish that the board is not published. */
	@Test
	void failsOnALaterPageThatCannotBeRead() {
		FakeWorkday workday = new FakeWorkday(List.of(posting("Registered Nurse", "A", "JR1"),
				posting("Registered Nurse", "B", "JR2"), posting("Registered Nurse", "C", "JR3")));
		workday.laterListStatus = 404;

		assertThatIOException().isThrownBy(() -> reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).isNotInstanceOf(BoardNotFoundException.class);
		assertThat(this.asked).containsExactly("jobs {} 0", "jobs {} 2");
	}

	/** Even offset zero under a facet cannot establish that the board is not published. */
	@Test
	void failsOnAFacetedPageThatCannotBeRead() {
		FakeWorkday workday = new FakeWorkday(List.of(
				posting("Registered Nurse", "A", "JR1", Map.of("timeType", Set.of("full"))),
				posting("Registered Nurse", "B", "JR2", Map.of("timeType", Set.of("full"))),
				posting("Registered Nurse", "C", "JR3", Map.of("timeType", Set.of("part"))),
				posting("Registered Nurse", "D", "JR4", Map.of("timeType", Set.of("part")))));
		workday.facetedListStatus = 404;

		assertThatIOException().isThrownBy(() -> reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).isNotInstanceOf(BoardNotFoundException.class);
		assertThat(this.asked).containsExactly("jobs {} 0", "jobs {timeType=[full]} 0");
	}

	/** A real total above the cap is paged whole without applying any facet. */
	@Test
	void pagesAnUncappedBoardPastTheLimit() throws IOException {
		FakeWorkday workday = new FakeWorkday(nurses(7));
		workday.uncapped = true;

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(7);
		assertThat(this.asked).containsExactly("jobs {} 0", "jobs {} 2", "jobs {} 4", "jobs {} 6");
	}

	/** Exactly the cap still needs narrowing, and one-value facets must not reproduce the part. */
	@Test
	void narrowsAPartOfExactlyTheLimit() throws IOException {
		FakeWorkday workday = new FakeWorkday(nurses(4));
		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(4);
		assertThat(this.asked).hasSize(5).contains("jobs {locations=[L0]} 0", "jobs {locations=[L3]} 0");
		assertThat(this.asked).noneMatch((request) -> request.contains("workerSubType") || request.contains("coverage"));
	}

	/** Neither an incomplete company facet nor an overlapping facet covers the part. */
	@Test
	void passesOverFacetsWhoseSumsMissOrDoubleCountVacancies() throws IOException {
		List<Posting> postings = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			postings.add(posting("Registered Nurse", "L" + i, "JR" + i,
					Map.of("company", i == 0 ? Set.of("small") : Set.of(),
							"timeType", Set.of("full", "part"), "locations", Set.of("L" + i))));
		}
		assertThat(reader(new FakeWorkday(postings)).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(6);
		assertThat(this.asked).hasSize(7).noneMatch((request) -> request.contains("company") || request.contains("timeType"));
	}

	/** With no covering split, the finest location facet still reads overlapping parts once. */
	@Test
	void fallsBackToTheFinestLocationFacetWhenNoFacetCoversThePart() throws IOException {
		List<Posting> postings = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			postings.add(posting("Registered Nurse", "L" + i, "JR" + i,
					Map.of("countryLocation", i == 0 ? Set.of("country", "other") : Set.of("country"),
							"locations", i < 2 ? Set.of("L" + i, "shared") : Set.of("L" + i))));
		}
		assertThat(reader(new FakeWorkday(postings)).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(5);
		assertThat(this.asked).contains("jobs {locations=[L0]} 0");
		assertThat(this.asked).noneMatch((request) -> request.contains("countryLocation"));
	}

	/** The list is checked before an engineering vacancy can trigger a description request. */
	@Test
	void throwsWhenTheListStopsBeforeNinetyPercentOfTheBoard(CapturedOutput output) {
		FakeWorkday workday = new FakeWorkday(nurses(8).stream()
			.map((posting) -> posting("Software Engineer", posting.location(), posting.location())).toList());
		workday.uncapped = true;
		workday.reportedTotal = 10;
		assertThatIOException().isThrownBy(() -> reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).isInstanceOfSatisfying(ShortBoardException.class, (ex) -> {
			assertThat(ex.read()).isEqualTo(8);
			assertThat(ex.size()).isEqualTo(10);
		});
		assertThat(this.asked).noneMatch((request) -> request.startsWith("job "));
		assertThat(output.getOut()).contains("parts=1 read=8 size=10 failed=true");
	}

	/** Exactly ninety percent is enough to mirror a board whose counts drifted. */
	@Test
	void returnsABoardReadToExactlyNinetyPercent() throws IOException {
		FakeWorkday workday = new FakeWorkday(nurses(9));
		workday.uncapped = true;
		workday.reportedTotal = 10;
		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(9);
	}

	/** Without two agreeing facets, a capped board cannot be checked for completeness. */
	@Test
	void logsAnUncheckedCappedBoard(CapturedOutput output) throws IOException {
		FakeWorkday workday = new FakeWorkday(nurses(5));
		workday.coverage = false;
		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(5);
		assertThat(output.getOut()).contains("read=5 size=- failed=false", "not checked");
	}

	/** An exact agreement wins even when a larger pair agrees only within tolerance. */
	@ParameterizedTest
	@CsvSource({ "100,100,200,201,100", "100,100,200,200,200", "100,101,200,203,101" })
	void derivesTheLargestAgreedSizePreferringExactAgreement(long first, long second, long third, long fourth,
			int expected) {
		FakeWorkday workday = new FakeWorkday(nurses(4));
		workday.rootCounts = Map.of("first", Map.of("all", first), "second", Map.of("all", second),
				"third", Map.of("all", third), "fourth", Map.of("all", fourth));
		assertThatIOException().isThrownBy(() -> reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).isInstanceOfSatisfying(ShortBoardException.class, (ex) -> {
			assertThat(ex.read()).isEqualTo(4);
			assertThat(ex.size()).isEqualTo(expected);
		});
		assertThat(this.asked).containsExactly("jobs {} 0", "jobs {} 2");
	}

	/** Two sums farther apart than the tolerance leave a capped board unchecked. */
	@Test
	void leavesTheSizeUncheckedWhenFacetSumsDisagreeBeyondTolerance(CapturedOutput output) throws IOException {
		FakeWorkday workday = new FakeWorkday(nurses(4));
		workday.rootCounts = Map.of("first", Map.of("all", 100L), "second", Map.of("all", 102L));
		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(4);
		assertThat(output.getOut()).contains("read=4 size=- failed=false");
	}

	/** Equal page costs prefer fewer values even when one value must be narrowed again. */
	@Test
	void breaksAPageCostTieByFewerValuesIncludingACappedValue() throws IOException {
		List<Posting> postings = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			postings.add(posting("Registered Nurse", "L" + i, "JR" + i,
					Map.of("timeType", Set.of(i < 4 ? "full" : "part"),
							"jobFamilyGroup", Set.of("family" + i / 2), "locations", Set.of("L" + i))));
		}
		assertThat(reader(new FakeWorkday(postings)).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(6);
		assertThat(this.asked).contains("jobs {timeType=[full]} 0", "jobs {timeType=[part]} 0");
		assertThat(this.asked).noneMatch((request) -> request.startsWith("jobs {jobFamilyGroup="));
	}

	/** Counts within one percent may narrow a part despite request-to-request drift. */
	@Test
	void acceptsACoveringFacetWithinOnePercentOfTheSize() throws IOException {
		List<Posting> postings = new ArrayList<>();
		for (int i = 0; i < 101; i++) {
			postings.add(posting("Registered Nurse", "L" + i, "JR" + i,
					Map.of("timeType", i == 100 ? Set.of() : Set.of(i < 50 ? "full" : "part"),
							"locations", Set.of("L" + i))));
		}
		assertThat(reader(new FakeWorkday(postings)).read(SLUG, RegionEnum.US, () -> {
		})).hasSize(100);
		assertThat(this.asked).contains("jobs {timeType=[full]} 0", "jobs {timeType=[part]} 0");
	}

	/** Distinct locations provide a covering split for a miniature board. */
	private List<Posting> nurses(int count) {
		List<Posting> postings = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			postings.add(posting("Registered Nurse", "L" + i, "JR" + i, Map.of("locations", Set.of("L" + i))));
		}
		return postings;
	}

	/** Reads the miniature board with the descriptions the store holds. */
	private WorkdayVacancyReaderAdapter reader(FakeWorkday workday) {
		return new WorkdayVacancyReaderAdapter(workday, this.vacancies, new TitleCleaningRule(),
				new TitleClassificationRule(), 2, 4);
	}

	private static Posting posting(String title, String location, String requisition) {
		return posting(title, location, requisition, Map.of());
	}

	private static Posting posting(String title, String location, String requisition,
			Map<String, Set<String>> facets) {
		return new Posting(title, "/job/%s/%s_%s".formatted(location, title.replace(' ', '-'), requisition), location,
				facets);
	}

	private record Posting(String title, String externalPath, String location, Map<String, Set<String>> facets) {
	}

	/** A Workday board held in memory, answering as the real API does. */
	private final class FakeWorkday implements WorkdayApiPort {

		private final List<Posting> postings;

		private final Map<String, String> details = new LinkedHashMap<>();

		private int listStatus = 200;

		/** The answer for a page after offset zero. */
		private int laterListStatus = 200;

		/** The answer for a page narrowed by facets. */
		private int facetedListStatus = 200;

		private int detailStatus = 200;

		/** Whether the board pages beyond the miniature cap. */
		private boolean uncapped;

		/** A drifting total, or null to use the matching vacancies. */
		private Integer reportedTotal;

		/** Two single-value facets establish size without offering a split. */
		private boolean coverage = true;

		/** Root facet counts override the miniature's counts to model drifting size estimates. */
		private Map<String, Map<String, Long>> rootCounts;

		FakeWorkday(List<Posting> postings) {
			this.postings = postings;
		}

		@Override
		public Answer jobs(String slug, Map<String, List<String>> appliedFacets, int offset, int limit,
				Runnable onRefusal) {
			WorkdayVacancyReaderAdapterTest.this.asked.add("jobs %s %d".formatted(appliedFacets, offset));
			if (this.listStatus != 200) {
				return new Answer(this.listStatus, MissingNode.getInstance());
			}
			if (offset > 0 && this.laterListStatus != 200) {
				return new Answer(this.laterListStatus, MissingNode.getInstance());
			}
			if (!appliedFacets.isEmpty() && this.facetedListStatus != 200) {
				return new Answer(this.facetedListStatus, MissingNode.getInstance());
			}
			Predicate<Posting> applied = (posting) -> appliedFacets.entrySet()
				.stream()
				.allMatch((facet) -> facet.getValue()
					.stream()
					.anyMatch((value) -> posting.facets().getOrDefault(facet.getKey(), Set.of()).contains(value)));
			List<Posting> matching = this.postings.stream().filter(applied).toList();
			int from = (!this.uncapped && offset >= 4) ? 0 : offset;
			ObjectNode page = WorkdayVacancyReaderAdapterTest.this.json.createObjectNode();
			page.put("total", (offset == 0) ? (this.reportedTotal != null ? this.reportedTotal
					: this.uncapped ? matching.size() : Math.min(matching.size(), 4)) : 0);
			ArrayNode jobPostings = page.putArray("jobPostings");
			matching.stream().skip(from).limit(limit).forEach((posting) -> jobPostings.addObject()
				.put("title", posting.title())
				.put("externalPath", posting.externalPath())
				.put("locationsText", posting.location()));
			ArrayNode facets = page.putArray("facets");
			Map<String, Map<String, Long>> counts = new LinkedHashMap<>();
			if (this.coverage && !matching.isEmpty()) {
				counts.put("workerSubType", Map.of("all", (long) matching.size()));
				counts.put("coverage", Map.of("all", (long) matching.size()));
			}
			for (Posting posting : matching) {
				posting.facets().forEach((facet, values) -> values.forEach((value) -> counts
					.computeIfAbsent(facet, (key) -> new LinkedHashMap<>())
					.merge(value, 1L, Long::sum)));
			}
			if (appliedFacets.isEmpty() && this.rootCounts != null) {
				counts.clear();
				counts.putAll(this.rootCounts);
			}
			ObjectNode locationGroup = null;
			for (Map.Entry<String, Map<String, Long>> facet : counts.entrySet()) {
				ObjectNode node;
				if (facet.getKey().equals("locations")) {
					// Workday nests its location facets under a group that names no values of its own.
					locationGroup = facets.addObject().put("facetParameter", "locationMainGroup");
					node = locationGroup.putArray("values").addObject();
				}
				else {
					node = facets.addObject();
				}
				node.put("facetParameter", facet.getKey());
				ArrayNode values = node.putArray("values");
				facet.getValue().forEach((value, count) -> values.addObject().put("id", value).put("count", count));
			}
			return new Answer(200, page);
		}

		@Override
		public Answer job(String slug, String externalPath, Runnable onRefusal) {
			WorkdayVacancyReaderAdapterTest.this.asked.add("job " + externalPath);
			if (this.detailStatus != 200) {
				return new Answer(this.detailStatus, MissingNode.getInstance());
			}
			String detail = this.details.get(externalPath);
			if (detail == null) {
				return new Answer(404, MissingNode.getInstance());
			}
			JsonNode node = WorkdayVacancyReaderAdapterTest.this.json.readTree(detail);
			return new Answer(200, node);
		}

	}

}
