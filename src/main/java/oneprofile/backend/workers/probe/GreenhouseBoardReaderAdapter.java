package oneprofile.backend.workers.probe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Set;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.workers.RetryAfter;
import oneprofile.backend.workers.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
@Component
public class GreenhouseBoardReaderAdapter implements BoardReaderPort {

	private static final URI BOARDS_API = URI.create("https://boards-api.greenhouse.io/");

	/** What an ATS answers when it asks to be given time, which slows the run down. */
	private static final Set<Integer> REFUSED = Set.of(429, 503);

	private static final int ATTEMPTS = 5;

	private static final Duration BACK_OFF = Duration.ofSeconds(1);

	private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

	private final ObjectMapper json = new ObjectMapper();

	private final URI base;

	private final String userAgent;

	private final int attempts;

	private final Duration backOff;

	/** Reads from Greenhouse. */
	@Autowired
	public GreenhouseBoardReaderAdapter() {
		this(BOARDS_API, UserAgent.VALUE, ATTEMPTS, BACK_OFF);
	}

	/**
	 * @param base what board paths are resolved against
	 * @param userAgent how this client identifies itself
	 * @param attempts how many times a request is made before it is given up on
	 * @param backOff how long to wait after the first refusal, doubling with each further one
	 */
	GreenhouseBoardReaderAdapter(URI base, String userAgent, int attempts, Duration backOff) {
		if (attempts < 1) {
			throw new IllegalArgumentException("A request has to be attempted at least once");
		}
		this.base = base;
		this.userAgent = userAgent;
		this.attempts = attempts;
		this.backOff = backOff;
	}

	@Override
	public BoardReading read(String slug, RegionEnum region, Runnable onRefusal) throws IOException {
		HttpRequest request = HttpRequest.newBuilder(this.base.resolve("v1/boards/%s/jobs".formatted(slug)))
			.header("User-Agent", this.userAgent)
			.GET()
			.build();
		HttpResponse<String> response = send(request, slug, onRefusal);
		if (response.statusCode() == 404) {
			return new BoardReading(BoardStatusEnum.NOT_FOUND, null);
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
			return new BoardReading(BoardStatusEnum.EMPTY, null);
		}
		JsonNode companyName = openings.get(0).path("company_name");
		String name = companyName.isString() ? companyName.stringValue() : null;
		return new BoardReading(BoardStatusEnum.ACTIVE, (name != null && !name.isBlank()) ? name : null);
	}

	/**
	 * Asks until the ATS answers, waiting after a refusal as long as it says, or for the back-off if
	 * it does not.
	 */
	private HttpResponse<String> send(HttpRequest request, String slug, Runnable onRefusal) throws IOException {
		for (int attempt = 1;; attempt++) {
			try {
				HttpResponse<String> response = this.http.send(request, BodyHandlers.ofString());
				if (!REFUSED.contains(response.statusCode())) {
					return response;
				}
				onRefusal.run();
				if (attempt == this.attempts) {
					throw new IOException("Asked for the board of %s and got %d".formatted(slug, response.statusCode()));
				}
				Thread.sleep(RetryAfter.or(response, this.backOff.multipliedBy(1L << (attempt - 1))));
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while reading the board of " + slug, ex);
			}
		}
	}

}
