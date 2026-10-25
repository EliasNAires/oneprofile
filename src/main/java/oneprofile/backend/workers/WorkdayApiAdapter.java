package oneprofile.backend.workers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.MissingNode;

/**
 * Asks Workday's careers-site API, at {@code <tenant>.<host>.myworkdayjobs.com/wday/cxs/<tenant>/<site>},
 * as docs/measurements/2026-10-workday.md measured it.
 * <p>
 * Cloudflare limits this machine across every Workday host together, so one throttle paces every
 * request a Workday probe or sweep asks, whichever run asks it: 16 requests a second and 20 open at
 * once, decided with Elias on 2026-10-23 for probes and sweeps together, a departure from ADR-0025's
 * half of the ceiling. A run's own pace counts boards, and a sweep asks many requests a board.
 * <p>
 * Cloudflare refuses with a {@code 429} marked {@code cf-mitigated: challenge}, with no
 * {@code Retry-After}; it cleared within one to five minutes. Every request then waits it out
 * together, held by the throttle from one minute, doubling while it still refuses, and the burst
 * slows the throttle and the run down once: in a sweep on 2026-10-24, each request refused after a
 * challenge was asked to halve again, and one challenge took the pace from 16 a second to 2. A tenant's own {@code 429}, with Workday's JSON error body, and a gateway error from one
 * tenant's server ({@code 502}, {@code 503} "no healthy upstream", {@code 504}, {@code 520}) are asked
 * again without slowing anything. Workday's other errors ({@code 400}, {@code 401}, {@code 403},
 * {@code 404}, {@code 422}) say a board cannot be read, and are answers.
 */
@Component
public class WorkdayApiAdapter implements WorkdayApiPort {

	private static final String BOARD_API = "https://%1$s.%2$s.myworkdayjobs.com/wday/cxs/%1$s/%3$s/";

	private static final int REQUESTS_AT_ONCE = 20;

	private static final int STARTS_A_SECOND = 16;

	/**
	 * How long Workday has to be left alone before a slowed throttle takes its full pace back: longer
	 * than any wait inside a run, so that only the end of every run that shares it restores it.
	 */
	private static final Duration RESTORED_AFTER_IDLE = Duration.ofMinutes(10);

	private static final Set<Integer> GATEWAY_ERRORS = Set.of(502, 503, 504, 520);

	private static final int ATTEMPTS = 5;

	/** Cloudflare's challenge cleared within one to five minutes; the throttle holds this long at first. */
	private static final Duration REFUSAL_BACK_OFF = Duration.ofMinutes(1);

	private static final Duration BACK_OFF = Duration.ofSeconds(1);

	/** An answer took up to two minutes in the long probe. */
	private static final Duration READ_TIMEOUT = Duration.ofMinutes(3);

	private static final Duration MAX_BACK_OFF = Duration.ofMinutes(5);

	private final HttpClient http = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(10))
		.build();

	private final ObjectMapper json = new ObjectMapper();

	private final String boardApi;

	private final String userAgent;

	private final Throttle throttle;

	private final int attempts;

	private final Duration refusalBackOff;

	private final Duration backOff;

	private final Duration readTimeout;

	/** Asks Workday. */
	@Autowired
	public WorkdayApiAdapter() {
		this(BOARD_API, UserAgent.VALUE,
				new Throttle("Workday", REQUESTS_AT_ONCE, STARTS_A_SECOND, RESTORED_AFTER_IDLE), ATTEMPTS,
				REFUSAL_BACK_OFF, BACK_OFF, READ_TIMEOUT);
	}

	/**
	 * @param boardApi the URL of a board's API, its tenant, host and site written {@code %1$s},
	 * {@code %2$s} and {@code %3$s}
	 * @param userAgent how this client identifies itself
	 * @param throttle what paces every request
	 * @param attempts how many times a request is made before it is given up on
	 * @param refusalBackOff how long every request waits after Cloudflare's first refusal of a burst, doubling
	 * while it still refuses
	 * @param backOff how long to wait after any other first failure, doubling with each further one
	 * @param readTimeout how long a request is given to answer
	 */
	WorkdayApiAdapter(String boardApi, String userAgent, Throttle throttle, int attempts, Duration refusalBackOff,
			Duration backOff, Duration readTimeout) {
		if (attempts < 1) {
			throw new IllegalArgumentException("A request has to be attempted at least once");
		}
		this.boardApi = boardApi;
		this.userAgent = userAgent;
		this.throttle = throttle;
		this.attempts = attempts;
		this.refusalBackOff = refusalBackOff;
		this.backOff = backOff;
		this.readTimeout = readTimeout;
	}

	@Override
	public Answer jobs(String slug, Map<String, List<String>> appliedFacets, int offset, int limit, Runnable onRefusal)
			throws IOException {
		Map<String, Object> search = new LinkedHashMap<>();
		search.put("appliedFacets", appliedFacets);
		search.put("limit", limit);
		search.put("offset", offset);
		search.put("searchText", "");
		HttpRequest request = request(slug, "jobs").header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(this.json.writeValueAsString(search)))
			.build();
		return send(request, slug, onRefusal);
	}

	@Override
	public Answer job(String slug, String externalPath, Runnable onRefusal) throws IOException {
		String path = externalPath.startsWith("/") ? externalPath.substring(1) : externalPath;
		return send(request(slug, path).GET().build(), slug, onRefusal);
	}

	private HttpRequest.Builder request(String slug, String path) throws IOException {
		String[] board = slug.split("[./]", 3);
		if (board.length != 3) {
			throw new IllegalArgumentException("Not a Workday slug: " + slug);
		}
		try {
			return HttpRequest.newBuilder(URI.create(this.boardApi.formatted(board[0], board[1], board[2]) + path))
				.header("User-Agent", this.userAgent)
				.header("Accept", "application/json")
				.timeout(this.readTimeout);
		}
		catch (IllegalArgumentException ex) {
			throw new IOException("Not a path Workday can be asked for on %s: %s".formatted(slug, path), ex);
		}
	}

	/**
	 * Asks until Workday answers about the board, held by the throttle after a refusal and waiting after
	 * a gateway error, each attempt paced by the throttle.
	 */
	private Answer send(HttpRequest request, String slug, Runnable onRefusal) throws IOException {
		for (int attempt = 1;; attempt++) {
			long startedAt = awaitStart(request);
			HttpResponse<String> response = sendOnce(request);
			int status = response.statusCode();
			boolean challenged = status == 429
					&& response.headers().firstValue("cf-mitigated").filter("challenge"::equalsIgnoreCase).isPresent();
			if (!challenged) {
				this.throttle.answered(startedAt);
			}
			if (status != 429 && !GATEWAY_ERRORS.contains(status)) {
				return answer(response, request);
			}
			if (challenged && this.throttle.refused(startedAt, this.refusalBackOff, MAX_BACK_OFF)) {
				onRefusal.run();
			}
			if (attempt == this.attempts) {
				throw new IOException("Asked %s of %s and got %d".formatted(request.uri(), slug, status));
			}
			if (!challenged) {
				Duration doubled = this.backOff.multipliedBy(1L << (attempt - 1));
				wait(RetryAfter.or(response, (doubled.compareTo(MAX_BACK_OFF) > 0) ? MAX_BACK_OFF : doubled), slug);
			}
		}
	}

	private long awaitStart(HttpRequest request) throws IOException {
		try {
			return this.throttle.awaitStart();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting to ask " + request.uri(), ex);
		}
	}

	/** Sends a request the throttle has started, and counts it finished once answered. */
	private HttpResponse<String> sendOnce(HttpRequest request) throws IOException {
		try {
			return this.http.send(request, BodyHandlers.ofString());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while asking " + request.uri(), ex);
		}
		finally {
			this.throttle.finished();
		}
	}

	/** A {@code 200} has to be JSON; an error's body is kept where it is JSON, as Workday's are. */
	private Answer answer(HttpResponse<String> response, HttpRequest request) throws IOException {
		try {
			return new Answer(response.statusCode(), this.json.readTree(response.body()));
		}
		catch (JacksonException ex) {
			if (response.statusCode() == 200) {
				throw new IOException("Unreadable answer from " + request.uri(), ex);
			}
			return new Answer(response.statusCode(), MissingNode.getInstance());
		}
	}

	private static void wait(Duration wait, String slug) throws IOException {
		try {
			Thread.sleep(wait);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting to ask again about " + slug, ex);
		}
	}

}
