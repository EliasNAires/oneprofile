package oneprofile.backend.workers.normalization;

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
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.storage.normalizedvacancy.WorkModeVacancy;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class WorkModeRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	private final WorkModeRule rule = new WorkModeRule(new TitleCleaningRule());

	@Test
	void clearsWhatTheLastRunRecordedBeforeReadingTheSubset() {
		holds(new WorkModeVacancy(1, AtsEnum.GREENHOUSE, "Engineer", "Remote", null));

		run(10).readWorkModes();

		InOrder order = inOrder(this.normalized);
		order.verify(this.normalized).clearWorkModes();
		order.verify(this.normalized).workModeScopeAfter(0, 10);
	}

	@Test
	void recordsTheWorkModeOfEachVacancyThatDeclaresOne() {
		holds(new WorkModeVacancy(1, AtsEnum.GREENHOUSE, "Engineer", "Remote - USA", null),
				new WorkModeVacancy(2, AtsEnum.ASHBY, "Engineer", "Berlin", "OnSite"),
				new WorkModeVacancy(3, AtsEnum.GREENHOUSE, "Engineer", "London", null),
				new WorkModeVacancy(4, AtsEnum.LEVER, "Engineer (Hybrid)", "Paris", "unspecified"));

		run(10).readWorkModes();

		assertThat(recorded()).isEqualTo(Map.of(1L, WorkModeEnum.REMOTE, 2L, WorkModeEnum.ONSITE, 4L,
				WorkModeEnum.HYBRID));
	}

	@Test
	void reportsByAtsWhichStepDecidedEachVacancy() {
		holds(new WorkModeVacancy(1, AtsEnum.GREENHOUSE, "Engineer", "Remote - USA", null),
				new WorkModeVacancy(2, AtsEnum.GREENHOUSE, "Engineer", "London", null),
				new WorkModeVacancy(3, AtsEnum.ASHBY, "Engineer", "Berlin", "OnSite"),
				new WorkModeVacancy(4, AtsEnum.ASHBY, "Engineer", "Berlin", "Hybrid"));

		assertThat(run(10).readWorkModes()).isEqualTo(new WorkModeRun.Report(4,
				Map.of(AtsEnum.GREENHOUSE, Map.of(WorkModeEvidenceEnum.LOCATION, 1, WorkModeEvidenceEnum.NONE, 1),
						AtsEnum.ASHBY, Map.of(WorkModeEvidenceEnum.FIELD, 2))));
	}

	@Test
	void walksTheSubsetABatchAtATime() {
		holds(new WorkModeVacancy(1, AtsEnum.GREENHOUSE, "Engineer", null, null),
				new WorkModeVacancy(2, AtsEnum.GREENHOUSE, "Engineer", null, null),
				new WorkModeVacancy(3, AtsEnum.GREENHOUSE, "Engineer", null, null));

		assertThat(run(2).readWorkModes().vacancies()).isEqualTo(3);

		then(this.normalized).should().workModeScopeAfter(0, 2);
		then(this.normalized).should().workModeScopeAfter(2, 2);
		then(this.normalized).should().workModeScopeAfter(3, 2);
	}

	@Test
	void refusesABatchThatHoldsNothing() {
		assertThatThrownBy(() -> run(0)).isInstanceOf(IllegalArgumentException.class);
	}

	private WorkModeRun run(int batch) {
		return new WorkModeRun(this.rule, this.normalized, batch);
	}

	private void holds(WorkModeVacancy... scope) {
		given(this.normalized.workModeScopeAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return List.of(scope).stream().filter((vacancy) -> vacancy.vacancyId() > after).limit(batch).toList();
		});
	}

	private Map<Long, WorkModeEnum> recorded() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, WorkModeEnum>> captor = ArgumentCaptor.forClass(Map.class);
		then(this.normalized).should(Mockito.atLeastOnce()).recordWorkModes(captor.capture());
		Map<Long, WorkModeEnum> all = new HashMap<>();
		captor.getAllValues().forEach(all::putAll);
		return all;
	}

}
