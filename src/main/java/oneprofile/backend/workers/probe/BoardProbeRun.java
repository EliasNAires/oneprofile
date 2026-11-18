package oneprofile.backend.workers.probe;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import oneprofile.backend.storage.boardlist.BoardListStore;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.vacancy.VacancyStore;
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
 * rather than recorded as anything, so the next run probes it again. A board found empty or not
 * found has its held vacancies removed before its probe is recorded. An active board keeps its
 * vacancies for the sweep to read. An active company holding no name is asked for it within the
 * paced read; a failed name request leaves the board active and is tried again on the next probe.
 * <p>
 * A board on the list of boards the sweep does not read ({@code boards/unread.tsv}) is not asked
 * for: its held vacancies are removed and it is recorded unread, so no later probe sets it back to
 * active. A board taken off the list is probed as usual on the next run.
 */
@Component
public class BoardProbeRun {

	private static final Log logger = LogFactory.getLog(BoardProbeRun.class);

	/**
	 * Half of what each ATS served without refusing one on 2026-10-22 (#82,
	 * docs/measurements/2026-10-pace.md § The settings). Lever holds 16 open at once, not half its 32,
	 * because its slow tail rather than its rate binds it, and it is paced per region, since each has an API of its own.
	 * Workday is asked at 16 a second, 20 at once, as decided on 2026-10-23
	 * (docs/measurements/2026-10-workday.md § The setting); a probe is one request a board, and its
	 * requests share one throttle with a Workday sweep's ({@code WorkdayApiAdapter}).
	 */
	private static final Map<AtsEnum, Pace> PACES = Map.of(AtsEnum.GREENHOUSE, new Pace(16, 60, false),
			AtsEnum.ASHBY, new Pace(16, 40, false), AtsEnum.LEVER, new Pace(16, 30, true), AtsEnum.WORKDAY,
			new Pace(20, 16, false));

	private final Map<AtsEnum, BoardReaderPort> readers;

	private final Map<AtsEnum, Pace> paces;

	private final CompanyStore companies;

	private final VacancyStore vacancies;

	private final BoardListStore boardList;

	private final Clock clock;

	/**
	 * Probes each ATS's boards at the pace it tolerates.
	 * @param greenhouse reads Greenhouse boards
	 * @param ashby reads Ashby boards
	 * @param lever reads Lever boards
	 * @param workday reads Workday boards
	 * @param companies the companies to probe, and where the probes are recorded
	 * @param vacancies where vacancies of boards found empty or not found are removed
	 * @param boardList the boards the sweep does not read
	 */
	@Autowired
	public BoardProbeRun(GreenhouseBoardReaderAdapter greenhouse, AshbyBoardReaderAdapter ashby,
			LeverBoardReaderAdapter lever, WorkdayBoardReaderAdapter workday, CompanyStore companies,
			VacancyStore vacancies, BoardListStore boardList) {
		this(Map.of(AtsEnum.GREENHOUSE, greenhouse, AtsEnum.ASHBY, ashby, AtsEnum.LEVER, lever, AtsEnum.WORKDAY,
				workday), PACES, companies, vacancies, boardList, Clock.systemUTC());
	}

	/**
	 * @param readers what reads the boards of each ATS
	 * @param paces how fast each ATS is asked
	 * @param companies the companies to probe, and where the probes are recorded
	 * @param vacancies where vacancies of boards found empty or not found are removed
	 * @param boardList the boards the sweep does not read
	 * @param clock what a probe is timestamped from
	 */
	BoardProbeRun(Map<AtsEnum, BoardReaderPort> readers, Map<AtsEnum, Pace> paces, CompanyStore companies,
			VacancyStore vacancies, BoardListStore boardList, Clock clock) {
		this.readers = Map.copyOf(readers);
		this.paces = Map.copyOf(paces);
		this.companies = companies;
		this.vacancies = vacancies;
		this.boardList = boardList;
		this.clock = clock;
	}

	/**
	 * Probes the board of every company held for one ATS, starting them in slug order.
	 * @param ats the ATS whose boards to probe
	 * @return what the run found
	 */
	public Report probeAll(AtsEnum ats) {
		Set<String> unnamedSlugs = this.companies.unnamedSlugsOf(ats);
		Set<String> unreadSlugs = this.boardList.unreadSlugsOf(ats);
		SortedMap<String, RegionEnum> toRead = new TreeMap<>(this.companies.regionsOf(ats));
		int unread = 0;
		for (String slug : unreadSlugs) {
			if (toRead.remove(slug) != null) {
				this.vacancies.mirror(ats, slug, List.of());
				this.companies.recordProbe(ats, slug, BoardStatusEnum.UNREAD, null, Instant.now(this.clock));
				unread++;
			}
		}
		BoardReaderPort reader = this.readers.get(ats);
		AtomicInteger active = new AtomicInteger();
		AtomicInteger empty = new AtomicInteger();
		AtomicInteger notFound = new AtomicInteger();
		AtomicInteger unreadable = new AtomicInteger();
		this.paces.get(ats)
			.readEach("The %s probe".formatted(ats), toRead,
					(slug, region, onRefusal) -> read(ats, reader, slug, region, onRefusal, unnamedSlugs), (slug, reading) -> {
						if (reading == null) {
							unreadable.incrementAndGet();
							return;
						}
						if (reading.boardStatus() == BoardStatusEnum.UNREAD) {
							throw new IllegalStateException("A board reader returned UNREAD for " + slug);
						}
						if (reading.boardStatus() == BoardStatusEnum.NOT_FOUND
								|| reading.boardStatus() == BoardStatusEnum.EMPTY) {
							this.vacancies.mirror(ats, slug, List.of());
						}
						// If the company write fails, its old status remains with no vacancies; the next run retries.
						this.companies.recordProbe(ats, slug, reading.boardStatus(), reading.name(),
								Instant.now(this.clock));
						switch (reading.boardStatus()) {
							case ACTIVE -> active.incrementAndGet();
							case EMPTY -> empty.incrementAndGet();
							case NOT_FOUND -> notFound.incrementAndGet();
							case UNREAD -> {
							}
						}
					});
		return new Report(active.get(), empty.get(), notFound.get(), unread, unreadable.get());
	}

	/** What the board is, or null if the ATS could not be made to say. */
	private static BoardReading read(AtsEnum ats, BoardReaderPort reader, String slug, RegionEnum region,
			Runnable onRefusal, Set<String> unnamedSlugs) {
		BoardReading reading;
		try {
			reading = reader.read(slug, region, onRefusal);
		}
		catch (IOException ex) {
			logger.warn("Could not read the %s board of %s".formatted(ats, slug), ex);
			return null;
		}
		if (reading.boardStatus() == BoardStatusEnum.ACTIVE && reading.name() == null && unnamedSlugs.contains(slug)) {
			try {
				return new BoardReading(reading.boardStatus(), reader.name(slug, region, onRefusal));
			}
			catch (IOException ex) {
				logger.warn("Could not read the %s name of %s".formatted(ats, slug), ex);
			}
		}
		return reading;
	}

	/**
	 * What one run found. A company whose board could not be read keeps the board status it already
	 * had, including none.
	 *
	 * @param active how many boards had openings
	 * @param empty how many boards had none
	 * @param notFound how many slugs had no board
	 * @param unread how many held boards were on the list the sweep does not read
	 * @param unreadable how many boards the ATS would not say anything about
	 */
	public record Report(int active, int empty, int notFound, int unread, int unreadable) {
	}

}
