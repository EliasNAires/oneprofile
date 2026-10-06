package oneprofile.backend.workers.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.PileVacancy;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.normalizedvacancy.UnknownReasonEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class BodyClassificationRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	@Test
	void putsThePileBackToWhatTheTitleLeftItBeforeReadingIt() {
		holds(pile(1, "en", "Build things."));

		run(new BodyClassificationRule(), 10).classifyPile();

		InOrder order = inOrder(this.normalized);
		order.verify(this.normalized).resetPile();
		order.verify(this.normalized).pileAfter(0, 10);
	}

	@Test
	void leavesThePileUnknownWhileNoBodyRuleDecides() {
		holds(pile(1, "en", "Build things."), pile(2, "en", "Own the roadmap."));

		BodyClassificationRun.Report report = run(new BodyClassificationRule(), 10).classifyPile();

		assertThat(report).isEqualTo(new BodyClassificationRun.Report(2, 0, 0, 2, 0));
		then(this.normalized).should(never()).recordBodyDecisions(anyMap());
	}

	@Test
	void recordsWhatTheBodyDecidesFromTheStoredSegments() {
		holds(pile(1, "en", "Ship Go services."), pile(2, "en", "Weld pipes."),
				pile(3, "en", "We are hiring."));
		BodyClassificationRule rule = mock(BodyClassificationRule.class);
		given(rule.classify(anyString(), any(), anyList())).willReturn(ClassificationStateEnum.UNKNOWN);
		given(rule.classify("Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, segments("Ship Go services.")))
			.willReturn(ClassificationStateEnum.IN);
		given(rule.classify("Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, segments("Weld pipes.")))
			.willReturn(ClassificationStateEnum.OUT);

		BodyClassificationRun.Report report = run(rule, 10).classifyPile();

		assertThat(report).isEqualTo(new BodyClassificationRun.Report(3, 1, 1, 1, 0));
		assertThat(recorded()).containsExactlyInAnyOrderEntriesOf(
				Map.of(1L, ClassificationStateEnum.IN, 2L, ClassificationStateEnum.OUT));
	}

	@Test
	void readsAVacancyCleaningHasNotCutAsAnEmptyOne() {
		holds(new PileVacancy(1, "Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, "en", null));
		BodyClassificationRule rule = mock(BodyClassificationRule.class);
		given(rule.classify(any(), any(), any())).willReturn(ClassificationStateEnum.UNKNOWN);

		run(rule, 10).classifyPile();

		then(rule).should().classify("Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, List.of());
	}

	@Test
	void givesTheRuleTheReasonTheTitleWasLeftUnknown() {
		holds(new PileVacancy(1, "Product Manager", UnknownReasonEnum.SCOPE_AMBIGUITY, "en",
				segments("Own the roadmap.")));
		BodyClassificationRule rule = mock(BodyClassificationRule.class);
		given(rule.classify(any(), any(), any())).willReturn(ClassificationStateEnum.UNKNOWN);

		run(rule, 10).classifyPile();

		then(rule).should()
			.classify("Product Manager", UnknownReasonEnum.SCOPE_AMBIGUITY, segments("Own the roadmap."));
	}

	@Test
	void skipsAVacancyInALanguageOtherThanEnglishOrSpanishAndRecordsWhy() {
		holds(pile(1, "fr", "Développer des services."), pile(2, "es", "Desarrollar servicios."),
				pile(3, null, "Build services."), pile(4, "de", "Dienste entwickeln."));
		BodyClassificationRule rule = mock(BodyClassificationRule.class);
		given(rule.classify(any(), any(), any())).willReturn(ClassificationStateEnum.UNKNOWN);

		BodyClassificationRun.Report report = run(rule, 10).classifyPile();

		assertThat(report).isEqualTo(new BodyClassificationRun.Report(4, 0, 0, 2, 2));
		then(rule).should(times(2)).classify(any(), any(), any());
		then(this.normalized).should().recordUnsupportedLanguage(Set.of(1L, 4L));
	}

	@Test
	void skipsAVacancyAnEarlierRunAlreadySkippedForItsLanguage() {
		holds(new PileVacancy(1, "Engineer", UnknownReasonEnum.UNSUPPORTED_LANGUAGE, "fr", segments("Coder.")));
		BodyClassificationRule rule = mock(BodyClassificationRule.class);

		BodyClassificationRun.Report report = run(rule, 10).classifyPile();

		assertThat(report).isEqualTo(new BodyClassificationRun.Report(1, 0, 0, 0, 1));
		then(rule).shouldHaveNoInteractions();
	}

	@Test
	void walksThePileABatchAtATime() {
		holds(pile(1, "en", "a"), pile(2, "en", "b"), pile(3, "en", "c"));

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
		return new BodyClassificationRun(rule, this.normalized, batch);
	}

	private static PileVacancy pile(long vacancyId, String language, String description) {
		return new PileVacancy(vacancyId, "Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, language,
				segments(description));
	}

	private static List<Segment> segments(String description) {
		return List.of(new Segment(SegmentKindEnum.SENTENCE, null, description, false));
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
