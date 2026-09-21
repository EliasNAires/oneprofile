package oneprofile.backend.normalizedvacancy;

/**
 * One vacancy's cleaned title, and the vacancy it was derived from. What a pass that reads titles
 * rather than bodies walks the corpus with.
 *
 * @param vacancyId the vacancy the title was derived from
 * @param cleanedTitle its title with what is noise in any title taken out
 */
public record NormalizedTitle(long vacancyId, String cleanedTitle) {
}
