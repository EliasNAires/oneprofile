import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import oneprofile.backend.storage.normalizedvacancy.CountrySet;
import oneprofile.backend.storage.normalizedvacancy.StatedSet;
import oneprofile.backend.storage.normalizedvacancy.StatedSetKindEnum;
import oneprofile.backend.storage.normalizedvacancy.StatedSetSourceEnum;
import oneprofile.backend.storage.placelist.PlaceListConfiguration;
import oneprofile.backend.workers.eligibility.Eligibility;
import oneprofile.backend.workers.eligibility.EligibilityRule;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Derives the eligibility state of vacancies read from stdin with the application's own rule, for each country
 * given, with the person's city unknown, and names the stated sets that decided it. Run by
 * draw-eligibility-sample, never on its own.
 *
 * Reads one vacancy a line, as {"id": vacancy id, "statedSets": [its stated sets, as StatedSet holds them]}, and
 * writes one line a vacancy and country, as id<TAB>country<TAB>state<TAB>source<TAB>positions: the source is
 * where the deciding set was read, NONE where none decided; the positions, comma-separated, are those of the
 * deciding source's sets that give the same state when judged alone (for the places the description hires in,
 * those sets together), empty where none decided.
 */
class EligibilityStates {

	record Vacancy(long id, List<StatedSet> statedSets) {
	}

	public static void main(String[] args) throws Exception {
		EligibilityRule rule = new EligibilityRule(PlaceListConfiguration.shipped());
		JsonMapper json = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
		PrintStream out = new PrintStream(System.out, false, StandardCharsets.UTF_8);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
		String line;
		while ((line = in.readLine()) != null) {
			Vacancy vacancy = json.readValue(line, Vacancy.class);
			for (String country : args) {
				Eligibility eligibility = rule.eligibility(new CountrySet(vacancy.statedSets(), false, false), country,
						null);
				List<Integer> positions = new ArrayList<>();
				if (eligibility.source() != null) {
					List<StatedSet> hiresIn = vacancy.statedSets().stream().filter(EligibilityStates::hiresIn).toList();
					for (StatedSet set : vacancy.statedSets()) {
						if (set.source() != eligibility.source()) {
							continue;
						}
						List<StatedSet> alone = hiresIn(set) ? hiresIn : List.of(set);
						if (rule.eligibility(new CountrySet(alone, false, false), country, null).state() == eligibility
								.state()) {
							positions.add(set.position());
						}
					}
				}
				out.println(vacancy.id() + "\t" + country + "\t" + eligibility.state() + "\t"
						+ ((eligibility.source() != null) ? eligibility.source() : "NONE") + "\t"
						+ positions.stream().map(String::valueOf).collect(Collectors.joining(",")));
			}
		}
		out.flush();
	}

	/** Whether a set is one of the places the description says the vacancy hires in, which count as one set. */
	private static boolean hiresIn(StatedSet set) {
		return set.source() == StatedSetSourceEnum.DESCRIPTION
				&& (set.kind() == StatedSetKindEnum.LOCATION || set.kind() == StatedSetKindEnum.REGION);
	}

}
