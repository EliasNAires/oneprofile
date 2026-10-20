package oneprofile.backend.storage.vacancy;

import java.time.Instant;
import java.util.List;

/**
 * One opening as a board publishes it today. Everything a sweep holds comes from here, so a field
 * the board left out is null rather than guessed at, and so is a field its ATS does not provide.
 *
 * @param externalId the id the ATS gives the opening, unique within the board but not across boards:
 * a number on Greenhouse, a UUID on Ashby
 * @param title the title as the company wrote it
 * @param location the location as free text, such as "Remote - Americas"
 * @param department the department the opening belongs to, or null if the board named none
 * @param description the body of the opening as plain text, one line per paragraph, heading or list
 * item, or null if it published none. A line may be marked with what its source said it was:
 * {@code # } for a heading, {@code - } for a list item, and {@code > } before either for text the
 * ATS adds to every vacancy of the board rather than the company writing it for this one
 * @param language the language the board says the opening is written in, as an ISO 639-1 code, or
 * null if it said none
 * @param url where the opening is published
 * @param payMinCents the bottom of the published pay range, or null if it published none
 * @param payMaxCents the top of the published pay range, or null if it published none
 * @param payCurrency the currency the range is in, or null if it published none
 * @param payTitle the heading above the range, which on Greenhouse is the only thing telling a rate
 * from a salary, or null if it published none
 * @param payInterval the interval the range is quoted in, such as {@code 1 YEAR} or {@code 1 HOUR}, as
 * the ATS writes it, or null if it gave none
 * @param firstPublishedAt when the opening was first published
 * @param updatedAt when the opening was last changed, or null if the ATS does not say
 * @param workplaceType the work mode as the ATS names it, such as {@code Remote}, or null if it gave
 * none
 * @param addressCountry the country of the opening's own address as the ATS writes it, or null if it
 * gave none
 * @param secondaryLocations the other places the opening names for the work, or null if its ATS does
 * not provide them
 */
public record PublishedVacancy(String externalId, String title, String location, String department,
		String description, String language, String url, Long payMinCents, Long payMaxCents, String payCurrency,
		String payTitle, String payInterval, Instant firstPublishedAt, Instant updatedAt, String workplaceType,
		String addressCountry, List<SecondaryLocation> secondaryLocations) {

	public PublishedVacancy {
		secondaryLocations = (secondaryLocations != null) ? List.copyOf(secondaryLocations) : null;
	}

}
