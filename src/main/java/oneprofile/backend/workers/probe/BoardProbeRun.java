package oneprofile.backend.workers.probe;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.SortedMap;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.company.RegionEnum;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Probes every board held for one ATS and records what it finds.
 * <p>
 * Requests are paced at what each ATS tolerates, because a run walks every company held and an ATS is under no obligation to
 * serve them all at once. The pace is counted from the start of one request to the start of the
 * next, so what it sets is how many companies a minute a run gets through, whatever the ATS takes
 * to answer. A board that could not be read at all is left as it was rather than recorded as
 * anything, so the next run probes it again.
 */
@Component
public class BoardProbeRun {

	private static final Log logger = LogFactory.getLog(BoardProbeRun.class);

	/**
	 * How long one company takes at most on each ATS: about the boards a second each served one
	 * after another without refusing one, a quarter or less of what each served four at a time.
	 * Greenhouse served 4.7 a second on 2026-10-21 (#78), Ashby four a second on 2026-10-20 (#20).
	 * Lever refused nothing on 2026-10-22 (#79) but takes about three seconds to answer, so its own
	 * latency, not this pace, sets how fast a run goes.
	 */
	private static final Map<AtsEnum, Duration> PACES = Map.of(AtsEnum.GREENHOUSE, Duration.ofMillis(250),
			AtsEnum.ASHBY, Duration.ofMillis(250), AtsEnum.LEVER, Duration.ofMillis(250));

	private final Map<AtsEnum, BoardReaderPort> readers;

	private final Map<AtsEnum, Duration> paces;

	private final CompanyStore companies;

	private final Clock clock;

	/**
	 * Probes each ATS's boards at the pace it tolerates.
	 * @param greenhouse reads Greenhouse boards
	 * @param ashby reads Ashby boards
	 * @param lever reads Lever boards
	 * @param companies the companies to probe, and where the probes are recorded
	 */
	@Autowired
	public BoardProbeRun(GreenhouseBoardReaderAdapter greenhouse, AshbyBoardReaderAdapter ashby,
			LeverBoardReaderAdapter lever, CompanyStore companies) {
		this(Map.of(AtsEnum.GREENHOUSE, greenhouse, AtsEnum.ASHBY, ashby, AtsEnum.LEVER, lever), PACES, companies,
				Clock.systemUTC());
	}

	/**
	 * @param readers what reads the boards of each ATS
	 * @param paces how long one company of each ATS takes at most, counted from the start of its
	 * request
	 * @param companies the companies to probe, and where the probes are recorded
	 * @param clock what a probe is timestamped from
	 */
	BoardProbeRun(Map<AtsEnum, BoardReaderPort> readers, Map<AtsEnum, Duration> paces, CompanyStore companies,
			Clock clock) {
		this.readers = Map.copyOf(readers);
		this.paces = Map.copyOf(paces);
		this.companies = companies;
		this.clock = clock;
	}

	/**
	 * Probes the board of every company held for one ATS, in slug order.
	 * @param ats the ATS whose boards to probe
	 * @return what the run found
	 */
	public Report probeAll(AtsEnum ats) {
		BoardReaderPort reader = this.readers.get(ats);
		Duration pace = this.paces.get(ats);
		SortedMap<String, RegionEnum> regions = this.companies.regionsOf(ats);
		int active = 0;
		int empty = 0;
		int notFound = 0;
		int unreadable = 0;
		long nextRequestAt = System.nanoTime();
		for (Map.Entry<String, RegionEnum> company : regions.entrySet()) {
			String slug = company.getKey();
			waitUntil(nextRequestAt);
			nextRequestAt = System.nanoTime() + pace.toNanos();
			BoardReading reading = read(ats, reader, slug, company.getValue());
			if (reading == null) {
				unreadable++;
				continue;
			}
			this.companies.recordProbe(ats, slug, reading.boardStatus(), reading.name(),
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
	private static BoardReading read(AtsEnum ats, BoardReaderPort reader, String slug,
			RegionEnum region) {
		try {
			return reader.read(slug, region);
		}
		catch (IOException ex) {
			logger.warn("Could not read the %s board of %s".formatted(ats, slug), ex);
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
