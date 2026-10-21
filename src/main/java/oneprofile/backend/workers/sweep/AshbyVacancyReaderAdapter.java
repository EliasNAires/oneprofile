package oneprofile.backend.workers.sweep;

import java.io.IOException;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import oneprofile.backend.storage.vacancy.PublishedAshbyVacancy;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.SecondaryLocation;
import oneprofile.backend.workers.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads every opening of an Ashby board from its public posting API.
 * <p>
 * The whole board comes in one answer, descriptions and compensation included, and needs no key.
 * What Ashby structures and Greenhouse does not — its workplace type, the country of each location,
 * the interval a pay range is quoted in — is kept as Ashby writes it. A board that does
 * not answer with its openings is a failure rather than an answer, as it is on Greenhouse.
 */
@Component
public class AshbyVacancyReaderAdapter implements VacancyReaderPort {

	private static final URI POSTING_API = URI.create("https://api.ashbyhq.com/");

	private static final int ATTEMPTS = 5;

	private static final Duration BACK_OFF = Duration.ofSeconds(1);

	/** The largest boards answer over ten megabytes, every description written twice over. */
	private static final Duration READ_TIMEOUT = Duration.ofMinutes(3);

	/** The one compensation component that is pay; the others are equity, bonuses and the like. */
	private static final String SALARY = "Salary";

	private final HttpClient http = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(10))
		.build();

	private final ObjectMapper json = new ObjectMapper();

	private final URI base;

	private final String userAgent;

	private final int attempts;

	private final Duration backOff;

	private final Duration readTimeout;

	/** Reads from Ashby. */
	@Autowired
	public AshbyVacancyReaderAdapter() {
		this(POSTING_API, UserAgent.VALUE, ATTEMPTS, BACK_OFF, READ_TIMEOUT);
	}

	/**
	 * @param base what board paths are resolved against
	 * @param userAgent how this client identifies itself
	 * @param attempts how many times a request is made before it is given up on
	 * @param backOff how long to wait after the first refusal, doubling with each further one
	 * @param readTimeout how long a board is given to answer with all of its openings
	 */
	AshbyVacancyReaderAdapter(URI base, String userAgent, int attempts, Duration backOff, Duration readTimeout) {
		if (attempts < 1) {
			throw new IllegalArgumentException("A request has to be attempted at least once");
		}
		this.base = base;
		this.userAgent = userAgent;
		this.attempts = attempts;
		this.backOff = backOff;
		this.readTimeout = readTimeout;
	}

	@Override
	public List<PublishedVacancy> read(String slug) throws IOException {
		HttpRequest request = HttpRequest
			.newBuilder(this.base.resolve("posting-api/job-board/%s?includeCompensation=true".formatted(slug)))
			.header("User-Agent", this.userAgent)
			.timeout(this.readTimeout)
			.GET()
			.build();
		HttpResponse<String> response = send(request, slug);
		if (response.statusCode() != 200) {
			throw new IOException("Asked for the openings of %s and got %d".formatted(slug, response.statusCode()));
		}
		return published(response.body(), slug);
	}

	private List<PublishedVacancy> published(String body, String slug) throws IOException {
		JsonNode openings;
		try {
			openings = this.json.readTree(body).path("jobs");
		}
		catch (JacksonException ex) {
			throw new IOException("Unreadable answer for the openings of " + slug, ex);
		}
		if (!openings.isArray()) {
			throw new IOException("The answer for the openings of %s named none".formatted(slug));
		}
		List<PublishedVacancy> published = new ArrayList<>(openings.size());
		for (JsonNode opening : openings) {
			published.add(publishedVacancy(opening));
		}
		return published;
	}

	private static PublishedVacancy publishedVacancy(JsonNode opening) {
		JsonNode pay = salary(opening.path("compensation").path("summaryComponents"));
		return new PublishedAshbyVacancy(text(opening.path("id")), text(opening.path("title")),
				text(opening.path("location")), text(opening.path("department")),
				DescriptionMarkingRule.markedLines(text(opening.path("descriptionHtml"))), text(opening.path("jobUrl")),
				cents(pay.path("minValue")), cents(pay.path("maxValue")), text(pay.path("currencyCode")),
				timestamp(opening.path("publishedAt")), text(opening.path("workplaceType")),
				country(opening.path("address")), secondaryLocations(opening.path("secondaryLocations")),
				text(pay.path("interval")));
	}

	/**
	 * The salary among the components that summarize an opening's compensation, which span every
	 * tier it is paid on, or a missing node if it publishes no pay.
	 */
	private static JsonNode salary(JsonNode components) {
		for (JsonNode component : components) {
			if (SALARY.equals(text(component.path("compensationType")))) {
				return component;
			}
		}
		return components.path(components.size());
	}

	private static List<SecondaryLocation> secondaryLocations(JsonNode locations) {
		List<SecondaryLocation> secondary = new ArrayList<>(locations.size());
		for (JsonNode location : locations) {
			secondary.add(new SecondaryLocation(text(location.path("location")), country(location.path("address"))));
		}
		return secondary;
	}

	private static String country(JsonNode address) {
		return text(address.path("postalAddress").path("addressCountry"));
	}

	private static String text(JsonNode node) {
		return node.isString() ? node.stringValue() : null;
	}

	/** Ashby quotes pay in whole units of its currency, and an hourly rate may carry a fraction. */
	private static Long cents(JsonNode node) {
		return node.isNumber() ? node.decimalValue().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue()
				: null;
	}

	/** A board writes its timestamps with an offset, which the instant drops. */
	private static Instant timestamp(JsonNode node) {
		String written = text(node);
		return (written != null) ? OffsetDateTime.parse(written).toInstant() : null;
	}

	private HttpResponse<String> send(HttpRequest request, String slug) throws IOException {
		IOException lastFailure = null;
		for (int attempt = 0; attempt < this.attempts; attempt++) {
			if (attempt > 0) {
				waitBefore(attempt);
			}
			try {
				HttpResponse<String> response = this.http.send(request, BodyHandlers.ofString());
				if (response.statusCode() != 429 && response.statusCode() != 503) {
					return response;
				}
				lastFailure = new IOException(
						"Asked for the openings of %s and got %d".formatted(slug, response.statusCode()));
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while reading the openings of " + slug, ex);
			}
		}
		throw lastFailure;
	}

	private void waitBefore(int attempt) throws IOException {
		try {
			Thread.sleep(this.backOff.multipliedBy(1L << (attempt - 1)));
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while backing off", ex);
		}
	}

}
