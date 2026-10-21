package oneprofile.backend.storage.vacancy;

import java.time.Instant;
import java.util.List;

/**
 * One vacancy as an Ashby board publishes it, with the fields Ashby structures kept as it writes
 * them.
 *
 * @param externalId the id Ashby gives the vacancy, a UUID
 * @param title the title as the company wrote it
 * @param location the location as free text
 * @param department the department, or null if the board named none
 * @param description the body as marked lines, or null if it published none
 * @param url where the vacancy is published
 * @param payMinCents the bottom of the published pay range, or null if it published none
 * @param payMaxCents the top of the published pay range, or null if it published none
 * @param payCurrency the currency the range is in, or null if it published none
 * @param firstPublishedAt when the vacancy was first published
 * @param workplaceType Ashby's workplace type, such as {@code Remote}, or null if it gave none
 * @param addressCountry the country of the vacancy's own address as Ashby writes it, or null if it
 * gave none
 * @param secondaryLocations the other places the vacancy names for the work, empty if it names none
 * @param payInterval the interval the range is quoted in, such as {@code 1 YEAR} or {@code 1 HOUR},
 * as Ashby writes it, or null if it gave none
 */
public record PublishedAshbyVacancy(String externalId, String title, String location, String department,
		String description, String url, Long payMinCents, Long payMaxCents, String payCurrency,
		Instant firstPublishedAt, String workplaceType, String addressCountry,
		List<SecondaryLocation> secondaryLocations, String payInterval) implements PublishedVacancy {

	public PublishedAshbyVacancy {
		secondaryLocations = (secondaryLocations != null) ? List.copyOf(secondaryLocations) : null;
	}

}
