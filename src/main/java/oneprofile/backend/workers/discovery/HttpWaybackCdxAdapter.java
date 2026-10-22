package oneprofile.backend.workers.discovery;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import oneprofile.backend.workers.UserAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks the Wayback Machine's CDX index, by default at {@code https://web.archive.org/cdx/search/cdx},
 * as {@code docs/measurements/2026-10-wayback.md} § "How Wayback's CDX is read" measured it: one
 * unpaged query per prefix, one request at a time, in chunks of 25,000 captures: the backfill's
 * chunks of 50,000 took 34 s at the median and up to 64 s, close enough to the gateway's 60 s that a
 * slow one became a {@code 504}.
 * <p>
 * The index refuses with a {@code 503} "Temporarily Offline" page rather than a 429, times out at
 * its gateway with a {@code 504}, and sends no {@code Content-Length}, so a cut answer looks whole
 * by its headers. Anything but a {@code 200} carrying a closed JSON array is therefore no answer: it
 * is waited out and the same request asked again, never skipped, so that no chunk of the read is
 * lost and no resume key is taken from a refusal.
 */
@Component
public class HttpWaybackCdxAdapter implements WaybackCdxPort {

	private static final Log logger = LogFactory.getLog(HttpWaybackCdxAdapter.class);

	private static final URI WAYBACK_CDX = URI.create("https://web.archive.org/cdx/search/cdx");

	/**
	 * How many times a request is asked before the read is given up on. Ten attempts wait out
	 * 30 s, 1, 2 and 4 minutes, then 5 minutes five times: about half an hour of refusals in a row,
	 * which is no longer a refusal to wait out but the index turning the read away.
	 */
	private static final int ATTEMPTS = 10;

	private static final Duration BACK_OFF = Duration.ofSeconds(30);

	private static final Duration MAX_BACK_OFF = Duration.ofMinutes(5);

	/**
	 * How long one attempt may take, body included. Above the gateway's own 60 s, so that a slow
	 * answer is cut by the gateway's 504 rather than by us; a stream that stalls without closing is
	 * cut here. {@link HttpRequest#timeout} would not do: it stops counting once the headers arrive,
	 * and the index sends them before it streams the rows.
	 */
	private static final Duration ANSWER_TIMEOUT = Duration.ofSeconds(90);

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

	private final HttpClient http = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(CONNECT_TIMEOUT)
		.build();

	private final ObjectMapper json = JsonMapper.builder().build();

	private final URI base;

	private final String userAgent;

	private final int attempts;

	private final Duration backOff;

	private final Duration maxBackOff;

	private final Duration answerTimeout;

	/** Asks the Wayback Machine. */
	@Autowired
	public HttpWaybackCdxAdapter() {
		this(WAYBACK_CDX, UserAgent.VALUE, ATTEMPTS, BACK_OFF, MAX_BACK_OFF, ANSWER_TIMEOUT);
	}

	/**
	 * @param base the CDX endpoint the query is sent to
	 * @param userAgent how this client identifies itself
	 * @param attempts how many times a request is made before the read is given up on
	 * @param backOff how long to wait after the first failed attempt, doubling with each further one
	 * @param maxBackOff the longest wait between two attempts
	 * @param answerTimeout how long one attempt may take, its body included
	 */
	HttpWaybackCdxAdapter(URI base, String userAgent, int attempts, Duration backOff, Duration maxBackOff,
			Duration answerTimeout) {
		if (attempts < 1) {
			throw new IllegalArgumentException("A request has to be attempted at least once");
		}
		this.base = base;
		this.userAgent = userAgent;
		this.attempts = attempts;
		this.backOff = backOff;
		this.maxBackOff = maxBackOff;
		this.answerTimeout = answerTimeout;
	}

	@Override
	public String chunk(String urlPrefix, Optional<String> resumeKey) throws IOException {
		String query = "url=%s&matchType=prefix&limit=25000&showResumeKey=true&output=json&fl=original"
			.formatted(encoded(urlPrefix)) + resumeKey.map((key) -> "&resumeKey=" + encoded(key)).orElse("");
		HttpRequest request = HttpRequest.newBuilder(URI.create(this.base + "?" + query))
			.header("User-Agent", this.userAgent)
			.GET()
			.build();
		String lastFailure = "";
		for (int attempt = 1; attempt <= this.attempts; attempt++) {
			if (attempt > 1) {
				waitBefore(attempt);
			}
			Answer answer = ask(request, resumeKey.isPresent());
			if (answer.failure().isEmpty()) {
				return answer.body();
			}
			lastFailure = answer.failure();
			logger.warn("Wayback gave no answer for %s (attempt %d of %d): %s".formatted(urlPrefix, attempt,
					this.attempts, lastFailure));
		}
		throw new IOException("Gave up on %s after %d attempts: %s".formatted(request.uri(), this.attempts, lastFailure));
	}

	/**
	 * Sends the request once and says what, if anything, kept it from being a whole answer.
	 * @param resumed whether the request continues from a resume key, after which an empty body
	 * cannot be the rest of the captures: the key said more remain
	 */
	private Answer ask(HttpRequest request, boolean resumed) throws IOException {
		HttpResponse<String> response;
		CompletableFuture<HttpResponse<String>> sent = this.http.sendAsync(request,
				BodyHandlers.ofString(StandardCharsets.UTF_8));
		try {
			response = sent.get(this.answerTimeout.toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex) {
			sent.cancel(true);
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while reading " + request.uri(), ex);
		}
		catch (TimeoutException ex) {
			sent.cancel(true);
			return new Answer("", "no whole answer within " + this.answerTimeout);
		}
		catch (ExecutionException ex) {
			// A dropped connection is as much a missing answer as a refusal.
			return new Answer("", "no response (%s)".formatted(ex.getCause()));
		}
		if (response.statusCode() != 200) {
			return new Answer("", "answered " + response.statusCode());
		}
		if (resumed && response.body().isBlank()) {
			return new Answer("", "answered 200 with an empty body after a resume key");
		}
		if (!isWhole(response.body())) {
			return new Answer("", "answered 200 with a body that does not close (%d chars)"
				.formatted(response.body().length()));
		}
		return new Answer(response.body(), "");
	}

	/**
	 * Whether a body is a whole answer: one JSON array that parses and closes, with nothing after it,
	 * or nothing at all where the prefix holds no captures. A body cut anywhere, even inside a URL
	 * that itself ends in {@code ]]}, does not parse.
	 */
	private boolean isWhole(String body) {
		if (body.isBlank()) {
			return true;
		}
		try (JsonParser parser = this.json.createParser(body)) {
			if (parser.nextToken() != JsonToken.START_ARRAY) {
				return false;
			}
			parser.skipChildren();
			return parser.nextToken() == null;
		}
		catch (JacksonException ex) {
			return false;
		}
	}

	private void waitBefore(int attempt) throws IOException {
		Duration doubled = this.backOff.multipliedBy(1L << Math.min(attempt - 2, 30));
		Duration wait = (doubled.compareTo(this.maxBackOff) > 0) ? this.maxBackOff : doubled;
		try {
			Thread.sleep(wait);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while backing off", ex);
		}
	}

	private static String encoded(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	/** One attempt's result: the body of a whole answer, or why there was none. */
	private record Answer(String body, String failure) {
	}

}
