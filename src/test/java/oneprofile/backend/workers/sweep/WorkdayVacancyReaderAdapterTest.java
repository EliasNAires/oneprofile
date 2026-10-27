package oneprofile.backend.workers.sweep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

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
import oneprofile.backend.workers.WorkdayApiPort;
import oneprofile.backend.workers.classification.TitleClassificationRule;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.junit.jupiter.api.Test;
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

	@Test
	void readsTheDescriptionOfAVacancyWhoseTitleIsAnEngineeringRole() throws IOException {
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

	@Test
	void readsABoardTooLargeToPageWholeInPartsByTheFacetThatSplitsItUnderTheLimit() throws IOException {
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

	@Test
	void leavesOutAVacancyThatClosedBetweenTheListAndItsPage() throws IOException {
		FakeWorkday workday = new FakeWorkday(List.of(posting("Senior Software Engineer", "Santa-Clara", "JR1")));

		assertThat(reader(workday).read(SLUG, RegionEnum.US, () -> {
		})).isEmpty();
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

	@Test
	void failsOnABoardThatCannotBeRead() {
		FakeWorkday workday = new FakeWorkday(List.of());
		workday.listStatus = 422;

		assertThatIOException().isThrownBy(() -> reader(workday).read(SLUG, RegionEnum.US, () -> {
		}));
	}

	private WorkdayVacancyReaderAdapter reader(FakeWorkday workday) {
		return new WorkdayVacancyReaderAdapter(workday, new TitleCleaningRule(), new TitleClassificationRule(), 2, 4);
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

		private int detailStatus = 200;

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
			Predicate<Posting> applied = (posting) -> appliedFacets.entrySet()
				.stream()
				.allMatch((facet) -> facet.getValue()
					.stream()
					.anyMatch((value) -> posting.facets().getOrDefault(facet.getKey(), Set.of()).contains(value)));
			List<Posting> matching = this.postings.stream().filter(applied).toList();
			int from = (offset >= 4) ? 0 : offset;
			ObjectNode page = WorkdayVacancyReaderAdapterTest.this.json.createObjectNode();
			page.put("total", (offset == 0) ? Math.min(matching.size(), 4) : 0);
			ArrayNode jobPostings = page.putArray("jobPostings");
			matching.stream().skip(from).limit(limit).forEach((posting) -> jobPostings.addObject()
				.put("title", posting.title())
				.put("externalPath", posting.externalPath())
				.put("locationsText", posting.location()));
			ArrayNode facets = page.putArray("facets");
			Map<String, Map<String, Integer>> counts = new LinkedHashMap<>();
			for (Posting posting : matching) {
				posting.facets().forEach((facet, values) -> values.forEach((value) -> counts
					.computeIfAbsent(facet, (key) -> new LinkedHashMap<>())
					.merge(value, 1, Integer::sum)));
			}
			ObjectNode locationGroup = null;
			for (Map.Entry<String, Map<String, Integer>> facet : counts.entrySet()) {
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
