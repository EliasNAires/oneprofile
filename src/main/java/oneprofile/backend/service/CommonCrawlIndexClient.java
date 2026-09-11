package oneprofile.backend.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads the CommonCrawl URL index (CDX). The index answers a prefix query with one
 * JSON line per capture, split into pages; a capture is a URL that CommonCrawl saw.
 */
@Component
public class CommonCrawlIndexClient {

	private static final Logger logger = LoggerFactory.getLogger(CommonCrawlIndexClient.class);

	private static final String INDEX_URL = "https://index.commoncrawl.org/{indexId}-index"
			+ "?url={pattern}&matchType=prefix&output=json";

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	/** Generous: a single page of the index takes a while to come back. */
	private static final Duration READ_TIMEOUT = Duration.ofMinutes(2);

	private static final int MAX_ATTEMPTS = 4;

	/** Doubles on every retry, so the waits are 2s, 4s and 8s. */
	private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(2);

	private final RestClient restClient;

	private final Duration firstRetryDelay;

	private final ObjectMapper json = new ObjectMapper();

	public CommonCrawlIndexClient() {
		this(RestClient.builder().requestFactory(requestFactory()), FIRST_RETRY_DELAY);
	}

	CommonCrawlIndexClient(RestClient.Builder builder, Duration firstRetryDelay) {
		this.restClient = builder.build();
		this.firstRetryDelay = firstRetryDelay;
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
		String body = withRetries("the page count of " + pattern, () -> this.restClient.get()
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
		withRetries("page " + page + " of " + pattern, () -> this.restClient.get()
				.uri(INDEX_URL + "&page=" + page, indexId, pattern)
				.exchange((request, response) -> {
					HttpStatusCode status = response.getStatusCode();
					if (status.isError()) {
						throw errorFor(status, response.getStatusText());
					}
					streamUrls(response.getBody(), onUrl);
					return null;
				}));
	}

	/**
	 * The index gets overloaded and answers 503 or 504 now and then, which would
	 * otherwise throw away a run of seven large requests. A 4xx is not retried: the
	 * index is saying the request itself is wrong, and repeating it changes nothing.
	 */
	private <T> T withRetries(String what, Supplier<T> call) {
		for (int attempt = 1;; attempt++) {
			try {
				return call.get();
			}
			catch (HttpClientErrorException ex) {
				throw ex;
			}
			catch (RestClientException ex) {
				if (attempt == MAX_ATTEMPTS) {
					throw ex;
				}
				Duration delay = this.firstRetryDelay.multipliedBy(1L << (attempt - 1));
				logger.warn("CommonCrawl index failed on {} (attempt {} of {}): {}. Retrying in {}s",
						what, attempt, MAX_ATTEMPTS, ex.getMessage(), delay.toSeconds());
				sleep(delay);
			}
		}
	}

	private static void sleep(Duration delay) {
		try {
			Thread.sleep(delay);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RestClientException("Interrupted while waiting to retry the CommonCrawl index", ex);
		}
	}

	private static RestClientException errorFor(HttpStatusCode status, String statusText) {
		return status.is4xxClientError()
				? HttpClientErrorException.create(status, statusText, HttpHeaders.EMPTY, new byte[0], null)
				: HttpServerErrorException.create(status, statusText, HttpHeaders.EMPTY, new byte[0], null);
	}

	/** Line by line: a page of the index is far too big to hold in memory at once. */
	private void streamUrls(InputStream body, Consumer<String> onUrl) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				JsonNode url = this.json.readTree(line).path("url");
				if (!url.isMissingNode()) {
					onUrl.accept(url.asString());
				}
			}
		}
	}

	/** Explicit timeouts so a stalled run dies with an exception instead of hanging. */
	private static ClientHttpRequestFactory requestFactory() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
		factory.setReadTimeout(READ_TIMEOUT);
		return factory;
	}
}
