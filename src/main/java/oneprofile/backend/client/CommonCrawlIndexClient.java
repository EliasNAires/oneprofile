package oneprofile.backend.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import oneprofile.backend.util.HttpRetry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Reads the CommonCrawl URL index (CDX). The index answers a prefix query with one
 * JSON line per capture, split into pages; a capture is a URL that CommonCrawl saw.
 */
@Component
public class CommonCrawlIndexClient {

	private static final String INDEX_URL = "https://index.commoncrawl.org/{indexId}-index"
			+ "?url={pattern}&matchType=prefix&output=json";

	/** Every index CommonCrawl publishes, newest first. */
	private static final String COLLECTIONS_URL = "https://index.commoncrawl.org/collinfo.json";

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	/** Generous: a single page of the index takes a while to come back. */
	private static final Duration READ_TIMEOUT = Duration.ofMinutes(2);

	/**
	 * The index gets overloaded and answers 503 or 504 now and then, which would
	 * otherwise throw away a run of seven large requests.
	 */
	private static final int MAX_ATTEMPTS = 4;

	/** Doubles on every retry, so the waits are 2s, 4s and 8s. */
	private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(2);

	private final RestClient restClient;

	private final HttpRetry retry;

	private final ObjectMapper json = new ObjectMapper();

	public CommonCrawlIndexClient() {
		this(RestClient.builder().requestFactory(HttpRetry.requestFactory(CONNECT_TIMEOUT, READ_TIMEOUT)),
				FIRST_RETRY_DELAY);
	}

	CommonCrawlIndexClient(RestClient.Builder builder, Duration firstRetryDelay) {
		this.restClient = builder.build();
		this.retry = new HttpRetry("CommonCrawl index", MAX_ATTEMPTS, firstRetryDelay, false);
	}

	/**
	 * The ids of the {@code count} most recent indexes, newest first. Asked on every
	 * run, so a new crawl comes in on its own and no id ever gets hardcoded.
	 */
	public List<String> latestIndexIds(int count) {
		String body = this.retry.call("the list of indexes", () -> this.restClient.get()
				.uri(COLLECTIONS_URL)
				.retrieve()
				.body(String.class));
		List<String> ids = new ArrayList<>(count);
		for (JsonNode index : this.json.readTree(body)) {
			if (ids.size() == count) {
				break;
			}
			ids.add(index.path("id").asString());
		}
		return ids;
	}

	/**
	 * Hands every URL the index holds for {@code pattern} to {@code onUrl}, page by
	 * page. The same URL can arrive more than once: the index reports captures, and a
	 * URL is captured again on every crawl.
	 */
	public void forEachUrl(String indexId, String pattern, Consumer<String> onUrl) {
		int pages = numberOfPages(indexId, pattern);
		for (int page = 0; page < pages; page++) {
			readPage(indexId, pattern, page, onUrl);
		}
	}

	private int numberOfPages(String indexId, String pattern) {
		String body = this.retry.call("the page count of " + pattern, () -> this.restClient.get()
				.uri(INDEX_URL + "&showNumPages=true", indexId, pattern)
				.retrieve()
				.body(String.class));
		return this.json.readTree(body).path("pages").asInt();
	}

	/**
	 * A retry that failed halfway through a page hands over its first URLs twice; the
	 * caller collects them in a set, so a repeat costs nothing.
	 */
	private void readPage(String indexId, String pattern, int page, Consumer<String> onUrl) {
		this.retry.call("page " + page + " of " + pattern, () -> this.restClient.get()
				.uri(INDEX_URL + "&page=" + page, indexId, pattern)
				.exchange((request, response) -> {
					HttpStatusCode status = response.getStatusCode();
					if (status.isError()) {
						throw HttpRetry.errorFor(status, response.getStatusText(), response.getBody().readAllBytes());
					}
					streamUrls(response.getBody(), onUrl);
					return null;
				}));
	}

	/**
	 * Line by line: a page of the index is far too big to hold in memory at once. An
	 * overloaded index closes the response halfway through a line, and the status stays
	 * 200; the cut line is turned into a network error so the page is asked for again.
	 */
	private void streamUrls(InputStream body, Consumer<String> onUrl) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				JsonNode url;
				try {
					url = this.json.readTree(line).path("url");
				}
				catch (JacksonException ex) {
					throw new ResourceAccessException("Page of the index arrived cut: " + ex.getOriginalMessage());
				}
				if (!url.isMissingNode()) {
					onUrl.accept(url.asString());
				}
			}
		}
	}
}
