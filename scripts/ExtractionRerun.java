import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
import oneprofile.backend.storage.vacancyskill.FoundSkill;
import oneprofile.backend.workers.extraction.SkillExtractionRule;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs skill extraction's own rule again over vacancies read from stdin, and writes what it finds
 * with the one thing vacancy_skill does not store: whether only context keys found the skill. Run by
 * draw-extraction-sample, never on its own.
 *
 * Reads one vacancy a line, as {"id": normalized vacancy id, "title": cleaned title, "segments": [its
 * stored segments]}, and writes one finding a line, as
 * id<TAB>skill id<TAB>segment indexes, comma-separated<TAB>in title (t/f)<TAB>from context keys (t/f).
 */
class ExtractionRerun {

	record Vacancy(long id, String title, List<Segment> segments) {
	}

	public static void main(String[] args) throws Exception {
		SkillExtractionRule rule;
		try (Reader tsv = Files.newBufferedReader(Path.of(args[0]))) {
			rule = new SkillExtractionRule(TaxonomyStore.read(tsv));
		}
		JsonMapper json = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
		PrintStream out = new PrintStream(System.out, false, StandardCharsets.UTF_8);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
		String line;
		while ((line = in.readLine()) != null) {
			Vacancy vacancy = json.readValue(line, Vacancy.class);
			for (FoundSkill found : rule.extract(vacancy.title(), vacancy.segments())) {
				out.println(vacancy.id() + "\t" + found.skillId() + "\t"
						+ found.segments().stream().map(String::valueOf).collect(Collectors.joining(",")) + "\t"
						+ (found.inTitle() ? "t" : "f") + "\t" + (found.fromContextKeys() ? "t" : "f"));
			}
		}
		out.flush();
	}

}
