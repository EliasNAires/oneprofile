package oneprofile.backend.workers.stoplist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.normalizedvacancy.SegmentedVacancy;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoplistRunTest {

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	private TaxonomyStore taxonomy = TaxonomyStore.read(new StringReader(""));

	@TempDir
	private Path files;

	private Path decisions;

	private Path labels;

	private Path answers;

	private Path otherLedger;

	private Path stoplist;

	private final List<String> asked = Collections.synchronizedList(new ArrayList<>());

	/** Jev's probability of no for a word, when it is not {@link #otherwise}. */
	private final Map<String, Double> nos = new HashMap<>();

	private double otherwise = 0.9;

	private final JevPort jev = (word) -> {
		this.asked.add(word);
		double no = this.nos.getOrDefault(word, this.otherwise);
		return new JevAnswer((no >= 0.5) ? "no" : "yes", no, Math.round((1 - no) * 100) / 100.0, JevPort.MODEL, 100);
	};

	private double spendLimit = 4.0;

	private int chunk = 200;

	@BeforeEach
	void emptyRecords() throws IOException {
		this.decisions = Files.writeString(this.files.resolve("decisions.tsv"),
				"name\tdecision\trevision\tsnapshot\tsource\n");
		this.labels = Files.writeString(this.files.resolve("labels.jsonl"), "");
		this.answers = this.files.resolve("stoplist-labels.jsonl");
		this.otherLedger = Files.writeString(this.files.resolve("body-labels.jsonl"), "");
		this.stoplist = this.files.resolve("stopwords.tsv");
	}

	@Test
	void countsTheNameLikeWordsAndTheLowercaseOnesOutsideTheKnownVocabularyAsPieces() {
		holds(vacancy(1, "Build APIs in Go and k8s.", "Strong deployment skills."));

		StoplistRun.Report report = run(10).measure();

		assertThat(report.top()).containsExactlyInAnyOrder(piece("build", 1), piece("apis", 1), piece("in", 1),
				piece("go", 1), piece("and", 1), piece("k8s", 1), piece("strong", 1), piece("deployment", 1),
				piece("skills", 1));
		assertThat(report.pieces()).isEqualTo(9);
		assertThat(report.words()).isEqualTo(9);
	}

	@Test
	void countsALowercaseWordWrittenInTenVacanciesOnlyWhereItIsWrittenCapitalized() {
		List<SegmentedVacancy> corpus = new ArrayList<>();
		for (int vacancyId = 1; vacancyId <= 10; vacancyId++) {
			corpus.add(vacancy(vacancyId, "with experience"));
		}
		corpus.add(vacancy(11, "Experience with rust"));
		holds(corpus.toArray(SegmentedVacancy[]::new));

		StoplistRun.Report report = run(4).measure();

		assertThat(report.top()).containsExactly(piece("experience", 1), piece("rust", 1));
		assertThat(report.pieces()).isEqualTo(2);
	}

	@Test
	void ranksTheWordsByTheirPiecesAndTakesTheFewestThatCoverEightyPercentOfThemForTheHead() {
		holds(vacancy(1, Stream.of(repeat("Go", 5), repeat("Rust", 3), repeat("Java", 1), repeat("Scala", 1))
			.flatMap(List::stream)
			.toArray(String[]::new)));

		StoplistRun.Report report = run(10).measure();

		assertThat(report.top()).containsExactly(piece("go", 5), piece("rust", 3), piece("java", 1),
				piece("scala", 1));
		assertThat(report.head()).isEqualTo(2);
	}

	@Test
	void reportsWhatTheClosedClassRemovesAndTheHeadOfWhatIsLeft() {
		holds(vacancy(1, Stream.of(repeat("The", 6), repeat("Go", 2), repeat("Rust", 1), repeat("Java", 1))
			.flatMap(List::stream)
			.toArray(String[]::new)));

		StoplistRun.Report report = run(10).measure();

		assertThat(report.closedClassPieces()).isEqualTo(6);
		assertThat(report.closedClassShare()).isEqualTo(0.6);
		assertThat(report.headOfTheRest()).isEqualTo(3);
	}

	@Test
	void walksTheCorpusABatchAtATime() {
		holds(vacancy(1, "Go"), vacancy(2, "Go"), vacancy(3, "Go"));

		assertThat(run(2).measure().pieces()).isEqualTo(3);

		then(this.normalized).should().inAndPileAfter(0, 2);
		then(this.normalized).should().inAndPileAfter(2, 2);
		then(this.normalized).should().inAndPileAfter(3, 2);
	}

	@Test
	void assemblesTheGuardSetFromTheKeysTheReopenedFormsWhatJevFoundOrKeptAndTheDecisionRecordsKeeps() throws IOException {
		this.taxonomy = TaxonomyStore.read(new StringReader("golang\tGo\tlanguage\tGolang|Go language\n"));
		Files.writeString(this.decisions, """
				name\tdecision\trevision\tsnapshot\tsource
				rust\tkeep\t0\traw-2026-09-20\t#38
				team\tdrop\t0\traw-2026-09-20\t#38
				apache spark\tkeep\t0\traw-2026-09-20\t#38
				""");
		Files.writeString(this.labels, """
				{"vacancy_id": "1", "segment": "Excel and Go", "found": ["Excel", "Power BI"], "answer": "missed", "criterion_revision": "1"}
				{"name_form": "code", "name": "code", "segment": "Code in Go", "decision": "keep", "criterion_revision": "5"}
				{"name_form": "work", "name": "Work", "segment": "Work in Go", "decision": "drop", "criterion_revision": "5"}
				{"name_form": "vs code", "name": "VS Code", "segment": "VS Code", "decision": "keep", "criterion_revision": "5"}
				""");
		holds(vacancy(1, "Go"));

		// go, golang, rust, code and excel, with go among the 47 reopened forms too
		assertThat(run(10).measure().guardSetSize()).isEqualTo(GuardSetRule.REOPENED.size() + 4);
	}

	@Test
	void setsTheCutoffAtTheLowestCapitalizedShareOfTheGuardSetAndCountsTheHeadWordsBelowIt() throws IOException {
		Files.writeString(this.labels, """
				{"name_form": "code", "name": "code", "segment": "Use code", "decision": "keep", "criterion_revision": "5"}
				""");
		holds(vacancy(1, "Use Go and Code with experience", "Use Go, code, experience and team",
				"Use code with experience and team", "Use code with experience"));

		StoplistRun.Report report = run(10).measure();

		// code is written capitalized in 1 of its 4 mid-sentence mentions, Go in both of its own.
		assertThat(report.cutoff()).isEqualTo(0.25);
		assertThat(report.guardAtTheCutoff()).containsExactly(share("code", 0.25, 4));
		// The head is code, experience, use and go: experience is never capitalized, use is never written
		// mid-sentence, and team is below the cutoff but outside the head.
		assertThat(report.headOfTheRest()).isEqualTo(4);
		assertThat(report.capitalizationLetsThrough()).isEqualTo(1);
	}

	@Test
	void asksJevAboutEveryWordOfTheHeadOfTheRestAndOfTheGuardSetAndLedgersEachAnswer() throws IOException {
		this.taxonomy = TaxonomyStore.read(new StringReader("rust\tRust\tlanguage\t\n"));
		holds(vacancy(1, Stream.of(repeat("The", 6), repeat("Go", 5), repeat("Team", 3), repeat("Scala", 1))
			.flatMap(List::stream)
			.toArray(String[]::new)));

		StoplistRun.Report report = run(10).measure();

		// The head of the rest is go and team; the guard set is rust and the 47 reopened forms, go among them.
		assertThat(this.asked).hasSize(GuardSetRule.REOPENED.size() + 2)
			.contains("go", "team", "rust")
			.doesNotContain("the", "scala")
			.doesNotHaveDuplicates();
		assertThat(report.jevWords()).isEqualTo(this.asked.size());
		assertThat(report.jevCalls()).isEqualTo(this.asked.size());
		assertThat(report.jevCost()).isCloseTo(this.asked.size() * 100 * 0.042 / 1_000_000, within(1e-12));
		assertThat(report.jevStopped()).isFalse();
		assertThat(Files.readAllLines(this.answers)).hasSize(this.asked.size())
			.contains("{\"word\":\"team\",\"answer\":\"no\",\"probabilities\":{\"no\":0.9,\"yes\":0.1},"
					+ "\"model\":\"jev-1.13.0\",\"input_tokens\":100}");
	}

	@Test
	void asksNothingThePinnedModelHasAnsweredBefore() throws IOException {
		Files.writeString(this.answers, """
				{"word":"go","answer":"yes","probabilities":{"no":0.0,"yes":1.0},"model":"jev-1.13.0","input_tokens":100}
				{"word":"make","answer":"yes","probabilities":{"no":0.0,"yes":1.0},"model":"jev-1.12.0","input_tokens":100}
				""");
		holds(vacancy(1, "Go"));

		StoplistRun.Report report = run(10).measure();

		assertThat(this.asked).hasSize(GuardSetRule.REOPENED.size() - 1).contains("make").doesNotContain("go");
		assertThat(report.jevWords()).isEqualTo(GuardSetRule.REOPENED.size());
		assertThat(report.jevCalls()).isEqualTo(this.asked.size());
	}

	@Test
	void stopsBetweenChunksOnceJevHasCostTheSpendLimitAcrossEveryLedger() throws IOException {
		Files.writeString(this.otherLedger, "{\"input_tokens\": 900}\n");
		this.spendLimit = 1000 * 0.042 / 1_000_000;
		this.chunk = 2;
		holds(vacancy(1, "Go"));

		StoplistRun.Report report = run(10).measure();

		assertThat(this.asked).hasSize(2);
		assertThat(report.jevCalls()).isEqualTo(2);
		assertThat(report.jevStopped()).isTrue();
		assertThat(report.jevSpent()).isCloseTo(1100 * 0.042 / 1_000_000, within(1e-12));
	}

	@Test
	void asksNothingOnceTheSpendLimitIsReached() throws IOException {
		Files.writeString(this.otherLedger, "{\"input_tokens\": 1000}\n");
		this.spendLimit = 1000 * 0.042 / 1_000_000;
		holds(vacancy(1, "Go"));

		StoplistRun.Report report = run(10).measure();

		assertThat(this.asked).isEmpty();
		assertThat(report.jevStopped()).isTrue();
		assertThat(this.stoplist).doesNotExist();
	}

	@Test
	void raisesJevsBarUntilNoGuardWordClearsItAndStoplistsTheClosedClassAndTheHeadWordsPastBothGuards()
			throws IOException {
		codeIsKeptInContext();
		this.otherwise = 0.0;
		this.nos.put("experience", 0.9);
		this.nos.put("code", 0.85);
		this.nos.put("use", 0.95);
		holds(vacancy(1, "Use Go and Code with experience", "Use Go, code, experience and team",
				"Use code with experience and team", "Use code with experience"));

		StoplistRun.Report report = run(10).measure();

		// The cutoff is code's 0.25. Use is never written mid-sentence, so only experience passes it.
		assertThat(report.bar()).isEqualTo(0.86);
		assertThat(Files.readAllLines(this.stoplist)).containsExactly("word\tpieces\tcapitalized_share\tjev_no\tlayer",
				"experience\t4\t0.0000\t0.90\thead", "and\t3\t0.0000\t\tclosed-class",
				"with\t3\t0.0000\t\tclosed-class");
		assertThat(report.stoplisted()).isEqualTo(3);
		assertThat(report.coverage()).isEqualTo(10 / 22.0);
		assertThat(report.guardCheck()).isEmpty();
		assertThat(report.blocked()).containsExactly(new StoplistRun.HeadWord("code", 4, 0.25, 0.85),
				new StoplistRun.HeadWord("use", 4, null, 0.95), new StoplistRun.HeadWord("go", 2, 1.0, 0.0));
	}

	@Test
	void startsJevsBarAtEightyPercent() throws IOException {
		codeIsKeptInContext();
		this.otherwise = 0.0;
		this.nos.put("experience", 0.8);
		holds(vacancy(1, "Use Code with experience", "Use code with experience", "Use code with experience",
				"Use code with experience"));

		StoplistRun.Report report = run(10).measure();

		assertThat(report.bar()).isEqualTo(0.8);
		assertThat(Files.readAllLines(this.stoplist)).contains("experience\t4\t0.0000\t0.80\thead");
	}

	@Test
	void reportsTheStoplistedWordsThatAreInTheGuardSet() throws IOException {
		Files.writeString(this.decisions, "name\tdecision\trevision\tsnapshot\tsource\nwith\tkeep\t0\traw\t#38\n");
		holds(vacancy(1, "Go with Rust"));

		assertThat(run(10).measure().guardCheck()).containsExactly("with");
	}

	private void codeIsKeptInContext() throws IOException {
		Files.writeString(this.labels, """
				{"name_form": "code", "name": "code", "segment": "Use code", "decision": "keep", "criterion_revision": "5"}
				""");
	}

	private StoplistRun run(int batch) {
		return new StoplistRun(this.normalized, this.taxonomy, this.jev, this.decisions, this.labels, this.answers,
				List.of(this.otherLedger), this.stoplist, this.spendLimit, batch, this.chunk);
	}

	private static StoplistRun.WordShare share(String word, double share, long midSentence) {
		return new StoplistRun.WordShare(word, share, midSentence);
	}

	private static StoplistRun.WordPieces piece(String word, long pieces) {
		return new StoplistRun.WordPieces(word, pieces);
	}

	private static List<String> repeat(String word, int times) {
		return Collections.nCopies(times, word);
	}

	private static SegmentedVacancy vacancy(long vacancyId, String... sentences) {
		return new SegmentedVacancy(vacancyId, Stream.of(sentences)
			.map((sentence) -> new Segment(SegmentKindEnum.SENTENCE, null, sentence, false))
			.toList());
	}

	private void holds(SegmentedVacancy... corpus) {
		given(this.normalized.inAndPileAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return Stream.of(corpus).filter((vacancy) -> vacancy.vacancyId() > after).limit(batch).toList();
		});
	}

}
