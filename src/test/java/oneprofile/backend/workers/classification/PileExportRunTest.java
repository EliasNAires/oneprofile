package oneprofile.backend.workers.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.PileVacancy;
import oneprofile.backend.storage.normalizedvacancy.UnknownReasonEnum;
import oneprofile.backend.workers.cleaning.DescriptionCleaningRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class PileExportRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	private final JsonMapper json = JsonMapper.builder().build();

	@TempDir
	private Path directory;

	@Test
	void writesEveryVacancyOfThePileAsOneLineWithItsCleanedDescription() throws IOException {
		holds(new PileVacancy(7, "Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, "Build 🚀 APIs  in Go."),
				new PileVacancy(9, "Product Manager", UnknownReasonEnum.SCOPE_AMBIGUITY, null));
		Path file = this.directory.resolve("pile.jsonl");

		export(10).exportTo(file);

		assertThat(lines(file)).containsExactly(
				Map.of("vacancy_id", 7, "cleaned_title", "Engineer", "title_reason", "DOMAIN_AMBIGUITY",
						"cleaned_description", "Build APIs in Go."),
				Map.of("vacancy_id", 9, "cleaned_title", "Product Manager", "title_reason", "SCOPE_AMBIGUITY",
						"cleaned_description", ""));
	}

	@Test
	void walksThePileABatchAtATime() {
		holds(pile(1, "a"), pile(2, "b"), pile(3, "c"));

		assertThat(export(2).exportTo(this.directory.resolve("pile.jsonl")).vacancies()).isEqualTo(3);

		then(this.normalized).should().pileAfter(0, 2);
		then(this.normalized).should().pileAfter(2, 2);
		then(this.normalized).should().pileAfter(3, 2);
	}

	@Test
	void estimatesTheTokensOfTheDescriptionsAtFourCharactersEach() {
		PileVacancy[] pile = new PileVacancy[20];
		for (int i = 0; i < 20; i++) {
			pile[i] = pile(i + 1, "x".repeat(40 * (i + 1)));
		}
		holds(pile);

		PileExportRun.Report report = export(10).exportTo(this.directory.resolve("pile.jsonl"));

		assertThat(report).isEqualTo(new PileExportRun.Report(this.directory.resolve("pile.jsonl").toString(), 20,
				420, 760, 105, 190));
	}

	private PileExportRun export(int batch) {
		return new PileExportRun(new DescriptionCleaningRule(), this.normalized, this.json, batch);
	}

	private static PileVacancy pile(long vacancyId, String description) {
		return new PileVacancy(vacancyId, "Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, description);
	}

	private void holds(PileVacancy... pile) {
		given(this.normalized.pileAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return List.of(pile).stream().filter((vacancy) -> vacancy.vacancyId() > after).limit(batch).toList();
		});
	}

	private List<Map<String, Object>> lines(Path file) throws IOException {
		return Files.readAllLines(file)
			.stream()
			.map((line) -> this.json.readValue(line, new TypeReference<Map<String, Object>>() {
			}))
			.toList();
	}

}
