package oneprofile.backend.storage.vacancy;

import java.time.Instant;
import java.util.List;

/**
 * A vacancy as a Workday board publishes it. A board's list names each vacancy's title, path and
 * location; the rest is read from the vacancy's own page, which a sweep asks for only where the title
 * may make it an engineering role (docs/measurements/2026-10-workday.md § The setting). Where it was
 * not asked for, what only that page says is null. Workday publishes no department and no pay.
 *
 * @param externalId the last segment of the vacancy's path, its title and requisition id, such as
 * {@code RTL-Design-Engineer--DFT_JR2028303}, unique within the board
 * @param title the title as the company wrote it
 * @param location the vacancy's main location, or the list's location text where its page was not
 * read, such as {@code 2 Locations}
 * @param description the body as marked lines, or null if it was not read or published none
 * @param url where the vacancy is published
 * @param firstPublishedAt the vacancy's start date, the day it was posted, or null if its page was not
 * read
 * @param timeType how the work is held, such as {@code Full time}, as Workday writes it, or null if it
 * gave none
 * @param country the country of the vacancy's requisition, as a two-letter code, or null if it gave none
 * @param allLocations every location the vacancy names, the main one first, or null if its page was not
 * read
 * @param companyName the organization its page names as hiring, such as {@code 2100 NVIDIA USA}, or
 * null if its page was not read or names none
 */
public record PublishedWorkdayVacancy(String externalId, String title, String location, String description,
		String url, Instant firstPublishedAt, String timeType, String country, List<String> allLocations,
		String companyName) implements PublishedVacancy {

	public PublishedWorkdayVacancy {
		allLocations = (allLocations != null) ? List.copyOf(allLocations) : null;
	}

	@Override
	public String department() {
		return null;
	}

	@Override
	public Long payMinCents() {
		return null;
	}

	@Override
	public Long payMaxCents() {
		return null;
	}

	@Override
	public String payCurrency() {
		return null;
	}

}
