package oneprofile.backend.workers.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.normalizedvacancy.GroupMark;
import oneprofile.backend.storage.normalizedvacancy.GroupVacancy;
import oneprofile.backend.storage.normalizedvacancy.ManagementLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class VacancyGroupRunTest {

	/** A substantial description whose shingles can join vacancies. */
	private static final String DESCRIPTION = "Design reliable distributed systems and build scalable services with our engineering team "
			+ "while improving production performance through careful testing monitoring collaboration architecture reviews "
			+ "and thoughtful technical decisions for customers around the world";

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	private final DescriptionSimilarityRule similarity = new DescriptionSimilarityRule();

	@Test
	void loneVacancyGetsItsOwnGroupWithoutReadingItsDescription() {
		holds(vacancy(1, null));
		assertThat(run(2).groupVacancies()).isEqualTo(new VacancyGroupRun.Report(1, 1, 0, 1));
		assertThat(recorded()).isEqualTo(Map.of(1L, GroupMark.inGroup(100)));
		then(this.normalized).should(never()).segmentsOf(anyCollection());
		then(this.normalized).should().clearVacancyGroupsOutsideSubset();
	}

	@Test
	void carriesForwardAStoredId() {
		holds(vacancy(1, 17L));
		assertThat(run(2).groupVacancies()).isEqualTo(new VacancyGroupRun.Report(1, 1, 0, 0));
		assertThat(recorded()).isEqualTo(Map.of(1L, GroupMark.inGroup(17)));
	}

	@Test
	void mergedGroupKeepsTheIdMostMembersHeld() {
		holds(vacancy(1, 17L), vacancy(2, 23L), vacancy(3, 23L));
		assertThat(run(2).groupVacancies()).isEqualTo(new VacancyGroupRun.Report(3, 1, 0, 0));
		assertThat(recorded()).isEqualTo(Map.of(1L, GroupMark.inGroup(23), 2L, GroupMark.inGroup(23),
				3L, GroupMark.inGroup(23)));
	}

	@Test
	void splitGroupGivesItsFirstHalfTheOldIdAndReservesFreshIdsTogether() {
		holds(vacancy(4, 17L), vacancy(2, 17L), vacancy(3, 17L), vacancy(1, 17L));
		given(this.normalized.segmentsOf(anyCollection())).willAnswer((invocation) -> {
			Collection<Long> ids = invocation.getArgument(0);
			Map<Long, List<Segment>> segments = new HashMap<>();
			ids.forEach((id) -> segments.put(id, (id <= 2) ? description() : List.of()));
			return segments;
		});
		assertThat(run(2).groupVacancies()).isEqualTo(new VacancyGroupRun.Report(4, 3, 0, 2));
		assertThat(recorded()).isEqualTo(Map.of(1L, GroupMark.inGroup(17), 2L, GroupMark.inGroup(17),
				3L, GroupMark.inGroup(100), 4L, GroupMark.inGroup(101)));
		then(this.normalized).should().nextGroupIds(2);
	}

	@Test
	void duplicateNamesTheKeptVacancyAndVotesForTheCarriedId() {
		GroupVacancy first = vacancy(1, 17L);
		GroupVacancy duplicate = evidence(vacancy(2, 17L), first.slug(), first.normalizedTitles(), null, Set.of(),
				first.locations());
		holds(first, duplicate, vacancy(3, 23L));
		assertThat(run(2).groupVacancies()).isEqualTo(new VacancyGroupRun.Report(3, 1, 1, 0));
		assertThat(recorded()).isEqualTo(Map.of(1L, GroupMark.duplicateOf(2), 2L, GroupMark.inGroup(17),
				3L, GroupMark.inGroup(17)));
	}

	@Test
	void differentSlugsTitlesSenioritiesAndManagementLevelsStayApart() {
		GroupVacancy first = vacancy(1, null);
		holds(first,
				evidence(vacancy(2, null), "other", first.normalizedTitles(), null, Set.of(), List.of("Paris")),
				evidence(vacancy(3, null), "acme", List.of("data engineer"), null, Set.of(), List.of("Paris")),
				evidence(vacancy(4, null), "acme", first.normalizedTitles(), null, Set.of(SeniorityLevelEnum.SENIOR),
						List.of("Paris")),
				evidence(vacancy(5, null), "acme", first.normalizedTitles(), ManagementLevelEnum.LEAD, Set.of(),
						List.of("Paris")));
		assertThat(run(2).groupVacancies()).isEqualTo(new VacancyGroupRun.Report(5, 5, 0, 5));
		assertThat(recorded().values()).doesNotHaveDuplicates();
		then(this.normalized).should(never()).segmentsOf(anyCollection());
	}

	@Test
	void boardsOfOneSlugOnTwoAtsJoinAndUseOpenVacancyCounts() {
		GroupVacancy first = vacancy(1, null);
		GroupVacancy second = new GroupVacancy(2, 20, AtsEnum.ASHBY, "acme", "Engineer", List.of("software engineer"),
				null, Set.of(), first.locations(), Set.of(WorkModeEnum.REMOTE), null, null);
		holds(first, second);
		given(this.normalized.openVacanciesByCompany()).willReturn(Map.of(20L, 10));
		assertThat(run(2).groupVacancies()).isEqualTo(new VacancyGroupRun.Report(2, 1, 1, 1));
		assertThat(recorded()).isEqualTo(Map.of(1L, GroupMark.duplicateOf(2), 2L, GroupMark.inGroup(100)));
	}

	@Test
	void emptyAndNullNormalizedTitlesFallBackToTheLowercaseCleanedTitle() {
		GroupVacancy first = evidence(vacancy(1, 23L), "acme", null, null, Set.of(), List.of("Paris"));
		GroupVacancy second = new GroupVacancy(2, 10, AtsEnum.GREENHOUSE, "acme", "engineer", List.of(), null,
				Set.of(), List.of("London"), Set.of(WorkModeEnum.REMOTE), null, 17L);
		holds(second, first);
		assertThat(run(2).groupVacancies().groups()).isEqualTo(1);
		assertThat(recorded()).isEqualTo(Map.of(1L, GroupMark.inGroup(17), 2L, GroupMark.inGroup(17)));
	}

	@Test
	void readsWholeBucketsTogetherAndWritesBatchesOfAtMostTheChunk() {
		List<GroupVacancy> scope = new ArrayList<>();
		for (long id = 1; id <= 7; id++) {
			GroupVacancy vacancy = vacancy(id, null);
			scope.add(evidence(vacancy, (id <= 3) ? "acme" : (id <= 5) ? "other" : "third",
					vacancy.normalizedTitles(), null, Set.of(), vacancy.locations()));
		}
		holds(scope.toArray(GroupVacancy[]::new));
		run(4).groupVacancies();
		then(this.normalized).should().segmentsOf(List.of(1L, 2L, 3L));
		then(this.normalized).should().segmentsOf(List.of(4L, 5L, 6L, 7L));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, GroupMark>> batches = ArgumentCaptor.forClass(Map.class);
		then(this.normalized).should(Mockito.times(2)).recordVacancyGroups(batches.capture());
		assertThat(batches.getAllValues()).extracting(Map::size).containsExactly(4, 3);
	}

	@Test
	void stripsAllBucketLocationsFromBothDescriptions() {
		GroupVacancy first = evidence(vacancy(1, null), "acme", List.of("software engineer"), null, Set.of(),
				List.of("London"));
		GroupVacancy second = evidence(vacancy(2, null), "acme", first.normalizedTitles(), null, Set.of(),
				List.of("Paris"));
		holds(first, second);
		given(this.normalized.segmentsOf(List.of(1L, 2L))).willReturn(Map.of(
				1L, List.of(new Segment(SegmentKindEnum.SENTENCE, null, DESCRIPTION + " Paris", false)),
				2L, List.of(new Segment(SegmentKindEnum.SENTENCE, null, DESCRIPTION + " London", false))));
		VacancyGroupRun run = new VacancyGroupRun(this.similarity, new VacancyGroupRule(this.similarity),
				this.normalized, 1.0, 2);

		assertThat(run.groupVacancies()).isEqualTo(new VacancyGroupRun.Report(2, 1, 0, 1));
		assertThat(recorded()).isEqualTo(Map.of(1L, GroupMark.inGroup(100), 2L, GroupMark.inGroup(100)));
	}

	@Test
	void emptySubsetStillClearsOutsideItAndReportsZero() {
		holds();
		assertThat(run(2).groupVacancies()).isEqualTo(new VacancyGroupRun.Report(0, 0, 0, 0));
		then(this.normalized).should().clearVacancyGroupsOutsideSubset();
	}

	@Test
	void refusesAChunkThatHoldsNothing() {
		assertThatThrownBy(() -> run(0)).isInstanceOf(IllegalArgumentException.class);
	}

	/** Builds the run with the real rules and a controllable write and read chunk. */
	private VacancyGroupRun run(int chunk) {
		return new VacancyGroupRun(this.similarity, new VacancyGroupRule(this.similarity), this.normalized, 0.8, chunk);
	}

	/** Holds a scope with identical descriptions and fresh ids starting at 100. */
	private void holds(GroupVacancy... scope) {
		given(this.normalized.vacancyGroupScope()).willReturn(List.of(scope));
		given(this.normalized.openVacanciesByCompany()).willReturn(Map.of());
		given(this.normalized.nextGroupIds(anyInt())).willAnswer((invocation) -> {
			int count = invocation.getArgument(0);
			return LongStream.range(100, 100 + count).boxed().toList();
		});
		given(this.normalized.segmentsOf(anyCollection())).willAnswer((invocation) -> {
			Collection<Long> ids = invocation.getArgument(0);
			Map<Long, List<Segment>> segments = new HashMap<>();
			ids.forEach((id) -> segments.put(id, description()));
			return segments;
		});
	}

	/** One vacancy with a distinct location token. */
	private GroupVacancy vacancy(long id, Long groupId) {
		return new GroupVacancy(id, 10, AtsEnum.GREENHOUSE, "acme", "Engineer", List.of("software engineer"), null,
				Set.of(), List.of("city" + id), Set.of(WorkModeEnum.REMOTE), null, groupId);
	}

	/** Changes the bucket and duplicate evidence while retaining a vacancy's identity. */
	private GroupVacancy evidence(GroupVacancy vacancy, String slug, List<String> titles,
			ManagementLevelEnum management, Set<SeniorityLevelEnum> seniorities, List<String> locations) {
		return new GroupVacancy(vacancy.vacancyId(), vacancy.companyId(), vacancy.ats(), slug, vacancy.cleanedTitle(),
				titles, management, seniorities, locations, vacancy.workModes(), vacancy.firstPublishedAt(), vacancy.groupId());
	}

	/** A comparable description without boilerplate. */
	private List<Segment> description() {
		return List.of(new Segment(SegmentKindEnum.SENTENCE, null, DESCRIPTION, false));
	}

	/** Collects all marks written across batches. */
	private Map<Long, GroupMark> recorded() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, GroupMark>> captor = ArgumentCaptor.forClass(Map.class);
		then(this.normalized).should(Mockito.atLeastOnce()).recordVacancyGroups(captor.capture());
		Map<Long, GroupMark> all = new HashMap<>();
		captor.getAllValues().forEach(all::putAll);
		return all;
	}

}
