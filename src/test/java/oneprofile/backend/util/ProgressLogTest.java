package oneprofile.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ProgressLogTest {

	@Test
	void logsOnTheIntervalAndNowhereElse(CapturedOutput output) {
		ProgressLog progress = new ProgressLog("Test batch", "items", 25, 10,
				Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

		for (int i = 0; i < 25; i++) {
			progress.itemDone(false);
		}

		assertThat(output.getOut()).contains("Test batch progress: 10 of 25 items")
				.contains("Test batch progress: 20 of 25 items");
		assertThat(occurrencesOf(output.getOut(), "Test batch progress:")).isEqualTo(2);
	}

	@Test
	void doesNotLogOnTheLastItemEvenWhenItLandsOnTheInterval(CapturedOutput output) {
		ProgressLog progress = new ProgressLog("Test batch", "items", 20, 10,
				Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

		for (int i = 0; i < 20; i++) {
			progress.itemDone(false);
		}

		assertThat(output.getOut()).contains("10 of 20").doesNotContain("20 of 20");
	}

	@Test
	void countsTheFailuresSinceTheLastLog(CapturedOutput output) {
		ProgressLog progress = new ProgressLog("Test batch", "items", 20, 10,
				Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

		for (int i = 0; i < 9; i++) {
			progress.itemDone(false);
		}
		progress.itemDone(true);

		assertThat(output.getOut()).contains("10 of 20 items (50%), 1 failed");
	}

	@Test
	void computesThePercentAndTheRateFromTheClock(CapturedOutput output) {
		MutableClock clock = new MutableClock(Instant.EPOCH);
		ProgressLog progress = new ProgressLog("Greenhouse board probe", "companies", 18051, 1000, clock);

		for (int i = 0; i < 999; i++) {
			progress.itemDone(false);
		}
		clock.advance(Duration.ofSeconds(469));
		progress.itemDone(false);

		assertThat(output.getOut()).contains(
				"Greenhouse board probe progress: 1000 of 18051 companies (5%), 0 failed, 128 per min, 7m49s elapsed");
	}

	private static int occurrencesOf(String text, String needle) {
		int count = 0;
		int index = 0;
		while ((index = text.indexOf(needle, index)) != -1) {
			count++;
			index += needle.length();
		}
		return count;
	}

	/** A {@link Clock} the test can move forward by hand, so no test waits on real time. */
	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		void advance(Duration by) {
			this.instant = this.instant.plus(by);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return this.instant;
		}
	}
}
