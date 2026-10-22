package oneprofile.backend.storage.vacancy;

import java.time.Instant;

/**
 * One vacancy as a board publishes it today, in the shape its ATS publishes it. Everything a sweep
 * holds comes from here. What any ATS would be expected to publish is read here; what one ATS
 * publishes its own way is read from its own record. A field the board left out is null rather than
 * guessed at.
 */
public sealed interface PublishedVacancy permits PublishedGreenhouseVacancy, PublishedAshbyVacancy,
		PublishedLeverVacancy {

	/**
	 * The id the ATS gives the vacancy, unique within the board but not across boards: a number on
	 * Greenhouse, a UUID on Ashby and Lever.
	 */
	String externalId();

	/** The title as the company wrote it. */
	String title();

	/** The location as free text, such as "Remote - Americas". */
	String location();

	/** The department the vacancy belongs to, or null if the board named none. */
	String department();

	/**
	 * The body of the vacancy as plain text, one line per paragraph, heading or list item, or null if
	 * it published none. A line may be marked with what its source said it was: {@code # } for a
	 * heading, {@code - } for a list item, and {@code > } before either for text the ATS adds to every
	 * vacancy of the board rather than the company writing it for this one.
	 */
	String description();

	/** Where the vacancy is published. */
	String url();

	/** The bottom of the published pay range, or null if it published none. */
	Long payMinCents();

	/** The top of the published pay range, or null if it published none. */
	Long payMaxCents();

	/** The currency the range is in, or null if it published none. */
	String payCurrency();

	/** When the vacancy was first published. */
	Instant firstPublishedAt();

}
