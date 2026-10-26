package oneprofile.backend.workers.normalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.BreakdownVacancy;
import oneprofile.backend.storage.normalizedvacancy.ManagementLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.TitleBreakdown;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
import oneprofile.backend.storage.vacancyskill.VacancySkillStore;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class TitleBreakdownRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	private final VacancySkillStore skills = mock(VacancySkillStore.class);

	private final TitleBreakdownRule rule = new TitleBreakdownRule(new TitleCleaningRule(),
			TaxonomyStore.read(new StringReader("""
					java\tJava\tlanguage\t\t\t
					""")));

	@Test
	void clearsWhatTheLastRunRecordedBeforeReadingTheSubset() {
		holds(vacancy(1, "Backend Engineer", null));

		run(10).breakDownTitles();

		InOrder order = inOrder(this.normalized);
		order.verify(this.normalized).clearTitleBreakdowns();
		order.verify(this.normalized).breakdownScopeAfter(0, 10);
	}

	@Test
	void recordsTheBreakdownOfEachTitleWithTheSkillsItNamesAndThePlacesItsLocationNames() {
		holds(vacancy(1, "Staff Java Engineer - Lisbon", "Lisbon, Portugal"),
				vacancy(2, "Engineering Manager", null));
		given(this.skills.titleSkillIds(anyCollection())).willReturn(Map.of(101L, Set.of("java")));

		run(10).breakDownTitles();

		assertThat(recorded()).isEqualTo(Map.of(1L,
				new TitleBreakdown(List.of("engineer"), Set.of(SeniorityLevelEnum.STAFF), null), 2L,
				new TitleBreakdown(List.of("engineering"), Set.of(), ManagementLevelEnum.MANAGER)));
	}

	@Test
	void reportsHowManyTitlesItReadAndHowManyLeftNoNormalizedTitle() {
		holds(vacancy(1, "Backend Engineer", null), vacancy(2, "Manager", null), vacancy(3, "Director", null));

		assertThat(run(10).breakDownTitles()).isEqualTo(new TitleBreakdownRun.Report(3, 2));
	}

	@Test
	void walksTheSubsetABatchAtATime() {
		holds(vacancy(1, "Engineer", null), vacancy(2, "Engineer", null), vacancy(3, "Engineer", null));

		assertThat(run(2).breakDownTitles().vacancies()).isEqualTo(3);

		then(this.normalized).should().breakdownScopeAfter(0, 2);
		then(this.normalized).should().breakdownScopeAfter(2, 2);
		then(this.normalized).should().breakdownScopeAfter(3, 2);
	}

	@Test
	void refusesABatchThatHoldsNothing() {
		assertThatThrownBy(() -> run(0)).isInstanceOf(IllegalArgumentException.class);
	}

	private TitleBreakdownRun run(int batch) {
		return new TitleBreakdownRun(this.rule, this.normalized, this.skills, batch);
	}

	private static BreakdownVacancy vacancy(long vacancyId, String title, String location) {
		return new BreakdownVacancy(100 + vacancyId, vacancyId, title, location);
	}

	private void holds(BreakdownVacancy... scope) {
		given(this.normalized.breakdownScopeAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return List.of(scope).stream().filter((vacancy) -> vacancy.vacancyId() > after).limit(batch).toList();
		});
	}

	private Map<Long, TitleBreakdown> recorded() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, TitleBreakdown>> captor = ArgumentCaptor.forClass(Map.class);
		then(this.normalized).should(Mockito.atLeastOnce()).recordTitleBreakdowns(captor.capture());
		Map<Long, TitleBreakdown> all = new HashMap<>();
		captor.getAllValues().forEach(all::putAll);
		return all;
	}

}
