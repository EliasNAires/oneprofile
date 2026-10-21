package oneprofile.backend.storage.vacancy;

import java.time.Instant;

/**
 * One vacancy as a Greenhouse board publishes it.
 *
 * @param externalId the id Greenhouse gives the vacancy, a number
 * @param title the title as the company wrote it
 * @param location the location as free text
 * @param department the department, or null if the board named none
 * @param description the body as marked lines, or null if it published none
 * @param url where the vacancy is published
 * @param payMinCents the bottom of the published pay range, or null if it published none
 * @param payMaxCents the top of the published pay range, or null if it published none
 * @param payCurrency the currency the range is in, or null if it published none
 * @param firstPublishedAt when the vacancy was first published
 * @param payTitle the heading above the range, which on Greenhouse is the only thing telling a rate
 * from a salary, or null if it published none
 * @param updatedAt when the vacancy was last changed, or null if the board did not say
 * @param language the language the board says the vacancy is written in, as an ISO 639-1 code, or
 * null if it said none
 */
public record PublishedGreenhouseVacancy(String externalId, String title, String location, String department,
		String description, String url, Long payMinCents, Long payMaxCents, String payCurrency,
		Instant firstPublishedAt, String payTitle, Instant updatedAt, String language) implements PublishedVacancy {
}
