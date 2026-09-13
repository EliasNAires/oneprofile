package oneprofile.backend.client;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.util.HtmlToText;
import oneprofile.backend.util.HttpRetry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reads the public job board API of Greenhouse. A board answers with every opening
 * at once —there is no pagination— or with 404 when the slug is not a board.
 */
@Component
public class GreenhouseBoardClient {

	private static final String BOARD_JOBS_URL = "https://boards-api.greenhouse.io/v1/boards/{slug}/jobs";

	/**
	 * The same endpoint asked for everything worth keeping: {@code content} is the only
	 * place with the real requirements, and {@code pay_transparency} is what turns the
	 * salary into numbers instead of a sentence buried in the description.
	 */
	private static final String BOARD_JOBS_WITH_CONTENT_URL = BOARD_JOBS_URL
			+ "?content=true&pay_transparency=true";

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	/**
	 * A probe is a few hundred KB, but asking for the content multiplies that by ten or
	 * more: the largest boards answer several MB in a single response.
	 */
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(120);

	private static final int MAX_ATTEMPTS = 3;

	/** Doubles on every retry, so the waits are 1s and 2s. */
	private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(1);

	private final RestClient restClient;

	private final HttpRetry retry;

	private final ObjectMapper json = new ObjectMapper();

	public GreenhouseBoardClient() {
		this(RestClient.builder().requestFactory(HttpRetry.requestFactory(CONNECT_TIMEOUT, READ_TIMEOUT)),
				FIRST_RETRY_DELAY);
	}

	GreenhouseBoardClient(RestClient.Builder builder, Duration firstRetryDelay) {
		this.restClient = builder.build();
		this.retry = new HttpRetry("Greenhouse", MAX_ATTEMPTS, firstRetryDelay);
	}

	/**
	 * Asks the board of {@code slug} what it holds. A 404 is not a failure here: it is
	 * the answer that says the company is not on Greenhouse, so it is never retried.
	 */
	public BoardProbe probe(String slug) {
		return this.retry.call("the board of " + slug, () -> this.restClient.get()
				.uri(BOARD_JOBS_URL, slug)
				.exchange((request, response) -> {
					HttpStatusCode status = response.getStatusCode();
					if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
						return new BoardProbe(BoardStatus.NOT_FOUND, 0, null);
					}
					if (status.isError()) {
						throw HttpRetry.errorFor(status, response.getStatusText());
					}
					return read(response.getBody());
				}));
	}

	/**
	 * Brings every opening of a board with its description already turned into plain
	 * text. There is no pagination: this is one response with the whole board.
	 */
	public List<BoardJob> jobs(String slug) {
		return this.retry.call("the openings of " + slug, () -> this.restClient.get()
				.uri(BOARD_JOBS_WITH_CONTENT_URL, slug)
				.exchange((request, response) -> {
					HttpStatusCode status = response.getStatusCode();
					if (status.isError()) {
						throw HttpRetry.errorFor(status, response.getStatusText());
					}
					return readJobs(response.getBody());
				}));
	}

	private List<BoardJob> readJobs(InputStream body) throws IOException {
		JsonNode jobs = this.json.readTree(body).path("jobs");
		List<BoardJob> parsed = new ArrayList<>(jobs.size());
		for (JsonNode job : jobs) {
			parsed.add(toJob(job));
		}
		return parsed;
	}

	private static BoardJob toJob(JsonNode job) {
		// Always exactly one range when the board publishes any.
		JsonNode pay = job.path("pay_input_ranges").path(0);
		return new BoardJob(job.path("id").asLong(), text(job.path("title")), text(job.path("location").path("name")),
				text(job.path("departments").path(0).path("name")),
				HtmlToText.plainText(text(job.path("content"))), text(job.path("absolute_url")),
				text(job.path("language")), number(pay.path("min_cents")), number(pay.path("max_cents")),
				text(pay.path("currency_type")), text(pay.path("title")), timestamp(job.path("first_published")),
				timestamp(job.path("updated_at")));
	}

	private static String text(JsonNode node) {
		return node.isTextual() ? node.asString() : null;
	}

	private static Long number(JsonNode node) {
		return node.isNumber() ? node.asLong() : null;
	}

	/** The board writes its timestamps with an offset, which the instant drops. */
	private static Instant timestamp(JsonNode node) {
		String value = text(node);
		return value == null ? null : OffsetDateTime.parse(value).toInstant();
	}

	private BoardProbe read(InputStream body) throws IOException {
		JsonNode root = this.json.readTree(body);
		int jobCount = root.path("meta").path("total").asInt();
		JsonNode companyName = root.path("jobs").path(0).path("company_name");
		BoardStatus status = jobCount > 0 ? BoardStatus.ACTIVE : BoardStatus.EMPTY;
		return new BoardProbe(status, jobCount, companyName.isMissingNode() ? null : companyName.asString());
	}

	/** The readable name is only there when the board had at least one opening. */
	public record BoardProbe(BoardStatus status, int jobCount, String companyName) {
	}

	/** One opening as the board tells it. The four pay fields are null together. */
	public record BoardJob(long externalId, String title, String location, String department, String description,
			String url, String language, Long payMinCents, Long payMaxCents, String payCurrency, String payTitle,
			Instant firstPublished, Instant updatedAt) {
	}
}
