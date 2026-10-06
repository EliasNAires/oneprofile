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
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.normalizedvacancy.UnknownReasonEnum;
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
	void writesEveryVacancyOfThePileAsOneLineWithItsDescriptionOneSegmentALine() throws IOException {
		holds(new PileVacancy(7, "Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, "en", List.of(
				new Segment(SegmentKindEnum.SENTENCE, null, "We build payments.", true),
				new Segment(SegmentKindEnum.HEADING, null, "Requirements", false),
				new Segment(SegmentKindEnum.ITEM, "Requirements", "Build APIs in Go.", false),
				new Segment(SegmentKindEnum.SENTENCE, "Requirements", "You will ship weekly.", false))),
				new PileVacancy(9, "Product Manager", UnknownReasonEnum.SCOPE_AMBIGUITY, null, null));
		Path file = this.directory.resolve("pile.jsonl");

		export(10).exportTo(file);

		assertThat(lines(file)).containsExactly(
				Map.of("vacancy_id", 7, "cleaned_title", "Engineer", "title_reason", "DOMAIN_AMBIGUITY",
						"cleaned_description",
						"> We build payments.\n# Requirements\n- Build APIs in Go.\nYou will ship weekly."),
				Map.of("vacancy_id", 9, "cleaned_title", "Product Manager", "title_reason", "SCOPE_AMBIGUITY",
						"cleaned_description", ""));
	}

	@Test
	void leavesOutWhatTheBodyPassSkipsForItsLanguage() throws IOException {
		holds(pile(1, "Construire des API."), new PileVacancy(2, "Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, "fr",
				segments("Construire des API.")), new PileVacancy(3, "Engineer",
						UnknownReasonEnum.UNSUPPORTED_LANGUAGE, "de", segments("APIs bauen.")));
		Path file = this.directory.resolve("pile.jsonl");

		PileExportRun.Report report = export(10).exportTo(file);

		assertThat(lines(file)).extracting((line) -> line.get("vacancy_id")).containsExactly(1);
		assertThat(report.vacancies()).isEqualTo(1);
		assertThat(report.unsupportedLanguage()).isEqualTo(2);
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

		assertThat(report).isEqualTo(new PileExportRun.Report(this.directory.resolve("pile.jsonl").toString(), 20, 0,
				420, 760, 105, 190));
	}

	private PileExportRun export(int batch) {
		return new PileExportRun(this.normalized, this.json, batch);
	}

	private static PileVacancy pile(long vacancyId, String description) {
		return new PileVacancy(vacancyId, "Engineer", UnknownReasonEnum.DOMAIN_AMBIGUITY, "en", segments(description));
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
	}

	private List<Map<String, Object>> lines(Path file) throws IOException {
		return Files.readAllLines(file)
			.stream()
			.map((line) -> this.json.readValue(line, new TypeReference<Map<String, Object>>() {
			}))
			.toList();
	}

}
