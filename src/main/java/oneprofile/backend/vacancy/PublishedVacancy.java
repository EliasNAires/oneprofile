package oneprofile.backend.vacancy;

import java.time.Instant;

/**
 * One opening as a board publishes it today. Everything a sweep holds comes from here, so a field
 * the board left out is null rather than guessed at.
 *
 * @param externalId the id the ATS gives the opening, unique within the board but not across boards
 * @param title the title as the company wrote it
 * @param location the location as free text, such as "Remote - Americas"
 * @param department the department the opening belongs to, or null if the board named none
 * @param description the body of the opening as plain text, or null if it published none
 * @param url where the opening is published
 * @param payMinCents the bottom of the published pay range, or null if it published none
 * @param payMaxCents the top of the published pay range, or null if it published none
 * @param payCurrency the currency the range is in, or null if it published none
 * @param payTitle the heading above the range, the only thing telling a rate from a salary
 * @param firstPublishedAt when the opening was first published
 * @param updatedAt when the opening was last changed
 */
public record PublishedVacancy(long externalId, String title, String location, String department, String description,
		String url, Long payMinCents, Long payMaxCents, String payCurrency, String payTitle, Instant firstPublishedAt,
		Instant updatedAt) {
}
