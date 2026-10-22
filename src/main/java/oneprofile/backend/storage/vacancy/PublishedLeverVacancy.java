package oneprofile.backend.storage.vacancy;

import java.time.Instant;
import java.util.List;

/**
 * One vacancy as a Lever board publishes it, with the fields Lever structures kept as it writes
 * them.
 *
 * @param externalId the id Lever gives the vacancy, a UUID
 * @param title the title as the company wrote it
 * @param location the location as free text, Lever's first category
 * @param department the department, or null if the board named none
 * @param description the body as marked lines, or null if it published none
 * @param url where the vacancy is published
 * @param payMinCents the bottom of the published pay range, or null if it published none
 * @param payMaxCents the top of the published pay range, or null if it published none
 * @param payCurrency the currency the range is in, or null if it published none
 * @param firstPublishedAt when the vacancy was created on Lever
 * @param workplaceType Lever's workplace type, such as {@code remote} or {@code unspecified}, or null
 * if it gave none
 * @param country the country Lever places the vacancy in, as a two-letter code, or null if it gave
 * none
 * @param commitment how the work is held, such as {@code Full-time}, as the board names it, or null
 * if it named none
 * @param team the team within the department, or null if the board named none
 * @param allLocations every location the vacancy names, the first one included, empty if it names
 * none
 * @param payInterval the interval the range is quoted in, such as {@code per-year-salary}, as Lever
 * writes it, or null if it gave none
 */
public record PublishedLeverVacancy(String externalId, String title, String location, String department,
		String description, String url, Long payMinCents, Long payMaxCents, String payCurrency,
		Instant firstPublishedAt, String workplaceType, String country, String commitment, String team,
		List<String> allLocations, String payInterval) implements PublishedVacancy {

	public PublishedLeverVacancy {
		allLocations = (allLocations != null) ? List.copyOf(allLocations) : null;
	}

}
