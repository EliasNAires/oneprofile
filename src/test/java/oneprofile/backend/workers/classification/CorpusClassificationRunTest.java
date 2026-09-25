package oneprofile.backend.workers.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.normalizedvacancy.Classification;
import oneprofile.backend.storage.normalizedvacancy.NormalizedTitle;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CorpusClassificationRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	@Test
	void classifiesTheCleanedTitleOfEveryVacancyHeld() {
		holds(new NormalizedTitle(1, "Backend Engineer"), new NormalizedTitle(2, "Registered Nurse"));

		assertThat(classification(10).classifyAll().vacancies()).isEqualTo(2);

		assertThat(recorded()).containsExactly(Map.entry(1L, Classification.in()),
				Map.entry(2L, Classification.out()));
	}

	@Test
	void walksTheWholeCorpusABatchAtATime() {
		holds(new NormalizedTitle(1, "Backend Engineer"), new NormalizedTitle(2, "Frontend Engineer"),
				new NormalizedTitle(3, "Data Engineer"));

		assertThat(classification(2).classifyAll().vacancies()).isEqualTo(3);

		then(this.normalized).should().cleanedTitlesAfter(0, 2);
		then(this.normalized).should().cleanedTitlesAfter(2, 2);
		then(this.normalized).should().cleanedTitlesAfter(3, 2);
	}

	@Test
	void countsWhatItDecidedAndWhyItDidNot() {
		holds(new NormalizedTitle(1, "Backend Engineer"), new NormalizedTitle(2, "Civil Engineer"),
				new NormalizedTitle(3, "Engineer"), new NormalizedTitle(4, "Security Roboticist"),
				new NormalizedTitle(5, "Data Analyst"));

		CorpusClassificationRun.Report report = classification(10).classifyAll();

		assertThat(report.in()).isEqualTo(1);
		assertThat(report.out()).isEqualTo(1);
		assertThat(report.unknown()).isEqualTo(3);
		assertThat(report.domainAmbiguity()).isEqualTo(1);
		assertThat(report.unruled()).isEqualTo(1);
		assertThat(report.scopeAmbiguity()).isEqualTo(1);
	}

	@Test
	void refusesABatchThatHoldsNothing() {
		assertThatThrownBy(() -> classification(0)).isInstanceOf(IllegalArgumentException.class);
	}

	private CorpusClassificationRun classification(int batch) {
		return new CorpusClassificationRun(new TitleClassificationRule(), this.normalized, batch);
	}

	private void holds(NormalizedTitle... titles) {
		given(this.normalized.cleanedTitlesAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return List.of(titles)
				.stream()
				.filter((title) -> title.vacancyId() > after)
				.limit(batch)
				.toList();
		});
		given(this.normalized.recordClassifications(anyMap())).willAnswer((invocation) -> {
			Map<?, ?> recorded = invocation.getArgument(0);
			return recorded.size();
		});
	}

	private List<Map.Entry<Long, Classification>> recorded() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, Classification>> captor = ArgumentCaptor.forClass(Map.class);
		then(this.normalized).should(Mockito.atLeastOnce()).recordClassifications(captor.capture());
		List<Map.Entry<Long, Classification>> entries = new ArrayList<>();
		captor.getAllValues().forEach((batch) -> entries.addAll(batch.entrySet()));
		return entries;
	}

}
