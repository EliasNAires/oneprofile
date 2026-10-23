package oneprofile.backend.workers.probe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
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
 * Reads Lever boards from its public postings API, asking the API of the company's region: each
 * answers 404 for the other region's boards (ADR-0022).
 * <p>
 * A probe asks for one posting, which says whether a board has any as well as the whole board does,
 * and Lever's largest boards answer tens of megabytes. A slug with no board answers 404, which is an
 * answer about the board rather than a failure, so it is never retried, and the other region is not
 * asked. Lever names the company nowhere in a posting, so a probe brings no name. Lever answers a
 * gateway error now and then (#79, #82), which is retried without slowing the run.
 */
@Component
public class LeverBoardReaderAdapter implements BoardReaderPort {

	private static final Map<RegionEnum, URI> POSTINGS_APIS = Map.of(RegionEnum.US,
			URI.create("https://api.lever.co/"), RegionEnum.EU, URI.create("https://api.eu.lever.co/"));

	/** What Lever answers when it asks to be given time, which slows the run down. */
	private static final Set<Integer> REFUSED = Set.of(429, 503);

	/**
	 * What Lever answers now and then when it fails to reach a board's server (#79, #82): retried
	 * without slowing the run, since nothing tied them to load.
	 */
	private static final Set<Integer> GATEWAY_ERRORS = Set.of(500, 502, 504);

	private static final int ATTEMPTS = 5;

	private static final Duration BACK_OFF = Duration.ofSeconds(1);

	/** Lever took up to 24 seconds to answer, even with nothing to send. */
	private static final Duration READ_TIMEOUT = Duration.ofMinutes(1);

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
	public LeverBoardReaderAdapter() {
		this(POSTINGS_APIS, UserAgent.VALUE, ATTEMPTS, BACK_OFF, READ_TIMEOUT);
	}

	/**
	 * @param bases what board paths are resolved against, for each region
	 * @param userAgent how this client identifies itself
	 * @param attempts how many times a request is made before it is given up on
	 * @param backOff how long to wait after the first refusal, doubling with each further one
	 * @param readTimeout how long a board is given to answer
	 */
	LeverBoardReaderAdapter(Map<RegionEnum, URI> bases, String userAgent, int attempts, Duration backOff,
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
	public BoardReading read(String slug, RegionEnum region, Runnable onRefusal) throws IOException {
		HttpRequest request = HttpRequest
			.newBuilder(this.bases.get(region).resolve("v0/postings/%s?mode=json&limit=1".formatted(slug)))
			.header("User-Agent", this.userAgent)
			.timeout(this.readTimeout)
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
		JsonNode postings;
		try {
			postings = this.json.readTree(body);
		}
		catch (JacksonException ex) {
			throw new IOException("Unreadable answer for the board of " + slug, ex);
		}
		if (!postings.isArray()) {
			throw new IOException("The answer for the board of %s named no postings".formatted(slug));
		}
		return new BoardReading(postings.isEmpty() ? BoardStatusEnum.EMPTY : BoardStatusEnum.ACTIVE, null);
	}

	/**
	 * Asks until the ATS answers, waiting after a refusal as long as it says, or for the back-off if
	 * it does not.
	 */
	private HttpResponse<String> send(HttpRequest request, String slug, Runnable onRefusal) throws IOException {
		for (int attempt = 1;; attempt++) {
			try {
				HttpResponse<String> response = this.http.send(request, BodyHandlers.ofString());
				int status = response.statusCode();
				boolean refused = REFUSED.contains(status);
				if (!refused && !GATEWAY_ERRORS.contains(status)) {
					return response;
				}
				if (refused) {
					onRefusal.run();
				}
				if (attempt == this.attempts) {
					throw new IOException("Asked for the board of %s and got %d".formatted(slug, status));
				}
				Duration backOff = this.backOff.multipliedBy(1L << (attempt - 1));
				Thread.sleep(refused ? RetryAfter.or(response, backOff) : backOff);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while reading the board of " + slug, ex);
			}
		}
	}

}
