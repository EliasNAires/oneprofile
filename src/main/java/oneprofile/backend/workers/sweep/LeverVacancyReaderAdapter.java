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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.vacancy.PublishedLeverVacancy;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.workers.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads every opening of a Lever board from its public postings API, asking the API of the company's
 * region: each answers 404 for the other region's boards (ADR-0022).
 * <p>
 * The whole board comes in one answer, descriptions and pay included, and needs no key. Lever splits
 * a description into parts its page shows one after another — the description, each list under its
 * heading, the pay description and the closing — and the description is held as that page reads.
 * What Lever structures and no other ATS does — its workplace type, country, commitment, team and
 * every location — is kept as Lever writes it. A board that does not answer with its postings is a
 * failure rather than an answer, as it is on the other ATSs.
 */
@Component
public class LeverVacancyReaderAdapter implements VacancyReaderPort {

	private static final Map<RegionEnum, URI> POSTINGS_APIS = Map.of(RegionEnum.US,
			URI.create("https://api.lever.co/"), RegionEnum.EU, URI.create("https://api.eu.lever.co/"));

	/** What Lever answers when it asks to be given time, or fails to reach a board's server. */
	private static final Set<Integer> RETRIED = Set.of(429, 502, 503, 504);

	private static final int ATTEMPTS = 5;

	private static final Duration BACK_OFF = Duration.ofSeconds(1);

	/** The largest boards answer over thirty megabytes, every description written twice over. */
	private static final Duration READ_TIMEOUT = Duration.ofMinutes(3);

	private final HttpClient http = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(10))
		.build();

	private final ObjectMapper json = new ObjectMapper();

	private final Map<RegionEnum, URI> bases;

	private final String userAgent;

	private final int attempts;

	private final Duration backOff;

	private final Duration readTimeout;

	/** Reads from Lever. */
	@Autowired
	public LeverVacancyReaderAdapter() {
		this(POSTINGS_APIS, UserAgent.VALUE, ATTEMPTS, BACK_OFF, READ_TIMEOUT);
	}

	/**
	 * @param bases what board paths are resolved against, for each region
	 * @param userAgent how this client identifies itself
	 * @param attempts how many times a request is made before it is given up on
	 * @param backOff how long to wait after the first refusal, doubling with each further one
	 * @param readTimeout how long a board is given to answer with all of its openings
	 */
	LeverVacancyReaderAdapter(Map<RegionEnum, URI> bases, String userAgent, int attempts, Duration backOff,
			Duration readTimeout) {
		if (attempts < 1) {
			throw new IllegalArgumentException("A request has to be attempted at least once");
		}
		this.bases = Map.copyOf(bases);
		this.userAgent = userAgent;
		this.attempts = attempts;
		this.backOff = backOff;
		this.readTimeout = readTimeout;
	}

	@Override
	public List<PublishedVacancy> read(String slug, RegionEnum region) throws IOException {
		HttpRequest request = HttpRequest
			.newBuilder(this.bases.get(region).resolve("v0/postings/%s?mode=json".formatted(slug)))
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
		JsonNode postings;
		try {
			postings = this.json.readTree(body);
		}
		catch (JacksonException ex) {
			throw new IOException("Unreadable answer for the openings of " + slug, ex);
		}
		if (!postings.isArray()) {
			throw new IOException("The answer for the openings of %s named none".formatted(slug));
		}
		List<PublishedVacancy> published = new ArrayList<>(postings.size());
		for (JsonNode posting : postings) {
			published.add(publishedVacancy(posting));
		}
		return published;
	}

	private static PublishedVacancy publishedVacancy(JsonNode posting) {
		JsonNode categories = posting.path("categories");
		JsonNode pay = posting.path("salaryRange");
		return new PublishedLeverVacancy(text(posting.path("id")), text(posting.path("text")),
				text(categories.path("location")), text(categories.path("department")),
				DescriptionMarkingRule.markedLines(descriptionHtml(posting)), text(posting.path("hostedUrl")),
				cents(pay.path("min")), cents(pay.path("max")), text(pay.path("currency")),
				timestamp(posting.path("createdAt")), text(posting.path("workplaceType")),
				text(posting.path("country")), text(categories.path("commitment")), text(categories.path("team")),
				texts(categories.path("allLocations")), text(pay.path("interval")));
	}

	/**
	 * The parts of a description as Lever's page shows them, one after another: the description, each
	 * list under its heading, the pay description, and the closing.
	 */
	private static String descriptionHtml(JsonNode posting) {
		StringBuilder html = new StringBuilder();
		appendIfAny(html, text(posting.path("description")));
		for (JsonNode list : posting.path("lists")) {
			html.append("<h3>").append(escaped(text(list.path("text")))).append("</h3>");
			html.append("<ul>").append(orEmpty(text(list.path("content")))).append("</ul>");
		}
		appendIfAny(html, text(posting.path("salaryDescription")));
		appendIfAny(html, text(posting.path("additional")));
		return html.toString();
	}

	private static void appendIfAny(StringBuilder html, String part) {
		if (part != null) {
			html.append("<div>").append(part).append("</div>");
		}
	}

	/** A list's heading is plain text, so whatever it holds that looks like markup is text. */
	private static String escaped(String text) {
		return orEmpty(text).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String orEmpty(String text) {
		return (text != null) ? text : "";
	}

	private static List<String> texts(JsonNode node) {
		List<String> texts = new ArrayList<>(node.size());
		for (JsonNode element : node) {
			if (element.isString()) {
				texts.add(element.stringValue());
			}
		}
		return texts;
	}

	private static String text(JsonNode node) {
		return node.isString() ? node.stringValue() : null;
	}

	/** Lever quotes pay in whole units of its currency, and an hourly rate may carry a fraction. */
	private static Long cents(JsonNode node) {
		return node.isNumber() ? node.decimalValue().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue()
				: null;
	}

	/** Lever writes when a posting was created as milliseconds since the epoch. */
	private static Instant timestamp(JsonNode node) {
		return node.isNumber() ? Instant.ofEpochMilli(node.longValue()) : null;
	}

	private HttpResponse<String> send(HttpRequest request, String slug) throws IOException {
		IOException lastFailure = null;
		for (int attempt = 0; attempt < this.attempts; attempt++) {
			if (attempt > 0) {
				waitBefore(attempt);
			}
			try {
				HttpResponse<String> response = this.http.send(request, BodyHandlers.ofString());
				if (!RETRIED.contains(response.statusCode())) {
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
