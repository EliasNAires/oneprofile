package oneprofile.backend.workers.seniority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.ExperienceRequirement;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.SeniorityVacancy;
import oneprofile.backend.storage.normalizedvacancy.VacancySeniority;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class SeniorityRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	@Test
	void clearsWhatTheLastRunRecordedBeforeReadingTheSubset() {
		holds(vacancy(1, Set.of()));

		run(10).readSeniorities();

		InOrder order = inOrder(this.normalized);
		order.verify(this.normalized).clearSeniorities();
		order.verify(this.normalized).seniorityScopeAfter(0, 10);
	}

	@Test
	void recordsTheLevelAndTheRequiredYearsOfEachVacancyThatHasEither() {
		holds(vacancy(1, Set.of(SeniorityLevelEnum.SENIOR), "2+ years of Java"),
				vacancy(2, Set.of(SeniorityLevelEnum.II), "3-5 years of Python"), vacancy(3, Set.of()));

		run(10).readSeniorities();

		assertThat(recorded()).isEqualTo(Map.of(
				1L, new VacancySeniority(SeniorityLevelEnum.SENIOR, 2,
						List.of(new ExperienceRequirement(0, 0, 2, null, true))),
				2L, new VacancySeniority(SeniorityLevelEnum.MID, 3, List.of(new ExperienceRequirement(0, 0, 3, 5, true)))));
	}

	@Test
	void reportsCoverageBeforeAndAfterTheYearsAndHowOftenTheyDisagreeWithTheTitle() {
		holds(vacancy(1, Set.of(SeniorityLevelEnum.SENIOR), "2+ years of Java"),
				vacancy(2, Set.of(SeniorityLevelEnum.SENIOR), "8+ years of Java"),
				vacancy(3, Set.of(SeniorityLevelEnum.SENIOR), "5+ years of Java"),
				vacancy(4, Set.of(SeniorityLevelEnum.II), "3+ years of Go"), vacancy(5, Set.of(SeniorityLevelEnum.III)),
				vacancy(6, Set.of(), "1 year of experience"), vacancy(7, Set.of()));

		assertThat(run(10).readSeniorities()).isEqualTo(new SeniorityRun.Report(7, 3, 5, 5, 2, 1,
				new SeniorityRun.Disagreement(3, 1, 1),
				Map.of(SeniorityLevelEnum.SENIOR, 3, SeniorityLevelEnum.MID, 1, SeniorityLevelEnum.ENTRY, 1)));
	}

	@Test
	void readsTheRequirementsKnowingWhichCompanyPublishedThem() {
		holds(new SeniorityVacancy(1, "opentable", Set.of(), List.of(
				new Segment(SegmentKindEnum.SENTENCE, null,
						"With 25+ years of experience, OpenTable is an industry leader with a passion for restaurants.", false),
				new Segment(SegmentKindEnum.ITEM, "Requirements", "5+ years of hands-on Linux experience", false))));

		run(10).readSeniorities();

		assertThat(recorded().get(1L).requiredYears()).isEqualTo(5);
	}

	@Test
	void walksTheSubsetABatchAtATime() {
		holds(vacancy(1, Set.of()), vacancy(2, Set.of()), vacancy(3, Set.of()));

		assertThat(run(2).readSeniorities().vacancies()).isEqualTo(3);

		then(this.normalized).should().seniorityScopeAfter(0, 2);
		then(this.normalized).should().seniorityScopeAfter(2, 2);
		then(this.normalized).should().seniorityScopeAfter(3, 2);
	}

	@Test
	void refusesABatchThatHoldsNothing() {
		assertThatThrownBy(() -> run(0)).isInstanceOf(IllegalArgumentException.class);
	}

	private SeniorityRun run(int batch) {
		return new SeniorityRun(new ExperienceRequirementRule(), new SeniorityRule(), this.normalized, batch);
	}

	private static SeniorityVacancy vacancy(long vacancyId, Set<SeniorityLevelEnum> titleSeniorities, String... items) {
		return new SeniorityVacancy(vacancyId, "acme", titleSeniorities,
				List.of(items).stream().map((text) -> new Segment(SegmentKindEnum.ITEM, "Requirements", text, false)).toList());
	}

	private void holds(SeniorityVacancy... scope) {
		given(this.normalized.seniorityScopeAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return List.of(scope).stream().filter((vacancy) -> vacancy.vacancyId() > after).limit(batch).toList();
		});
	}

	private Map<Long, VacancySeniority> recorded() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, VacancySeniority>> captor = ArgumentCaptor.forClass(Map.class);
		then(this.normalized).should(Mockito.atLeastOnce()).recordSeniorities(captor.capture());
		Map<Long, VacancySeniority> all = new HashMap<>();
		captor.getAllValues().forEach(all::putAll);
		return all;
	}

}
