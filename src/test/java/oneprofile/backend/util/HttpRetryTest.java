package oneprofile.backend.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class HttpRetryTest {

	private final HttpRetry retry = new HttpRetry("Test service", 3, Duration.ZERO, false);

	private final AtomicInteger calls = new AtomicInteger();

	@Test
	void retriesAServerErrorUntilItWorks() {
		String result = this.retry.call("a page", () -> {
			if (this.calls.incrementAndGet() < 3) {
				throw HttpRetry.errorFor(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", new byte[0]);
			}
			return "ok";
		});

		assertThat(result).isEqualTo("ok");
		assertThat(this.calls).hasValue(3);
	}

	@Test
	void retriesANetworkError() {
		String result = this.retry.call("a page", () -> {
			if (this.calls.incrementAndGet() == 1) {
				throw new ResourceAccessException("Connection reset", new IOException());
			}
			return "ok";
		});

		assertThat(result).isEqualTo("ok");
		assertThat(this.calls).hasValue(2);
	}

	@Test
	void givesUpOnTheLastAttempt() {
		assertThatThrownBy(() -> this.retry.call("a page", () -> {
			this.calls.incrementAndGet();
			throw HttpRetry.errorFor(HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout", new byte[0]);
		})).isInstanceOf(HttpServerErrorException.class);

		assertThat(this.calls).hasValue(3);
	}

	@Test
	void doesNotRetryAClientError() {
		assertThatThrownBy(() -> this.retry.call("a page", () -> {
			this.calls.incrementAndGet();
			throw HttpRetry.errorFor(HttpStatus.BAD_REQUEST, "Bad Request", new byte[0]);
		})).isInstanceOf(HttpClientErrorException.class);

		assertThat(this.calls).hasValue(1);
	}

	@Test
	void anErrorSaysWhatTheServiceAnswered() {
		byte[] body = "{\"message\": \"Page 5 invalid\"}".getBytes(StandardCharsets.UTF_8);

		assertThat(HttpRetry.errorFor(HttpStatus.BAD_REQUEST, "Bad Request", body))
			.hasMessage("400 Bad Request: {\"message\": \"Page 5 invalid\"}");
	}

	@Test
	void doesNotRetryTooManyRequestsUnlessEnabled() {
		assertThatThrownBy(() -> this.retry.call("a page", () -> {
			this.calls.incrementAndGet();
			throw HttpRetry.errorFor(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new byte[0]);
		})).isInstanceOf(HttpClientErrorException.class);

		assertThat(this.calls).hasValue(1);
	}

	@Test
	void retriesTooManyRequestsWhenEnabled() {
		HttpRetry rateLimited = new HttpRetry("Test service", 3, Duration.ZERO, true);

		String result = rateLimited.call("a page", () -> {
			if (this.calls.incrementAndGet() == 1) {
				throw HttpRetry.errorFor(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new byte[0]);
			}
			return "ok";
		});

		assertThat(result).isEqualTo("ok");
		assertThat(this.calls).hasValue(2);
	}
}
