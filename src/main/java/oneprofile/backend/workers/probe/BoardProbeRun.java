package oneprofile.backend.workers.probe;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.CompanyStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Probes every board held for one ATS and records what it finds.
 * <p>
 * Requests are paced, because a run walks every company held and an ATS is under no obligation to
 * serve them all at once. The pace is counted from the start of one request to the start of the
 * next, so what it sets is how many companies a minute a run gets through, whatever the ATS takes
 * to answer. A board that could not be read at all is left as it was rather than recorded as
 * anything, so the next run probes it again.
 */
@Component
public class BoardProbeRun {

	private static final Log logger = LogFactory.getLog(BoardProbeRun.class);

	/** The 130 boards a minute Greenhouse served without complaint before the reset. */
	private static final Duration PACE = Duration.ofMinutes(1).dividedBy(130);

	private final AtsEnum ats;

	private final BoardReaderPort reader;

	private final CompanyStore companies;

	private final Clock clock;

	private final Duration pace;

	/**
	 * Probes Greenhouse boards at the pace Greenhouse tolerates.
	 * @param reader reads Greenhouse boards
	 * @param companies the companies to probe, and where the probes are recorded
	 */
	@Autowired
	public BoardProbeRun(BoardReaderPort reader, CompanyStore companies) {
		this(AtsEnum.GREENHOUSE, reader, companies, Clock.systemUTC(), PACE);
	}

	/**
	 * @param ats the ATS whose boards are probed
	 * @param reader reads the boards of that ATS
	 * @param companies the companies to probe, and where the probes are recorded
	 * @param clock what a probe is timestamped from
	 * @param pace how long one company takes at most, counted from the start of its request
	 */
	BoardProbeRun(AtsEnum ats, BoardReaderPort reader, CompanyStore companies, Clock clock, Duration pace) {
		this.ats = ats;
		this.reader = reader;
		this.companies = companies;
		this.clock = clock;
		this.pace = pace;
	}

	/**
	 * Probes the board of every company held for this ATS, in slug order.
	 * @return what the run found
	 */
	public Report probeAll() {
		Set<String> slugs = this.companies.slugsOf(this.ats);
		int active = 0;
		int empty = 0;
		int notFound = 0;
		int unreadable = 0;
		long nextRequestAt = System.nanoTime();
		for (String slug : slugs) {
			waitUntil(nextRequestAt);
			nextRequestAt = System.nanoTime() + this.pace.toNanos();
			BoardReading reading = read(slug);
			if (reading == null) {
				unreadable++;
				continue;
			}
			this.companies.recordProbe(this.ats, slug, reading.boardStatus(), reading.name(),
					Instant.now(this.clock));
			switch (reading.boardStatus()) {
				case ACTIVE -> active++;
				case EMPTY -> empty++;
				case NOT_FOUND -> notFound++;
			}
		}
		return new Report(active, empty, notFound, unreadable);
	}

	/** What the board is, or null if the ATS could not be made to say. */
	private BoardReading read(String slug) {
		try {
			return this.reader.read(slug);
		}
		catch (IOException ex) {
			logger.warn("Could not read the %s board of %s".formatted(this.ats, slug), ex);
			return null;
		}
	}

	private void waitUntil(long nanoTime) {
		long remaining = nanoTime - System.nanoTime();
		if (remaining <= 0) {
			return;
		}
		try {
			Thread.sleep(Duration.ofNanos(remaining));
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while pacing a probe run", ex);
		}
	}

	/**
	 * What one run found. A company whose board could not be read keeps the board status it already
	 * had, including none.
	 *
	 * @param active how many boards had openings
	 * @param empty how many boards had none
	 * @param notFound how many slugs had no board
	 * @param unreadable how many boards the ATS would not say anything about
	 */
	public record Report(int active, int empty, int notFound, int unreadable) {
	}

}
