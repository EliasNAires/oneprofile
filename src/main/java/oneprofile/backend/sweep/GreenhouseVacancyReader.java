package oneprofile.backend.sweep;

import java.io.IOException;
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
import oneprofile.backend.vacancy.PublishedVacancy;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads every opening of a Greenhouse board from its public job board API.
 * <p>
 * The whole board comes in one answer, and asking for the content multiplies that answer ten times
 * over, so the wait for it is generous. Unlike a probe, a board that does not answer with its
 * openings is a failure rather than an answer: the sweep of that company is given up on, and what
 * is already held for it is left alone.
 */
public class GreenhouseVacancyReader implements VacancyReader {

	private static final URI BOARDS_API = URI.create("https://boards-api.greenhouse.io/");

	private static final int ATTEMPTS = 5;

	private static final Duration BACK_OFF = Duration.ofSeconds(1);

	/** The largest boards answer several megabytes once the content is asked for. */
	private static final Duration READ_TIMEOUT = Duration.ofMinutes(3);

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

	/**
	 * Reads from Greenhouse.
	 * @param userAgent how this client identifies itself
	 */
	public GreenhouseVacancyReader(String userAgent) {
		this(BOARDS_API, userAgent, ATTEMPTS, BACK_OFF, READ_TIMEOUT);
	}

	/**
	 * @param base what board paths are resolved against
	 * @param userAgent how this client identifies itself
	 * @param attempts how many times a request is made before it is given up on
	 * @param backOff how long to wait after the first refusal, doubling with each further one
	 * @param readTimeout how long a board is given to answer with all of its openings
	 */
	public GreenhouseVacancyReader(URI base, String userAgent, int attempts, Duration backOff, Duration readTimeout) {
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
			.newBuilder(this.base.resolve("v1/boards/%s/jobs?content=true&pay_transparency=true".formatted(slug)))
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
		// A board publishes at most one range, and its four fields come and go together.
		JsonNode pay = opening.path("pay_input_ranges").path(0);
		return new PublishedVacancy(opening.path("id").asLong(), text(opening.path("title")),
				text(opening.path("location").path("name")), text(opening.path("departments").path(0).path("name")),
				plainText(text(opening.path("content"))), text(opening.path("absolute_url")),
				number(pay.path("min_cents")), number(pay.path("max_cents")), text(pay.path("currency_type")),
				text(pay.path("title")), timestamp(opening.path("first_published")),
				timestamp(opening.path("updated_at")));
	}

	/**
	 * The description as readable text. Greenhouse escapes it twice over — its literal value starts
	 * with {@code &lt;div} — so recovering the text takes two unescapes: one to get back the HTML,
	 * and one for the entities of the text itself, since a {@code &nbsp;} of the original arrives as
	 * {@code &amp;nbsp;}. The second is Jsoup's own, which decodes as it parses.
	 */
	private static String plainText(String escapedHtml) {
		if (escapedHtml == null || escapedHtml.isBlank()) {
			return null;
		}
		String text = Jsoup.parse(Parser.unescapeEntities(escapedHtml, false)).text();
		return text.isBlank() ? null : text;
	}

	private static String text(JsonNode node) {
		return node.isString() ? node.stringValue() : null;
	}

	private static Long number(JsonNode node) {
		return node.isNumber() ? node.asLong() : null;
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
