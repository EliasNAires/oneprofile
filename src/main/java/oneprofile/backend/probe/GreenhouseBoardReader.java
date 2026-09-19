package oneprofile.backend.probe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import oneprofile.backend.company.BoardStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads Greenhouse boards from its public job board API.
 * <p>
 * A slug with no board answers 404, which is an answer about the board rather than a failure, so it
 * is never retried. Greenhouse names the company on each opening it publishes, so a board with no
 * openings gives no name at all.
 */
public class GreenhouseBoardReader implements BoardReader {

	private static final URI BOARDS_API = URI.create("https://boards-api.greenhouse.io/");

	private static final int ATTEMPTS = 5;

	private static final Duration BACK_OFF = Duration.ofSeconds(1);

	private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

	private final ObjectMapper json = new ObjectMapper();

	private final URI base;

	private final String userAgent;

	private final int attempts;

	private final Duration backOff;

	/**
	 * Reads from Greenhouse.
	 * @param userAgent how this client identifies itself
	 */
	public GreenhouseBoardReader(String userAgent) {
		this(BOARDS_API, userAgent, ATTEMPTS, BACK_OFF);
	}

	/**
	 * @param base what board paths are resolved against
	 * @param userAgent how this client identifies itself
	 * @param attempts how many times a request is made before it is given up on
	 * @param backOff how long to wait after the first refusal, doubling with each further one
	 */
	public GreenhouseBoardReader(URI base, String userAgent, int attempts, Duration backOff) {
		if (attempts < 1) {
			throw new IllegalArgumentException("A request has to be attempted at least once");
		}
		this.base = base;
		this.userAgent = userAgent;
		this.attempts = attempts;
		this.backOff = backOff;
	}

	@Override
	public BoardReading read(String slug) throws IOException {
		HttpRequest request = HttpRequest.newBuilder(this.base.resolve("v1/boards/%s/jobs".formatted(slug)))
			.header("User-Agent", this.userAgent)
			.GET()
			.build();
		HttpResponse<String> response = send(request, slug);
		if (response.statusCode() == 404) {
			return new BoardReading(BoardStatus.NOT_FOUND, null);
		}
		if (response.statusCode() != 200) {
			throw new IOException("Asked for the board of %s and got %d".formatted(slug, response.statusCode()));
		}
		return reading(response.body(), slug);
	}

	private BoardReading reading(String body, String slug) throws IOException {
		JsonNode openings;
		try {
			openings = this.json.readTree(body).path("jobs");
		}
		catch (JacksonException ex) {
			throw new IOException("Unreadable answer for the board of " + slug, ex);
		}
		if (!openings.isArray()) {
			throw new IOException("The answer for the board of %s named no openings".formatted(slug));
		}
		if (openings.isEmpty()) {
			return new BoardReading(BoardStatus.EMPTY, null);
		}
		JsonNode companyName = openings.get(0).path("company_name");
		String name = companyName.isString() ? companyName.stringValue() : null;
		return new BoardReading(BoardStatus.ACTIVE, (name != null && !name.isBlank()) ? name : null);
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
						"Asked for the board of %s and got %d".formatted(slug, response.statusCode()));
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while reading the board of " + slug, ex);
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
