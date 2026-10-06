package oneprofile.backend.storage.vacancy;

/**
 * One vacancy's title and description, and the id it is held under. What cleaning reads, so that
 * walking the corpus does not carry the rest of each vacancy with it.
 *
 * @param id the id the vacancy is held under
 * @param title its title as the company wrote it
 * @param description its description as the sweep stored it, null if the board published none
 */
public record VacancyText(long id, String title, String description) {
}
