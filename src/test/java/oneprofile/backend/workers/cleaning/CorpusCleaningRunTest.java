package oneprofile.backend.workers.cleaning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.CleanedTitle;
import oneprofile.backend.storage.normalizedvacancy.CleanedVacancy;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import oneprofile.backend.storage.vacancy.VacancyStore;
import oneprofile.backend.storage.vacancy.VacancyText;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CorpusCleaningRunTest {

	private final VacancyStore vacancies = mock(VacancyStore.class);

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	@Test
	void cleansTheTitleOfEveryVacancyHeld() {
		holds(titled(1, "Senior Backend Engineer (m/w/d)"), titled(2, "Jr. Data Analyst"));

		assertThat(cleaning(10).cleanAll().vacancies()).isEqualTo(2);

		assertThat(recorded()).extracting(Map.Entry::getKey, (entry) -> entry.getValue().cleanedTitle())
			.containsExactly(tuple(1L, new CleanedTitle("Backend Engineer", Set.of(SeniorityLevelEnum.SENIOR))),
					tuple(2L, new CleanedTitle("Data Analyst", Set.of(SeniorityLevelEnum.JUNIOR))));
	}

	@Test
	void cutsTheCleanedDescriptionOfEveryVacancyIntoSegments() {
		holds(new VacancyText(1, "Backend Engineer", "# Requirements\n- 🚀 Java"), titled(2, "Designer"));

		cleaning(10).cleanAll();

		assertThat(recorded()).extracting(Map.Entry::getKey, (entry) -> entry.getValue().descriptionSegments())
			.containsExactly(
					tuple(1L, List.of(new Segment(SegmentKindEnum.HEADING, null, "Requirements", false),
							new Segment(SegmentKindEnum.ITEM, "Requirements", "Java", false))),
					tuple(2L, List.of()));
	}

	@Test
	void walksTheWholeCorpusABatchAtATime() {
		holds(titled(1, "Backend Engineer"), titled(2, "Frontend Engineer"),
				titled(3, "Data Engineer"));

		assertThat(cleaning(2).cleanAll().vacancies()).isEqualTo(3);

		then(this.vacancies).should().textsAfter(0, 2);
		then(this.vacancies).should().textsAfter(2, 2);
		then(this.vacancies).should().textsAfter(3, 2);
	}

	@Test
	void countsWhatEachRuleCollapsesOnItsOwn() {
		holds(titled(1, "Backend Engineer"), titled(2, "Backend Engineer (m/w/d)"),
				titled(3, "Backend Engineer!"), titled(4, "Senior Backend Engineer"),
				titled(5, "Data Analyst"));

		CorpusCleaningRun.Report report = cleaning(10).cleanAll();

		assertThat(report.distinctTitles()).isEqualTo(5);
		assertThat(report.collapsedBySpacing()).isZero();
		assertThat(report.collapsedByGenderMarkers()).isEqualTo(1);
		assertThat(report.collapsedByWhitelist()).isEqualTo(1);
		assertThat(report.collapsedBySeniorityWords()).isEqualTo(1);
		assertThat(report.distinctCleanedTitles()).isEqualTo(2);
	}

	@Test
	void creditsNoRuleWithTheSpacingEveryRuleMakesEven() {
		holds(titled(1, "Backend  Engineer"), titled(2, "Backend Engineer"));

		CorpusCleaningRun.Report report = cleaning(10).cleanAll();

		assertThat(report.distinctTitles()).isEqualTo(2);
		assertThat(report.collapsedBySpacing()).isEqualTo(1);
		assertThat(report.collapsedByGenderMarkers()).isZero();
		assertThat(report.collapsedByWhitelist()).isZero();
		assertThat(report.collapsedBySeniorityWords()).isZero();
	}

	@Test
	void countsATitleSeveralVacanciesShareOnce() {
		holds(titled(1, "Backend Engineer"), titled(2, "Backend Engineer"));

		CorpusCleaningRun.Report report = cleaning(10).cleanAll();

		assertThat(report.vacancies()).isEqualTo(2);
		assertThat(report.distinctTitles()).isEqualTo(1);
	}

	@Test
	void reportsAnEmptyCorpusAsNothingCleaned() {
		holds();

		assertThat(cleaning(10).cleanAll()).isEqualTo(new CorpusCleaningRun.Report(0, 0, 0, 0, 0, 0, 0));
	}

	@Test
	void refusesToWalkACorpusWithoutABatchToWalkItIn() {
		assertThatThrownBy(() -> cleaning(0)).isInstanceOf(IllegalArgumentException.class);
	}

	private CorpusCleaningRun cleaning(int batch) {
		return new CorpusCleaningRun(new TitleCleaningRule(), new DescriptionCleaningRule(),
				new DescriptionSplittingRule(), this.vacancies, this.normalized, batch);
	}

	/** Answers as the corpus does: the titles held after an id, in id order, at most a batch of them. */
	private static VacancyText titled(long id, String title) {
		return new VacancyText(id, title, null);
	}

	private void holds(VacancyText... held) {
		List<VacancyText> corpus = List.of(held);
		given(this.vacancies.textsAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return corpus.stream().filter((vacancy) -> vacancy.id() > after).limit(batch).toList();
		});
		given(this.normalized.recordCleanedVacancies(anyMap()))
			.willAnswer((invocation) -> ((Map<?, ?>) invocation.getArgument(0)).size());
	}

	private List<Map.Entry<Long, CleanedVacancy>> recorded() {
		ArgumentCaptor<Map<Long, CleanedVacancy>> batches = ArgumentCaptor.captor();
		then(this.normalized).should(Mockito.atLeastOnce()).recordCleanedVacancies(batches.capture());
		List<Map.Entry<Long, CleanedVacancy>> recorded = new ArrayList<>();
		batches.getAllValues().forEach((batch) -> recorded.addAll(batch.entrySet()));
		return recorded;
	}

}
