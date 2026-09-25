package oneprofile.backend.storage.vacancy;

/**
 * One vacancy's title, and the id it is held under. What a pass that derives facts from titles
 * reads, so that walking the corpus does not carry every description with it.
 *
 * @param id the id the vacancy is held under
 * @param title its title as the company wrote it
 */
public record VacancyTitle(long id, String title) {
}
