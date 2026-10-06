package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;
import java.util.Set;

/**
 * One vacancy of the pile, the vacancies the body pass reads: what its title left it, the language
 * it is written in, and its description as cleaning cut it.
 *
 * @param vacancyId the vacancy it was derived from
 * @param cleanedTitle its title with what is noise in any title taken out
 * @param titleReason why its title was left unknown, which stays on it once the body decides, or
 * {@code UNSUPPORTED_LANGUAGE} once the body pass has skipped it
 * @param language the language its board says it is written in, null if the board said none
 * @param descriptionSegments its cleaned description cut into segments, null until cleaning has cut it
 */
public record PileVacancy(long vacancyId, String cleanedTitle, UnknownReasonEnum titleReason, String language,
		List<Segment> descriptionSegments) {

	/**
	 * The languages the body pass reads: those the product can be marketed, supported and take
	 * feedback in.
	 */
	private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "es");

	/**
	 * Whether the body pass reads it. One whose board names no language is read.
	 * @return whether it is in English or Spanish, or in no language its board named
	 */
	public boolean inSupportedLanguage() {
		return this.language == null || SUPPORTED_LANGUAGES.contains(this.language);
	}

	/**
	 * Its segments, none if cleaning has not cut it.
	 * @return the segments of its description
	 */
	public List<Segment> segments() {
		return (this.descriptionSegments != null) ? this.descriptionSegments : List.of();
	}

}
