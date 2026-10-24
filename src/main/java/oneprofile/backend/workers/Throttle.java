package oneprofile.backend.workers;

import java.time.Duration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A pace as it is held while requests are asked at it: how many may be open at once, and how many may
 * start a second, each halved by every refusal, down to one at a time and one a second (ADR-0025).
 * <p>
 * A throttle one run holds for itself keeps its halving for the rest of that run. A throttle that
 * several runs share, as Workday's probes and sweeps share one, cannot tell where a run ends, so it
 * takes its full pace back once it has been left idle for a while: no request open, and none finished
 * within that time.
 */
public final class Throttle {

	private static final Log logger = LogFactory.getLog(Throttle.class);

	private final String name;

	private final int fullRequestsAtOnce;

	private final int fullStartsASecond;

	private final Duration restoredAfterIdle;

	private int requestsAtOnce;

	private int startsASecond;

	private int open;

	private long nextStartAt = System.nanoTime();

	private long lastFinishedAt = System.nanoTime();

	private long lastHalvedAt = Long.MIN_VALUE;

	/**
	 * A throttle that keeps every halving for as long as it is held.
	 * @param name what is asking at this pace, named in the log when it is refused
	 * @param requestsAtOnce how many requests are open at once at most
	 * @param startsASecond how many requests start each second at most
	 */
	public Throttle(String name, int requestsAtOnce, int startsASecond) {
		this(name, requestsAtOnce, startsASecond, null);
	}

	/**
	 * @param name what is asking at this pace, named in the log when it is refused
	 * @param requestsAtOnce how many requests are open at once at most
	 * @param startsASecond how many requests start each second at most
	 * @param restoredAfterIdle how long it has to be idle to take its full pace back, or null never to
	 */
	public Throttle(String name, int requestsAtOnce, int startsASecond, Duration restoredAfterIdle) {
		if (requestsAtOnce < 1 || startsASecond < 1) {
			throw new IllegalArgumentException("A throttle has to let at least one request through at a time, once a second");
		}
		this.name = name;
		this.fullRequestsAtOnce = requestsAtOnce;
		this.fullStartsASecond = startsASecond;
		this.restoredAfterIdle = restoredAfterIdle;
		this.requestsAtOnce = requestsAtOnce;
		this.startsASecond = startsASecond;
	}

	/**
	 * Waits until a request may be started, and counts it as open from then on, until
	 * {@link #finished()}.
	 * @return when it starts, as {@link System#nanoTime()} reads, for {@link #refused(long)}
	 * @throws InterruptedException if interrupted while waiting
	 */
	public long awaitStart() throws InterruptedException {
		long startAt;
		synchronized (this) {
			restoreIfIdle();
			while (this.open >= this.requestsAtOnce) {
				wait();
			}
			this.open++;
			startAt = Math.max(System.nanoTime(), this.nextStartAt);
			this.nextStartAt = startAt + Duration.ofSeconds(1).toNanos() / this.startsASecond;
		}
		long remaining = startAt - System.nanoTime();
		if (remaining > 0) {
			Thread.sleep(Duration.ofNanos(remaining));
		}
		return startAt;
	}

	/** Counts a request started by {@link #awaitStart()} as no longer open. */
	public synchronized void finished() {
		this.open--;
		this.lastFinishedAt = System.nanoTime();
		notifyAll();
	}

	/**
	 * Halves both limits for a refused request, unless the request started before they were last
	 * halved: it was asked at the pace already given up, so its refusal says nothing new. A limit that
	 * refuses every request open at once is then one refusal, not one for each of them.
	 * @param startedAt when the refused request started, as {@link #awaitStart()} returned it
	 * @return whether the limits were halved
	 */
	public synchronized boolean refused(long startedAt) {
		if (this.lastHalvedAt != Long.MIN_VALUE && startedAt - this.lastHalvedAt < 0) {
			return false;
		}
		refused();
		this.lastHalvedAt = System.nanoTime();
		return true;
	}

	/** Halves both limits, since a request was refused. */
	public synchronized void refused() {
		this.requestsAtOnce = Math.max(1, this.requestsAtOnce / 2);
		this.startsASecond = Math.max(1, this.startsASecond / 2);
		logger.warn("%s was refused, so it slows down to %d requests at once and %d a second".formatted(this.name,
				this.requestsAtOnce, this.startsASecond));
	}

	/** How many requests may be open at once now. */
	public synchronized int requestsAtOnce() {
		restoreIfIdle();
		return this.requestsAtOnce;
	}

	/** How many requests may start a second now. */
	public synchronized int startsASecond() {
		restoreIfIdle();
		return this.startsASecond;
	}

	private void restoreIfIdle() {
		boolean slowed = this.requestsAtOnce < this.fullRequestsAtOnce || this.startsASecond < this.fullStartsASecond;
		if (slowed && this.restoredAfterIdle != null && this.open == 0
				&& System.nanoTime() - this.lastFinishedAt >= this.restoredAfterIdle.toNanos()) {
			this.requestsAtOnce = this.fullRequestsAtOnce;
			this.startsASecond = this.fullStartsASecond;
			logger.info("%s has been idle, so it is back to %d requests at once and %d a second".formatted(this.name,
					this.requestsAtOnce, this.startsASecond));
		}
	}

}
