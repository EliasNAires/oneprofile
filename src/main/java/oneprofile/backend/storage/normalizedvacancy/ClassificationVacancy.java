package oneprofile.backend.storage.normalizedvacancy;

/**
 * One vacancy title classification reads: its cleaned title, and the vacancy it was derived from.
 *
 * @param vacancyId the vacancy the title was derived from
 * @param cleanedTitle its title with what is noise in any title taken out
 */
public record ClassificationVacancy(long vacancyId, String cleanedTitle) {
}
