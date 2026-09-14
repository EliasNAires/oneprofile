package oneprofile.backend.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the progress of a long-running batch every N items, so following a run in prod
 * does not require querying the database. Not thread-safe: meant for a single loop.
 */
public final class ProgressLog {

	private static final Logger logger = LoggerFactory.getLogger(ProgressLog.class);

	private final String process;

	private final String unit;

	private final int total;

	private final int interval;

	private final Clock clock;

	private final Instant startedAt;

	private int done;

	private int failed;

	public ProgressLog(String process, String unit, int total, int interval, Clock clock) {
		this.process = process;
		this.unit = unit;
		this.total = total;
		this.interval = interval;
		this.clock = clock;
		this.startedAt = clock.instant();
	}

	/**
	 * Counts one more item done, and logs when it lands on the interval. The last item
	 * never logs here: its process already logs a {@code finished} line of its own.
	 */
	public void itemDone(boolean failed) {
		this.done++;
		if (failed) {
			this.failed++;
		}
		if (this.done % this.interval == 0 && this.done < this.total) {
			log();
		}
	}

	private void log() {
		long elapsedSeconds = Math.round(Duration.between(this.startedAt, this.clock.instant()).toMillis() / 1000.0);
		int pct = this.done * 100 / this.total;
		long perMin = elapsedSeconds == 0 ? 0 : Math.round(this.done * 60.0 / elapsedSeconds);
		logger.info("{} progress: {} of {} {} ({}%), {} failed, {} per min, {} elapsed", this.process, this.done,
				this.total, this.unit, pct, this.failed, perMin, format(elapsedSeconds));
	}

	private static String format(long elapsedSeconds) {
		return (elapsedSeconds / 60) + "m" + (elapsedSeconds % 60) + "s";
	}
}
