package oneprofile.backend.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.function.Consumer;

import oneprofile.backend.util.HttpRetry;
import oneprofile.backend.util.ProgressLog;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads the Wayback Machine CDX index. It answers a prefix query with one captured URL
 * per line of plain text, split into pages.
 */
@Component
public class WaybackCdxClient {

	private static final String CDX_URL = "https://web.archive.org/cdx/search/cdx?url={pattern}&matchType=prefix";

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	private static final Duration READ_TIMEOUT = Duration.ofMinutes(2);

	private static final int MAX_ATTEMPTS = 4;

	/**
	 * Doubles on every retry, so the waits are 30s, 60s and 120s: a rate limit of the
	 * Internet Archive lasts minutes, and shorter waits would run out before it lifts.
	 */
	private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(30);

	/** The pace at which the whole archive was read without a single 429. */
	private static final Duration PAUSE_BETWEEN_PAGES = Duration.ofSeconds(2);

	/** One pattern takes up to ~270 pages; logging every page would flood the log. */
	private static final int PROGRESS_INTERVAL = 10;

	private final RestClient restClient;

	private final HttpRetry retry;

	private final Duration pauseBetweenPages;

	public WaybackCdxClient() {
		this(RestClient.builder().requestFactory(HttpRetry.requestFactory(CONNECT_TIMEOUT, READ_TIMEOUT)),
				FIRST_RETRY_DELAY, PAUSE_BETWEEN_PAGES);
	}

	WaybackCdxClient(RestClient.Builder builder, Duration firstRetryDelay, Duration pauseBetweenPages) {
		this.restClient = builder.build();
		this.retry = new HttpRetry("Wayback CDX", MAX_ATTEMPTS, firstRetryDelay, true);
		this.pauseBetweenPages = pauseBetweenPages;
	}

	/**
	 * Hands every URL the archive holds for {@code pattern} to {@code onUrl}, page by
	 * page. The same URL arrives many times: the archive reports every capture of it.
	 */
	public void forEachUrl(String pattern, Consumer<String> onUrl) {
		int pages = numberOfPages(pattern);
		ProgressLog progress = new ProgressLog("Wayback " + pattern, "pages", pages, PROGRESS_INTERVAL,
				Clock.systemUTC());
		for (int page = 0; page < pages; page++) {
			pause();
			readPage(pattern, page, onUrl);
			progress.itemDone(false);
		}
	}

	/** Asked without {@code fl}: with it, the archive answers {@code -} instead of the count. */
	private int numberOfPages(String pattern) {
		String body = this.retry.call("the page count of " + pattern, () -> this.restClient.get()
				.uri(CDX_URL + "&showNumPages=true", pattern)
				.retrieve()
				.body(String.class));
		return Integer.parseInt(body.strip());
	}

	/**
	 * A retry that failed halfway through a page hands over its first URLs twice; the
	 * caller collects them in a set, so a repeat costs nothing.
	 */
	private void readPage(String pattern, int page, Consumer<String> onUrl) {
		this.retry.call("page " + page + " of " + pattern, () -> this.restClient.get()
				.uri(CDX_URL + "&fl=original&page=" + page, pattern)
				.exchange((request, response) -> {
					HttpStatusCode status = response.getStatusCode();
					if (status.isError()) {
						throw HttpRetry.errorFor(status, response.getStatusText(), response.getBody().readAllBytes());
					}
					streamUrls(response.getBody(), onUrl);
					return null;
				}));
	}

	private void streamUrls(InputStream body, Consumer<String> onUrl) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					onUrl.accept(line);
				}
			}
		}
	}

	private void pause() {
		if (this.pauseBetweenPages.isZero()) {
			return;
		}
		try {
			Thread.sleep(this.pauseBetweenPages);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RestClientException("Interrupted while reading the Wayback CDX index", ex);
		}
	}
}
