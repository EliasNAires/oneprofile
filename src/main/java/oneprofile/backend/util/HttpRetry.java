package oneprofile.backend.util;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

	private final boolean retriesTooManyRequests;

	/**
	 * @param service how the service is named in the log
	 * @param firstDelay doubles on every retry
	 * @param retriesTooManyRequests whether a 429 counts as transient, for a service whose
	 * rate limit lifts on its own
	 */
	public HttpRetry(String service, int maxAttempts, Duration firstDelay, boolean retriesTooManyRequests) {
		this.service = service;
		this.maxAttempts = maxAttempts;
		this.firstDelay = firstDelay;
		this.retriesTooManyRequests = retriesTooManyRequests;
	}

	/**
	 * Retries a transient failure —5xx, a network error, and a 429 if enabled— and nothing
	 * else. Any other 4xx means the request itself is wrong, and repeating it changes nothing.
	 */
	public <T> T call(String what, Supplier<T> call) {
		for (int attempt = 1;; attempt++) {
			try {
				return call.get();
			}
			catch (RestClientException ex) {
				if (!isTransient(ex) || attempt == this.maxAttempts) {
					logger.warn("{} failed on {} (attempt {} of {}): {}. Giving up", this.service, what, attempt,
							this.maxAttempts, ex.getMessage());
					throw ex;
				}
				Duration delay = this.firstDelay.multipliedBy(1L << (attempt - 1));
				logger.warn("{} failed on {} (attempt {} of {}): {}. Retrying in {}s", this.service, what, attempt,
						this.maxAttempts, ex.getMessage(), delay.toSeconds());
				sleep(delay);
			}
		}
	}

	private boolean isTransient(RestClientException ex) {
		if (ex instanceof HttpClientErrorException clientError) {
			return this.retriesTooManyRequests && clientError.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
		}
		return true;
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

	/**
	 * Needed with {@code .exchange()}, which hands over the error status instead of throwing.
	 * The body goes into the message because it is where the service says why it refused.
	 */
	public static RestClientException errorFor(HttpStatusCode status, String statusText, byte[] body) {
		String message = status.value() + " " + statusText;
		if (body.length > 0) {
			message += ": " + new String(body, StandardCharsets.UTF_8);
		}
		return status.is4xxClientError()
				? HttpClientErrorException.create(message, status, statusText, HttpHeaders.EMPTY, body,
						StandardCharsets.UTF_8)
				: HttpServerErrorException.create(message, status, statusText, HttpHeaders.EMPTY, body,
						StandardCharsets.UTF_8);
	}

	/** Explicit timeouts so a stalled request dies with an exception instead of hanging. */
	public static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(connectTimeout).build());
		factory.setReadTimeout(readTimeout);
		return factory;
	}
}
