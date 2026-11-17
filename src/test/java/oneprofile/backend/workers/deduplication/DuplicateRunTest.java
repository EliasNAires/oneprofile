package oneprofile.backend.workers.deduplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.DuplicateVacancy;
import oneprofile.backend.storage.normalizedvacancy.DuplicateVacancy.Location;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import org.junit.jupiter.api.Test;

class DuplicateRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	@Test
	void loneKeysReadNoDescriptionsAndStillReplaceDuplicateMarks() {
		holds(vacancy(1, "acme"), vacancy(2, "other"));

		assertThat(run(2).markDuplicates()).isEqualTo(new DuplicateRun.Report(2, 0));
		then(this.normalized).should(never()).descriptionsOf(anyCollection());
		then(this.normalized).should().recordDuplicates(Map.of());
		then(this.normalized).should().duplicateScope();
		then(this.normalized).shouldHaveNoMoreInteractions();
	}

	@Test
	void readsWholeKeysTogetherAndRecordsEveryDuplicateInOneCall() {
		holds(vacancy(1, "acme"), vacancy(2, "acme"), vacancy(3, "acme"), vacancy(4, "other"),
				vacancy(5, "other"), vacancy(6, "third"), vacancy(7, "third"), vacancy(8, "lone"));

		assertThat(run(4).markDuplicates()).isEqualTo(new DuplicateRun.Report(8, 4));
		then(this.normalized).should().descriptionsOf(List.of(1L, 2L, 3L));
		then(this.normalized).should().descriptionsOf(List.of(4L, 5L, 6L, 7L));
		then(this.normalized).should().recordDuplicates(Map.of(1L, 3L, 2L, 3L, 4L, 5L, 6L, 7L));
		then(this.normalized).should().duplicateScope();
		then(this.normalized).shouldHaveNoMoreInteractions();
	}

	@Test
	void aKeyLargerThanTheChunkIsReadWhole() {
		holds(vacancy(1, "acme"), vacancy(2, "acme"), vacancy(3, "acme"),
				vacancy(4, "other"), vacancy(5, "other"));

		assertThat(run(2).markDuplicates()).isEqualTo(new DuplicateRun.Report(5, 3));
		then(this.normalized).should().descriptionsOf(List.of(1L, 2L, 3L));
		then(this.normalized).should().descriptionsOf(List.of(4L, 5L));
		then(this.normalized).should().recordDuplicates(Map.of(1L, 3L, 2L, 3L, 4L, 5L));
		then(this.normalized).should().duplicateScope();
		then(this.normalized).shouldHaveNoMoreInteractions();
	}

	@Test
	void passesRawEvidenceThroughTheRuleBeforeReadingDescriptions() {
		holds(new DuplicateVacancy(1, 20, "acme", " SOFTWARE\tEngineer ", List.of(new Location(" London ", null), new Location("PARIS", null), new Location("London", null)),
				Set.of(WorkModeEnum.REMOTE), Instant.parse("2020-01-01T00:00:00Z")),
				new DuplicateVacancy(2, 10, "acme", "software engineer", List.of(new Location("Paris", null), new Location("london", null)),
						Set.of(WorkModeEnum.REMOTE), Instant.parse("2021-01-01T00:00:00Z")),
				new DuplicateVacancy(3, 10, "acme", "software developer", List.of(new Location("Paris", null), new Location("london", null)),
						Set.of(WorkModeEnum.REMOTE), null));
		given(this.normalized.descriptionsOf(List.of(1L, 2L)))
			.willReturn(Map.of(1L, " BUILD\nSystems ", 2L, "build systems"));

		assertThat(run(2).markDuplicates()).isEqualTo(new DuplicateRun.Report(3, 1));
		then(this.normalized).should().descriptionsOf(List.of(1L, 2L));
		then(this.normalized).should().recordDuplicates(Map.of(2L, 1L));
		then(this.normalized).should().duplicateScope();
		then(this.normalized).shouldHaveNoMoreInteractions();
	}

	@Test
	void emptySubsetRecordsAnEmptyMapAndReportsZero() {
		holds();

		assertThat(run(2).markDuplicates()).isEqualTo(new DuplicateRun.Report(0, 0));
		then(this.normalized).should(never()).descriptionsOf(anyCollection());
		then(this.normalized).should().recordDuplicates(Map.of());
		then(this.normalized).should().duplicateScope();
		then(this.normalized).shouldHaveNoMoreInteractions();
	}

	@Test
	void refusesAChunkThatHoldsNothing() {
		assertThatThrownBy(() -> run(0)).isInstanceOf(IllegalArgumentException.class);
	}

	/** Builds the run with the real rule and a controllable read chunk. */
	private DuplicateRun run(int chunk) {
		return new DuplicateRun(new DuplicateRule(), this.normalized, chunk);
	}

	/** Holds a scope whose requested raw descriptions are identical. */
	private void holds(DuplicateVacancy... scope) {
		given(this.normalized.duplicateScope()).willReturn(List.of(scope));
		given(this.normalized.descriptionsOf(anyCollection())).willAnswer((invocation) -> {
			Collection<Long> ids = invocation.getArgument(0);
			Map<Long, String> descriptions = new HashMap<>();
			ids.forEach((id) -> descriptions.put(id, "Build systems"));
			return descriptions;
		});
	}

	/** One vacancy sharing the same raw evidence with every vacancy of its slug. */
	private DuplicateVacancy vacancy(long id, String slug) {
		return new DuplicateVacancy(id, 10, slug, "Engineer", List.of(new Location("London", null)), Set.of(WorkModeEnum.REMOTE), null);
	}

}
