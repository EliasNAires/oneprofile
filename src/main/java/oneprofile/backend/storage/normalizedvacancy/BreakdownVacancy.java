package oneprofile.backend.storage.normalizedvacancy;

/**
 * One vacancy the title breakdown reads: one of the engineering subset, with its title as the company
 * wrote it, whose commas and dashes say where the role ends and its team or place begins, and its
 * location field, whose places the title may repeat.
 *
 * @param normalizedVacancyId the facts derived from it, which the skills found in its title belong to
 * @param vacancyId the vacancy it was derived from
 * @param title its title as published
 * @param location its location field, null if it has none
 */
public record BreakdownVacancy(long normalizedVacancyId, long vacancyId, String title, String location) {
}
