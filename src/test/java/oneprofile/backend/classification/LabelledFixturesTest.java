package oneprofile.backend.classification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import oneprofile.backend.normalizedvacancy.ClassificationState;
import org.junit.jupiter.api.Test;

/**
 * Holds the classifier to the accumulated hand labels in {@code src/test/resources/labels/}.
 * <p>
 * A vacancy decided {@code OUT} reaches neither the body pass nor a profile, so a title labelled
 * {@code IN} that the rules decide {@code OUT} is the one error nothing downstream recovers. A rule
 * change may not raise that count. When a change lowers it, lower the ceiling with it.
 */
class LabelledFixturesTest {

	private static final int LABELLED_IN_DECIDED_OUT_CEILING = 19;

	private final TitleClassification classification = new TitleClassification();

	@Test
	void noRuleChangeDecidesOutMoreTitlesTheLabelsCallIn() throws IOException, URISyntaxException {
		List<String> labelledInDecidedOut = labelledRows()
			.filter((row) -> row[2].equals("IN"))
			.filter((row) -> this.classification.classify(row[1]).state() == ClassificationState.OUT)
			.map((row) -> row[0] + "\t" + row[1])
			.toList();

		assertThat(labelledInDecidedOut).as(String.join("\n", labelledInDecidedOut))
			.hasSizeLessThanOrEqualTo(LABELLED_IN_DECIDED_OUT_CEILING);
	}

	private static Stream<String[]> labelledRows() throws IOException, URISyntaxException {
		Path labels = Path.of(LabelledFixturesTest.class.getResource("/labels").toURI());
		try (Stream<Path> fixtures = Files.list(labels)) {
			List<Path> tsvs = fixtures.filter((fixture) -> fixture.toString().endsWith(".tsv")).sorted().toList();
			Stream<String[]> rows = Stream.empty();
			for (Path tsv : tsvs) {
				rows = Stream.concat(rows, Files.readAllLines(tsv)
					.stream()
					.filter((line) -> !line.isBlank() && !line.startsWith("#"))
					.map((line) -> line.split("\t", -1)));
			}
			return rows;
		}
	}

}
