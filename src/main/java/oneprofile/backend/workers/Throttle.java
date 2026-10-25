package oneprofile.backend.workers;

import java.time.Duration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A pace as it is held while requests are asked at it: how many may be open at once, and how many may
 * start a second, each halved by every refusal, down to one at a time and one a second (ADR-0025). A
 * refusal can also hold every start while it is waited out, and then a burst of refusals halves the
 * limits once.
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

	/** Whether a refusal is being waited out: no request started after its hold has been answered yet. */
	private boolean held;

	private long heldUntil;

	private Duration lastHold;

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
	 * @return when it starts, as {@link System#nanoTime()} reads, for {@link #refused(long, Duration, Duration)}
	 * and {@link #answered(long)}
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
	 * Takes a refused request and holds every start until the refusal is waited out. The first
	 * refusal of a burst halves both limits and holds for {@code firstHold}. A request started before
	 * the hold is over was asked into the same refusal, so its refusal says nothing new. A request
	 * started after the hold, refused before any such request was answered, finds the refusal still
	 * standing: the throttle holds again for twice as long, up to {@code longestHold}, without halving.
	 * A burst of refusals is so one halving, however many requests were open or waiting.
	 * @param startedAt when the refused request started, as {@link #awaitStart()} returned it
	 * @param firstHold how long the first refusal of a burst holds every start
	 * @param longestHold how long a hold lasts at most
	 * @return whether the limits were halved
	 */
	public synchronized boolean refused(long startedAt, Duration firstHold, Duration longestHold) {
		if (this.held && startedAt - this.heldUntil < 0) {
			return false;
		}
		boolean halved = !this.held;
		Duration hold = firstHold;
		if (halved) {
			refused();
		}
		else if (this.lastHold.multipliedBy(2).compareTo(longestHold) < 0) {
			hold = this.lastHold.multipliedBy(2);
		}
		else {
			hold = longestHold;
		}
		this.held = true;
		this.lastHold = hold;
		this.heldUntil = System.nanoTime() + hold.toNanos();
		if (this.heldUntil - this.nextStartAt > 0) {
			this.nextStartAt = this.heldUntil;
		}
		return halved;
	}

	/**
	 * Takes an answer that was no refusal, which ends a burst of refusals once the request was started
	 * after the hold.
	 * @param startedAt when the answered request started, as {@link #awaitStart()} returned it
	 */
	public synchronized void answered(long startedAt) {
		if (this.held && startedAt - this.heldUntil >= 0) {
			this.held = false;
		}
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
