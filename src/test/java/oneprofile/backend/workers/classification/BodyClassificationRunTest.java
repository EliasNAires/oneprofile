package oneprofile.backend.workers.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.PileVacancy;
import oneprofile.backend.storage.normalizedvacancy.UnknownReasonEnum;
import oneprofile.backend.workers.cleaning.DescriptionCleaningRule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class BodyClassificationRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	@Test
	void putsThePileBackToWhatTheTitleLeftItBeforeReadingIt() {
		holds(pile(1, "Engineer", "Build things."));

		run(new BodyClassificationRule(), 10).classifyPile();

		InOrder order = inOrder(this.normalized);
		order.verify(this.normalized).resetPile();
		order.verify(this.normalized).pileAfter(0, 10);
	}

	@Test
	void leavesThePileUnknownWhileNoBodyRuleDecides() {
		holds(pile(1, "Engineer", "Build things."), pile(2, "Product Manager", "Own the roadmap."));

		BodyClassificationRun.Report report = run(new BodyClassificationRule(), 10).classifyPile();

		assertThat(report).isEqualTo(new BodyClassificationRun.Report(2, 0, 0, 2));
		then(this.normalized).should(never()).recordBodyDecisions(anyMap());
	}

	@Test
	void recordsWhatTheBodyDecidesFromTheCleanedDescription() {
		holds(pile(1, "Engineer", "Ship 🚀 Go services."), pile(2, "Engineer", "Weld pipes."),
				pile(3, "Engineer", "We are hiring."));
		BodyClassificationRule rule = mock(BodyClassificationRule.class);
		given(rule.classify(anyString(), any(), anyString())).willReturn(ClassificationStateEnum.UNKNOWN);
		given(rule.classify("Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, "Ship Go services."))
			.willReturn(ClassificationStateEnum.IN);
		given(rule.classify("Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, "Weld pipes."))
			.willReturn(ClassificationStateEnum.OUT);

		BodyClassificationRun.Report report = run(rule, 10).classifyPile();

		assertThat(report).isEqualTo(new BodyClassificationRun.Report(3, 1, 1, 1));
		assertThat(recorded()).containsExactlyInAnyOrderEntriesOf(
				Map.of(1L, ClassificationStateEnum.IN, 2L, ClassificationStateEnum.OUT));
	}

	@Test
	void readsAVacancyWithNoDescriptionAsAnEmptyOne() {
		holds(pile(1, "Engineer", null));
		BodyClassificationRule rule = mock(BodyClassificationRule.class);
		given(rule.classify(any(), any(), any())).willReturn(ClassificationStateEnum.UNKNOWN);

		run(rule, 10).classifyPile();

		then(rule).should().classify("Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, "");
	}

	@Test
	void givesTheRuleTheReasonTheTitleWasLeftUnknown() {
		holds(new PileVacancy(1, "Product Manager", UnknownReasonEnum.SCOPE_AMBIGUITY, "Own the roadmap."));
		BodyClassificationRule rule = mock(BodyClassificationRule.class);
		given(rule.classify(any(), any(), any())).willReturn(ClassificationStateEnum.UNKNOWN);

		run(rule, 10).classifyPile();

		then(rule).should().classify("Product Manager", UnknownReasonEnum.SCOPE_AMBIGUITY, "Own the roadmap.");
	}

	@Test
	void walksThePileABatchAtATime() {
		holds(pile(1, "Engineer", "a"), pile(2, "Engineer", "b"), pile(3, "Engineer", "c"));

		assertThat(run(new BodyClassificationRule(), 2).classifyPile().pile()).isEqualTo(3);

		then(this.normalized).should().pileAfter(0, 2);
		then(this.normalized).should().pileAfter(2, 2);
		then(this.normalized).should().pileAfter(3, 2);
	}

	@Test
	void refusesABatchThatHoldsNothing() {
		assertThatThrownBy(() -> run(new BodyClassificationRule(), 0)).isInstanceOf(IllegalArgumentException.class);
	}

	private BodyClassificationRun run(BodyClassificationRule rule, int batch) {
		return new BodyClassificationRun(rule, new DescriptionCleaningRule(), this.normalized, batch);
	}

	private static PileVacancy pile(long vacancyId, String cleanedTitle, String description) {
		return new PileVacancy(vacancyId, cleanedTitle, UnknownReasonEnum.DOMAIN_AMBIGUITY, description);
	}

	private void holds(PileVacancy... pile) {
		given(this.normalized.pileAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return List.of(pile).stream().filter((vacancy) -> vacancy.vacancyId() > after).limit(batch).toList();
		});
		given(this.normalized.recordBodyDecisions(anyMap())).willAnswer((invocation) -> {
			Map<?, ?> recorded = invocation.getArgument(0);
			return recorded.size();
		});
	}

	private Map<Long, ClassificationStateEnum> recorded() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, ClassificationStateEnum>> captor = ArgumentCaptor.forClass(Map.class);
		then(this.normalized).should(Mockito.atLeastOnce()).recordBodyDecisions(captor.capture());
		Map<Long, ClassificationStateEnum> all = new HashMap<>();
		captor.getAllValues().forEach(all::putAll);
		return all;
	}

}
