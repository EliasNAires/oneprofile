package oneprofile.backend.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import oneprofile.backend.model.WorkMode;

/**
 * Reads where the job is done, out of the title and out of the free text location the
 * board publishes, and takes the words that say it out of the title.
 *
 * <p>It needs both sources because of where the signal lives: of the 16.444 vacancies
 * that declare remote work, <b>14.624 say it only in the location</b> and 1.066 only in
 * the title. Reading the title alone would miss 89% of it, so the location is the one
 * that decides and the title only answers when the location says nothing.
 *
 * <p>Like {@link TitleCleaner} and {@link SeniorityExtractor}, a pure function: no
 * network, no database and no Spring.
 */
public final class WorkModeExtractor {

	/** The title with the work mode words taken out, and the mode; both null when absent. */
	public record Extracted(String title, WorkMode mode) {
	}

	private static final Pattern SPACES = Pattern.compile("\\s+");

	/**
	 * Every way the dataset spells a work mode, and nothing else. The four spellings that
	 * seemed obvious and measured zero —"telecommute", "telework", "teletrabajo" and
	 * "a distancia"— are left out on purpose: writing them would be dead code.
	 */
	private static final Map<String, WorkMode> PHRASES = Map.ofEntries(
			Map.entry("work from home", WorkMode.REMOTE),
			Map.entry("home based", WorkMode.REMOTE),
			Map.entry("fully remote", WorkMode.REMOTE),
			Map.entry("100 remote", WorkMode.REMOTE),
			Map.entry("remote only", WorkMode.REMOTE),
			Map.entry("remote", WorkMode.REMOTE),
			Map.entry("remoto", WorkMode.REMOTE),
			Map.entry("wfh", WorkMode.REMOTE),
			Map.entry("hybrid", WorkMode.HYBRID),
			Map.entry("hibrido", WorkMode.HYBRID),
			Map.entry("on site", WorkMode.ONSITE),
			Map.entry("in office", WorkMode.ONSITE),
			Map.entry("in person", WorkMode.ONSITE),
			Map.entry("field based", WorkMode.ONSITE),
			Map.entry("onsite", WorkMode.ONSITE),
			Map.entry("presencial", WorkMode.ONSITE));

	/** The longest phrase above, in words: "work from home". */
	private static final int LONGEST_PHRASE = 3;

	private WorkModeExtractor() {
	}

	/**
	 * @param cleanTitle the title as {@link TitleCleaner} left it
	 * @param location the location exactly as the board publishes it, free text
	 */
	public static Extracted extract(String cleanTitle, String location) {
		List<String> titleTokens = tokens(cleanTitle);
		EnumSet<WorkMode> inTitle = takeOut(titleTokens);

		List<String> placeTokens = tokens(TitleCleaner.clean(location));
		EnumSet<WorkMode> inLocation = takeOut(placeTokens);

		// The location decides, because that is where the signal lives; the title only
		// answers when the location names no mode at all.
		WorkMode mode = mostSpecific(inLocation.isEmpty() ? inTitle : inLocation);
		if (mode == WorkMode.REMOTE && placeTokens.isEmpty()) {
			// Remote and naming no place: anyone can apply.
			mode = WorkMode.FULLY_REMOTE;
		}

		String title = titleTokens.isEmpty() ? null : String.join(" ", titleTokens);
		return new Extracted(title, mode);
	}

	private static List<String> tokens(String text) {
		if (text == null || text.isEmpty()) {
			return new ArrayList<>();
		}
		return new ArrayList<>(Arrays.asList(SPACES.split(text)));
	}

	/**
	 * Removes every phrase that names a work mode from {@code tokens}, and answers which
	 * modes were named. Longer phrases are tried first so that "fully remote" goes out
	 * whole instead of leaving a dangling "fully" behind.
	 */
	private static EnumSet<WorkMode> takeOut(List<String> tokens) {
		EnumSet<WorkMode> found = EnumSet.noneOf(WorkMode.class);
		for (int i = 0; i < tokens.size(); i++) {
			for (int words = LONGEST_PHRASE; words >= 1; words--) {
				if (i + words > tokens.size()) {
					continue;
				}
				WorkMode mode = PHRASES.get(String.join(" ", tokens.subList(i, i + words)));
				if (mode != null) {
					found.add(mode);
					tokens.subList(i, i + words).clear();
					i--;
					break;
				}
			}
		}
		return found;
	}

	/** From the most specific to the least, when one source names more than one. */
	private static WorkMode mostSpecific(EnumSet<WorkMode> modes) {
		if (modes.contains(WorkMode.HYBRID)) {
			return WorkMode.HYBRID;
		}
		if (modes.contains(WorkMode.REMOTE)) {
			return WorkMode.REMOTE;
		}
		return modes.contains(WorkMode.ONSITE) ? WorkMode.ONSITE : null;
	}
}
