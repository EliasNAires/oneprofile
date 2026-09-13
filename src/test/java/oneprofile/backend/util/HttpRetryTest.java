package oneprofile.backend.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class HttpRetryTest {

	private final HttpRetry retry = new HttpRetry("Test service", 3, Duration.ZERO);

	private final AtomicInteger calls = new AtomicInteger();

	@Test
	void retriesAServerErrorUntilItWorks() {
		String result = this.retry.call("a page", () -> {
			if (this.calls.incrementAndGet() < 3) {
				throw HttpRetry.errorFor(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable");
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
			throw HttpRetry.errorFor(HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout");
		})).isInstanceOf(HttpServerErrorException.class);

		assertThat(this.calls).hasValue(3);
	}

	@Test
	void doesNotRetryAClientError() {
		assertThatThrownBy(() -> this.retry.call("a page", () -> {
			this.calls.incrementAndGet();
			throw HttpRetry.errorFor(HttpStatus.BAD_REQUEST, "Bad Request");
		})).isInstanceOf(HttpClientErrorException.class);

		assertThat(this.calls).hasValue(1);
	}
}
