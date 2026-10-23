package oneprofile.backend.workers;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * How long an ATS that refused a request asks to be left alone: its {@code Retry-After} header, in
 * seconds or as a date, when it sends one. No ATS was seen to send one (#82), so a reader honours it
 * only if it is there.
 */
public final class RetryAfter {

	private RetryAfter() {
	}

	/**
	 * @param refusal the answer that refused a request
	 * @param otherwise how long to wait if the answer does not say
	 * @return how long to wait before asking again, never less than nothing
	 */
	public static Duration or(HttpResponse<?> refusal, Duration otherwise) {
		String value = refusal.headers().firstValue("Retry-After").orElse(null);
		if (value == null) {
			return otherwise;
		}
		try {
			return Duration.ofSeconds(Math.max(0, Long.parseLong(value.trim())));
		}
		catch (NumberFormatException ex) {
			// Not a number of seconds, so a date.
		}
		try {
			Instant until = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
			Duration remaining = Duration.between(Instant.now(), until);
			return remaining.isNegative() ? Duration.ZERO : remaining;
		}
		catch (DateTimeParseException ex) {
			return otherwise;
		}
	}

}
