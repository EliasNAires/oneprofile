package oneprofile.backend.workers.role;

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
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.RoleVacancy;
import oneprofile.backend.storage.rolelist.RoleListConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class RoleAssignmentRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	private final RoleRule rule = new RoleRule(RoleListConfiguration.shipped());

	@Test
	void clearsWhatTheLastRunRecordedBeforeReadingTheSubset() {
		holds(vacancy(1, "backend engineer"));

		run(10).assignRoles();

		InOrder order = inOrder(this.normalized);
		order.verify(this.normalized).clearRoles();
		order.verify(this.normalized).roleScopeAfter(0, 10);
	}

	@Test
	void givesEachVacancyTheRoleOfEachOfItsNormalizedTitles() {
		holds(vacancy(1, "data engineer"), vacancy(2, "data scientist", "machine learning engineer"),
				vacancy(3, "engineering"), vacancy(4), vacancy(5, "game developer"));

		run(10).assignRoles();

		assertThat(recorded()).isEqualTo(Map.of(1L, List.of("data-engineer"), 2L,
				List.of("data-scientist", "machine-learning-engineer"), 3L, List.of("software-engineer"), 4L,
				List.of(), 5L, List.of()));
	}

	@Test
	void reportsHowManyVacanciesItReadAndHowManyWereLeftWithoutARole() {
		holds(vacancy(1, "backend engineer"), vacancy(2), vacancy(3, "game developer"));

		assertThat(run(10).assignRoles()).isEqualTo(new RoleAssignmentRun.Report(3, 2));
	}

	@Test
	void walksTheSubsetABatchAtATime() {
		holds(vacancy(1, "engineer"), vacancy(2, "engineer"), vacancy(3, "engineer"));

		assertThat(run(2).assignRoles().vacancies()).isEqualTo(3);

		then(this.normalized).should().roleScopeAfter(0, 2);
		then(this.normalized).should().roleScopeAfter(2, 2);
		then(this.normalized).should().roleScopeAfter(3, 2);
	}

	@Test
	void refusesABatchThatHoldsNothing() {
		assertThatThrownBy(() -> run(0)).isInstanceOf(IllegalArgumentException.class);
	}

	private RoleAssignmentRun run(int batch) {
		return new RoleAssignmentRun(this.rule, this.normalized, batch);
	}

	private static RoleVacancy vacancy(long vacancyId, String... normalizedTitles) {
		return new RoleVacancy(vacancyId, List.of(normalizedTitles));
	}

	private void holds(RoleVacancy... scope) {
		given(this.normalized.roleScopeAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return List.of(scope).stream().filter((vacancy) -> vacancy.vacancyId() > after).limit(batch).toList();
		});
	}

	private Map<Long, List<String>> recorded() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, List<String>>> captor = ArgumentCaptor.forClass(Map.class);
		then(this.normalized).should(Mockito.atLeastOnce()).recordRoles(captor.capture());
		Map<Long, List<String>> all = new HashMap<>();
		captor.getAllValues().forEach(all::putAll);
		return all;
	}

}
