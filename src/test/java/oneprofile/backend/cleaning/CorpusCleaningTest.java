package oneprofile.backend.cleaning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import oneprofile.backend.normalizedvacancy.CleanedTitle;
import oneprofile.backend.normalizedvacancy.NormalizedVacancies;
import oneprofile.backend.normalizedvacancy.SeniorityLevel;
import oneprofile.backend.vacancy.Vacancies;
import oneprofile.backend.vacancy.VacancyTitle;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CorpusCleaningTest {

	private final Vacancies vacancies = mock(Vacancies.class);

	private final NormalizedVacancies normalized = mock(NormalizedVacancies.class);

	@Test
	void cleansTheTitleOfEveryVacancyHeld() {
		holds(new VacancyTitle(1, "Senior Backend Engineer (m/w/d)"), new VacancyTitle(2, "Jr. Data Analyst"));

		assertThat(cleaning(10).cleanAll().vacancies()).isEqualTo(2);

		assertThat(recorded()).containsExactly(
				Map.entry(1L, new CleanedTitle("Backend Engineer", Set.of(SeniorityLevel.SENIOR))),
				Map.entry(2L, new CleanedTitle("Data Analyst", Set.of(SeniorityLevel.JUNIOR))));
	}

	@Test
	void walksTheWholeCorpusABatchAtATime() {
		holds(new VacancyTitle(1, "Backend Engineer"), new VacancyTitle(2, "Frontend Engineer"),
				new VacancyTitle(3, "Data Engineer"));

		assertThat(cleaning(2).cleanAll().vacancies()).isEqualTo(3);

		then(this.vacancies).should().titlesAfter(0, 2);
		then(this.vacancies).should().titlesAfter(2, 2);
		then(this.vacancies).should().titlesAfter(3, 2);
	}

	@Test
	void countsWhatEachRuleCollapsesOnItsOwn() {
		holds(new VacancyTitle(1, "Backend Engineer"), new VacancyTitle(2, "Backend Engineer (m/w/d)"),
				new VacancyTitle(3, "Backend Engineer!"), new VacancyTitle(4, "Senior Backend Engineer"),
				new VacancyTitle(5, "Data Analyst"));

		CorpusCleaning.Report report = cleaning(10).cleanAll();

		assertThat(report.distinctTitles()).isEqualTo(5);
		assertThat(report.collapsedBySpacing()).isZero();
		assertThat(report.collapsedByGenderMarkers()).isEqualTo(1);
		assertThat(report.collapsedByWhitelist()).isEqualTo(1);
		assertThat(report.collapsedBySeniorityWords()).isEqualTo(1);
		assertThat(report.distinctCleanedTitles()).isEqualTo(2);
	}

	@Test
	void creditsNoRuleWithTheSpacingEveryRuleMakesEven() {
		holds(new VacancyTitle(1, "Backend  Engineer"), new VacancyTitle(2, "Backend Engineer"));

		CorpusCleaning.Report report = cleaning(10).cleanAll();

		assertThat(report.distinctTitles()).isEqualTo(2);
		assertThat(report.collapsedBySpacing()).isEqualTo(1);
		assertThat(report.collapsedByGenderMarkers()).isZero();
		assertThat(report.collapsedByWhitelist()).isZero();
		assertThat(report.collapsedBySeniorityWords()).isZero();
	}

	@Test
	void countsATitleSeveralVacanciesShareOnce() {
		holds(new VacancyTitle(1, "Backend Engineer"), new VacancyTitle(2, "Backend Engineer"));

		CorpusCleaning.Report report = cleaning(10).cleanAll();

		assertThat(report.vacancies()).isEqualTo(2);
		assertThat(report.distinctTitles()).isEqualTo(1);
	}

	@Test
	void reportsAnEmptyCorpusAsNothingCleaned() {
		holds();

		assertThat(cleaning(10).cleanAll()).isEqualTo(new CorpusCleaning.Report(0, 0, 0, 0, 0, 0, 0));
	}

	@Test
	void refusesToWalkACorpusWithoutABatchToWalkItIn() {
		assertThatThrownBy(() -> cleaning(0)).isInstanceOf(IllegalArgumentException.class);
	}

	private CorpusCleaning cleaning(int batch) {
		return new CorpusCleaning(new TitleCleaning(), this.vacancies, this.normalized, batch);
	}

	/** Answers as the corpus does: the titles held after an id, in id order, at most a batch of them. */
	private void holds(VacancyTitle... held) {
		List<VacancyTitle> corpus = List.of(held);
		given(this.vacancies.titlesAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return corpus.stream().filter((vacancy) -> vacancy.id() > after).limit(batch).toList();
		});
		given(this.normalized.recordCleanedTitles(anyMap()))
			.willAnswer((invocation) -> ((Map<?, ?>) invocation.getArgument(0)).size());
	}

	private List<Map.Entry<Long, CleanedTitle>> recorded() {
		ArgumentCaptor<Map<Long, CleanedTitle>> batches = ArgumentCaptor.captor();
		then(this.normalized).should(Mockito.atLeastOnce()).recordCleanedTitles(batches.capture());
		List<Map.Entry<Long, CleanedTitle>> recorded = new ArrayList<>();
		batches.getAllValues().forEach((batch) -> recorded.addAll(batch.entrySet()));
		return recorded;
	}

}
