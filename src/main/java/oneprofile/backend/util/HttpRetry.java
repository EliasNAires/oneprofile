package oneprofile.backend.util;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

/**
 * What every client of an external service does the same way: retry a transient
 * failure with a growing wait, turn an error status into an exception, and never hang.
 */
public final class HttpRetry {

	private static final Logger logger = LoggerFactory.getLogger(HttpRetry.class);

	private final String service;

	private final int maxAttempts;

	private final Duration firstDelay;

	/**
	 * @param service how the service is named in the log
	 * @param firstDelay doubles on every retry
	 */
	public HttpRetry(String service, int maxAttempts, Duration firstDelay) {
		this.service = service;
		this.maxAttempts = maxAttempts;
		this.firstDelay = firstDelay;
	}

	/**
	 * Retries a transient failure —5xx or a network error— and nothing else. A 4xx means
	 * the request itself is wrong, and repeating it changes nothing.
	 */
	public <T> T call(String what, Supplier<T> call) {
		for (int attempt = 1;; attempt++) {
			try {
				return call.get();
			}
			catch (HttpClientErrorException ex) {
				throw ex;
			}
			catch (RestClientException ex) {
				if (attempt == this.maxAttempts) {
					throw ex;
				}
				Duration delay = this.firstDelay.multipliedBy(1L << (attempt - 1));
				logger.warn("{} failed on {} (attempt {} of {}): {}. Retrying in {}s", this.service, what, attempt,
						this.maxAttempts, ex.getMessage(), delay.toSeconds());
				sleep(delay);
			}
		}
	}

	private void sleep(Duration delay) {
		try {
			Thread.sleep(delay);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RestClientException("Interrupted while waiting to retry " + this.service, ex);
		}
	}

	/** Needed with {@code .exchange()}, which hands over the error status instead of throwing. */
	public static RestClientException errorFor(HttpStatusCode status, String statusText) {
		return status.is4xxClientError()
				? HttpClientErrorException.create(status, statusText, HttpHeaders.EMPTY, new byte[0], null)
				: HttpServerErrorException.create(status, statusText, HttpHeaders.EMPTY, new byte[0], null);
	}

	/** Explicit timeouts so a stalled request dies with an exception instead of hanging. */
	public static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(connectTimeout).build());
		factory.setReadTimeout(readTimeout);
		return factory;
	}
}
