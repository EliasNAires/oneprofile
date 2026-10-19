package oneprofile.backend.workers.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.TreeMap;
import oneprofile.backend.storage.normalizedvacancy.ExtractionVacancy;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
import oneprofile.backend.storage.vacancyskill.FoundSkill;
import oneprofile.backend.storage.vacancyskill.VacancySkillStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class SkillExtractionRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	private final VacancySkillStore skills = mock(VacancySkillStore.class);

	private final SkillExtractionRule rule = new SkillExtractionRule(TaxonomyStore.read(new StringReader("""
			go\tGo\tlanguage\tGolang\tGo\t
			postgresql\tPostgreSQL\tdatabase\tPostgres\t\t
			kubernetes\tKubernetes\tdevops\t\t\t
			""")));

	@Test
	void clearsWhatTheLastRunFoundBeforeReadingItsScope() {
		holds(vacancy(1, "Engineer", "Kubernetes."));

		run(10).extractSkills();

		InOrder order = inOrder(this.skills, this.normalized);
		order.verify(this.skills).clear();
		order.verify(this.normalized).extractionScopeAfter(0, 10);
	}

	@Test
	void recordsTheSkillsOfEachVacancyByItsDerivedFacts() {
		holds(vacancy(1, "Kubernetes Engineer", "Go and Postgres."), vacancy(2, "Engineer", "Sales."));

		run(10).extractSkills();

		assertThat(recorded()).isEqualTo(Map.of(101L,
				List.of(new FoundSkill("kubernetes", List.of(), true, false), new FoundSkill("go", List.of(0), false, true),
						new FoundSkill("postgresql", List.of(0), false, false))));
	}

	@Test
	void reportsTheSkillsFoundPerVacancyAndTheRowsFromContextKeys() {
		holds(vacancy(1, "Engineer", "Go and Postgres."), vacancy(2, "Engineer", "Kubernetes."),
				vacancy(3, "Engineer", "Sales."), vacancy(4, "Engineer", "Go on Kubernetes."));

		SkillExtractionRun.Report report = run(10).extractSkills();

		assertThat(report).isEqualTo(new SkillExtractionRun.Report(4, 5, 2, new TreeMap<>(Map.of(0, 1, 1, 1, 2, 2))));
	}

	@Test
	void walksItsScopeABatchAtATime() {
		holds(vacancy(1, "Engineer", "a"), vacancy(2, "Engineer", "b"), vacancy(3, "Engineer", "c"));

		assertThat(run(2).extractSkills().vacancies()).isEqualTo(3);

		then(this.normalized).should().extractionScopeAfter(0, 2);
		then(this.normalized).should().extractionScopeAfter(2, 2);
		then(this.normalized).should().extractionScopeAfter(3, 2);
	}

	@Test
	void refusesABatchThatHoldsNothing() {
		assertThatThrownBy(() -> run(0)).isInstanceOf(IllegalArgumentException.class);
	}

	private SkillExtractionRun run(int batch) {
		return new SkillExtractionRun(this.rule, this.normalized, this.skills, batch);
	}

	private static ExtractionVacancy vacancy(long vacancyId, String title, String description) {
		return new ExtractionVacancy(100 + vacancyId, vacancyId, title,
				List.of(new Segment(SegmentKindEnum.SENTENCE, null, description, false)));
	}

	private void holds(ExtractionVacancy... scope) {
		given(this.normalized.extractionScopeAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return List.of(scope).stream().filter((vacancy) -> vacancy.vacancyId() > after).limit(batch).toList();
		});
	}

	private Map<Long, List<FoundSkill>> recorded() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, List<FoundSkill>>> captor = ArgumentCaptor.forClass(Map.class);
		then(this.skills).should(Mockito.atLeastOnce()).record(captor.capture());
		Map<Long, List<FoundSkill>> all = new HashMap<>();
		captor.getAllValues().forEach(all::putAll);
		return all;
	}

}
