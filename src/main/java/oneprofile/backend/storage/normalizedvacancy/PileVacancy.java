package oneprofile.backend.storage.normalizedvacancy;

/**
 * One vacancy of the pile, the vacancies the body pass reads: what its title left it, and its
 * description as the sweep stored it.
 *
 * @param vacancyId the vacancy it was derived from
 * @param cleanedTitle its title with what is noise in any title taken out
 * @param titleReason why its title was left unknown, which stays on it once the body decides
 * @param description its description as the sweep stored it, null if the board gave none
 */
public record PileVacancy(long vacancyId, String cleanedTitle, UnknownReasonEnum titleReason, String description) {
}
