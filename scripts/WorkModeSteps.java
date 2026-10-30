import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import oneprofile.backend.workers.normalization.WorkModeReading;
import oneprofile.backend.workers.normalization.WorkModeRule;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads, with the application's own rule, which step of the order of evidence would decide the work mode of
 * vacancies read from stdin by their location, field and title alone: the vacancies it finds none for are the
 * undeclared ones, whose mode only the description can give (#98). Run by draw-work-mode-description-sample,
 * never on its own.
 *
 * Reads one vacancy a line, as {"id": vacancy id, "location": ..., "field": ..., "title": ...}, and writes one
 * line a vacancy, as id<TAB>step, the step being LOCATION, FIELD, TITLE or NONE.
 */
class WorkModeSteps {

	record Vacancy(long id, String location, String field, String title) {
	}

	public static void main(String[] args) throws Exception {
		WorkModeRule rule = new WorkModeRule(new TitleCleaningRule());
		JsonMapper json = JsonMapper.builder().build();
		PrintStream out = new PrintStream(System.out, false, StandardCharsets.UTF_8);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
		String line;
		while ((line = in.readLine()) != null) {
			Vacancy vacancy = json.readValue(line, Vacancy.class);
			WorkModeReading reading = rule.read(vacancy.location(), vacancy.field(), vacancy.title(), List.of());
			out.println(vacancy.id() + "\t" + reading.evidence());
		}
		out.flush();
	}

}
