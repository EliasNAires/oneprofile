package oneprofile.backend.workers.classification;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.PileVacancy;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the pile to a file, one JSON object a line, each vacancy with its description for the
 * labeller to read (ADR-0012): the segments cleaning stored, one a line, marked as the sweep marks
 * a heading, an item and boilerplate. The body pass reads the same segments, so a label and a rule
 * never disagree about their input. A vacancy the body pass skips for its language is left out, since
 * nothing scores it.
 * <p>
 * It also measures what the labeller's budget rests on: how long a description is, in characters
 * and in tokens, estimated at four characters a token.
 */
@Component
public class PileExportRun {

	private static final int BATCH = 1000;

	private static final int CHARACTERS_PER_TOKEN = 4;

	private final NormalizedVacancyStore normalized;

	private final JsonMapper json;

	private final int batch;

	/**
	 * Exports the pile in batches of a thousand.
	 * @param normalized where the pile is read
	 * @param json what writes each line
	 */
	@Autowired
	public PileExportRun(NormalizedVacancyStore normalized, JsonMapper json) {
		this(normalized, json, BATCH);
	}

	/**
	 * @param normalized where the pile is read
	 * @param json what writes each line
	 * @param batch how many vacancies are read at a time
	 */
	PileExportRun(NormalizedVacancyStore normalized, JsonMapper json, int batch) {
		this.normalized = normalized;
		this.json = json;
		this.batch = batch;
	}

	/**
	 * Writes every vacancy of the pile, replacing whatever the file held.
	 * @param file where to write it
	 * @return how much was written, and how long its descriptions are
	 */
	public Report exportTo(Path file) {
		IntStream.Builder lengths = IntStream.builder();
		int unsupportedLanguage = 0;
		try (BufferedWriter writer = Files.newBufferedWriter(file)) {
			long after = 0;
			while (true) {
				List<PileVacancy> read = this.normalized.pileAfter(after, this.batch);
				if (read.isEmpty()) {
					break;
				}
				for (PileVacancy vacancy : read) {
					after = vacancy.vacancyId();
					if (!vacancy.inSupportedLanguage()) {
						unsupportedLanguage++;
						continue;
					}
					String description = vacancy.segments()
						.stream()
						.map(Segment::marked)
						.collect(Collectors.joining("\n"));
					Map<String, Object> line = new LinkedHashMap<>();
					line.put("vacancy_id", vacancy.vacancyId());
					line.put("cleaned_title", vacancy.cleanedTitle());
					line.put("title_reason", vacancy.titleReason().name());
					line.put("cleaned_description", description);
					writer.write(this.json.writeValueAsString(line));
					writer.newLine();
					lengths.add(description.length());
				}
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		int[] sorted = lengths.build().sorted().toArray();
		int mean = (int) Math.round(Arrays.stream(sorted).average().orElse(0));
		int p95 = (sorted.length == 0) ? 0 : sorted[(int) Math.ceil(0.95 * sorted.length) - 1];
		return new Report(file.toString(), sorted.length, unsupportedLanguage, mean, p95, mean / CHARACTERS_PER_TOKEN,
				p95 / CHARACTERS_PER_TOKEN);
	}

	/**
	 * What one export wrote.
	 *
	 * @param file where it was written
	 * @param vacancies how many vacancies of the pile it holds
	 * @param unsupportedLanguage how many it left out, since they are in a language other than English
	 * or Spanish
	 * @param meanDescriptionCharacters the mean length of a description, one segment a line
	 * @param p95DescriptionCharacters the length 95% of descriptions are no longer than
	 * @param meanDescriptionTokens the mean length in tokens, at four characters a token
	 * @param p95DescriptionTokens the 95th percentile in tokens, at four characters a token
	 */
	public record Report(String file, int vacancies, int unsupportedLanguage, int meanDescriptionCharacters,
			int p95DescriptionCharacters, int meanDescriptionTokens, int p95DescriptionTokens) {
	}

}
