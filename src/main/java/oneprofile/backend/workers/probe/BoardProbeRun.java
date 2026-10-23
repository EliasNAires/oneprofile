package oneprofile.backend.workers.probe;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.workers.Pace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Probes every board held for one ATS and records what it finds.
 * <p>
 * Requests are paced at what each ATS serves without refusing, with a margin, because a run walks
 * every company held and an ATS is under no obligation to serve them all at once: several are open at
 * once, up to a limit, and they start no faster than a limit a second. Runs of different ATSs share
 * no limit, so they can go side by side. A board that could not be read at all is left as it was
 * rather than recorded as anything, so the next run probes it again.
 */
@Component
public class BoardProbeRun {

	private static final Log logger = LogFactory.getLog(BoardProbeRun.class);

	/**
	 * Half of what each ATS served without refusing one on 2026-10-22 (#82,
	 * docs/measurements/2026-10-pace.md § The settings). Lever holds 16 open at once, not half its 32,
	 * because its slow tail rather than its rate binds it, and it is paced per region, since each has an API of its own.
	 */
	private static final Map<AtsEnum, Pace> PACES = Map.of(AtsEnum.GREENHOUSE, new Pace(16, 60, false),
			AtsEnum.ASHBY, new Pace(16, 40, false), AtsEnum.LEVER, new Pace(16, 30, true));

	private final Map<AtsEnum, BoardReaderPort> readers;

	private final Map<AtsEnum, Pace> paces;

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
	 * @param paces how fast each ATS is asked
	 * @param companies the companies to probe, and where the probes are recorded
	 * @param clock what a probe is timestamped from
	 */
	BoardProbeRun(Map<AtsEnum, BoardReaderPort> readers, Map<AtsEnum, Pace> paces, CompanyStore companies,
			Clock clock) {
		this.readers = Map.copyOf(readers);
		this.paces = Map.copyOf(paces);
		this.companies = companies;
		this.clock = clock;
	}

	/**
	 * Probes the board of every company held for one ATS, starting them in slug order.
	 * @param ats the ATS whose boards to probe
	 * @return what the run found
	 */
	public Report probeAll(AtsEnum ats) {
		BoardReaderPort reader = this.readers.get(ats);
		AtomicInteger active = new AtomicInteger();
		AtomicInteger empty = new AtomicInteger();
		AtomicInteger notFound = new AtomicInteger();
		AtomicInteger unreadable = new AtomicInteger();
		this.paces.get(ats)
			.readEach("The %s probe".formatted(ats), this.companies.regionsOf(ats),
					(slug, region, onRefusal) -> read(ats, reader, slug, region, onRefusal), (slug, reading) -> {
						if (reading == null) {
							unreadable.incrementAndGet();
							return;
						}
						this.companies.recordProbe(ats, slug, reading.boardStatus(), reading.name(),
								Instant.now(this.clock));
						switch (reading.boardStatus()) {
							case ACTIVE -> active.incrementAndGet();
							case EMPTY -> empty.incrementAndGet();
							case NOT_FOUND -> notFound.incrementAndGet();
						}
					});
		return new Report(active.get(), empty.get(), notFound.get(), unreadable.get());
	}

	/** What the board is, or null if the ATS could not be made to say. */
	private static BoardReading read(AtsEnum ats, BoardReaderPort reader, String slug, RegionEnum region,
			Runnable onRefusal) {
		try {
			return reader.read(slug, region, onRefusal);
		}
		catch (IOException ex) {
			logger.warn("Could not read the %s board of %s".formatted(ats, slug), ex);
			return null;
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
