package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;

/**
 * One vacancy of the pile, the vacancies the body pass reads: what its title left it, and its
 * description, both as the sweep stored it and as cleaning cut it.
 *
 * @param vacancyId the vacancy it was derived from
 * @param cleanedTitle its title with what is noise in any title taken out
 * @param titleReason why its title was left unknown, which stays on it once the body decides
 * @param description its description as the sweep stored it, null if the board gave none
 * @param descriptionSegments its cleaned description cut into segments, null until cleaning has cut it
 */
public record PileVacancy(long vacancyId, String cleanedTitle, UnknownReasonEnum titleReason, String description,
		List<Segment> descriptionSegments) {
}
