package oneprofile.backend.workers.sweep;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.VacancyStore;
import oneprofile.backend.workers.Pace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sweeps every board of one ATS that was last found to have openings, and mirrors each one.
 * <p>
 * Only the active boards are asked: the rest have already said they publish nothing. Requests are
 * paced at what each ATS serves without refusing, with a margin: several are open at once, up to a
 * limit, and they start no faster than a limit a second. Each board is mirrored as soon as it is
 * read. A board that could not be read is skipped whole: the company keeps the vacancies it already
 * had, so a board that went down is not mistaken for a board that emptied. Where its vacancies name
 * the company, as Workday's do, the company takes the name they give most often. A board the ATS
 * confirms is not found is mirrored as empty, then its company is recorded as NOT_FOUND without
 * changing its name or last probe time.
 */
@Component
public class BoardSweepRun {

	private static final Log logger = LogFactory.getLog(BoardSweepRun.class);

	/**
	 * Half of the rate each ATS's sweep served on 2026-10-22, which this machine's link capped before
	 * any ATS refused (#82, docs/measurements/2026-10-pace.md § The settings). Lever is paced per
	 * region, since each has an API of its own. A Workday board is many requests, each paced by the one
	 * throttle Workday's probes and sweeps share, 16 a second and 20 open ({@code WorkdayApiAdapter}),
	 * so its sweep reads up to 20 boards at once and lets that throttle hold it.
	 */
	private static final Map<AtsEnum, Pace> PACES = Map.of(AtsEnum.GREENHOUSE, new Pace(8, 10, false),
			AtsEnum.ASHBY, new Pace(8, 20, false), AtsEnum.LEVER, new Pace(8, 9, true), AtsEnum.WORKDAY,
			new Pace(20, 16, false));

	/** How often a running sweep reports its progress. */
	private static final Duration HEARTBEAT = Duration.ofMinutes(1);

	private final InstantSource clock;

	private final Duration heartbeat;

	private final Map<AtsEnum, VacancyReaderPort> readers;

	private final Map<AtsEnum, Pace> paces;

	private final CompanyStore companies;

	private final VacancyStore vacancies;

	/**
	 * Sweeps each ATS's boards at the pace it tolerates.
	 * @param greenhouse reads the openings of Greenhouse boards
	 * @param ashby reads the openings of Ashby boards
	 * @param lever reads the openings of Lever boards
	 * @param workday reads the openings of Workday boards
	 * @param companies where the boards to sweep are read from
	 * @param vacancies where what the boards publish is mirrored
	 */
	@Autowired
	public BoardSweepRun(GreenhouseVacancyReaderAdapter greenhouse, AshbyVacancyReaderAdapter ashby,
			LeverVacancyReaderAdapter lever, WorkdayVacancyReaderAdapter workday, CompanyStore companies,
			VacancyStore vacancies) {
		this(Map.of(AtsEnum.GREENHOUSE, greenhouse, AtsEnum.ASHBY, ashby, AtsEnum.LEVER, lever, AtsEnum.WORKDAY,
				workday), PACES, companies, vacancies, InstantSource.system(), HEARTBEAT);
	}

	/**
	 * @param readers what reads the openings of each ATS
	 * @param paces how fast each ATS is asked
	 * @param companies where the boards to sweep are read from
	 * @param vacancies where what the boards publish is mirrored
	 * @param clock supplies the run's wall time
	 * @param heartbeat how often progress is logged
	 */
	BoardSweepRun(Map<AtsEnum, VacancyReaderPort> readers, Map<AtsEnum, Pace> paces, CompanyStore companies,
			VacancyStore vacancies, InstantSource clock, Duration heartbeat) {
		this.clock = clock;
		this.heartbeat = heartbeat;
		this.readers = Map.copyOf(readers);
		this.paces = Map.copyOf(paces);
		this.companies = companies;
		this.vacancies = vacancies;
	}

	/**
	 * Sweeps the board of every company of one ATS whose board has openings, starting them in slug
	 * order.
	 * @param ats the ATS whose boards to sweep
	 * @param after the exclusive slug cursor, or null to start at the first board
	 * @param limit the maximum boards to read, or null for all remaining boards
	 * @return what the run found
	 */
	public Report sweep(AtsEnum ats, String after, Integer limit) {
		Instant started = this.clock.instant();
		VacancyReaderPort reader = this.readers.get(ats);
		TreeMap<String, RegionEnum> regions = new TreeMap<>();
		this.companies.regionsOf(ats, BoardStatusEnum.ACTIVE).entrySet().stream()
			.filter((board) -> after == null || board.getKey().compareTo(after) > 0)
			.limit(limit == null ? Long.MAX_VALUE : limit)
			.forEach((board) -> regions.put(board.getKey(), board.getValue()));
		ConcurrentSkipListSet<String> unfinished = new ConcurrentSkipListSet<>(regions.keySet());
		Map<String, Long> open = new ConcurrentHashMap<>();
		AtomicInteger published = new AtomicInteger();
		AtomicInteger added = new AtomicInteger();
		AtomicInteger updated = new AtomicInteger();
		AtomicInteger deleted = new AtomicInteger();
		AtomicInteger notFound = new AtomicInteger();
		AtomicInteger unreadable = new AtomicInteger();
		AtomicInteger openingsRead = new AtomicInteger();
		ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(
				Thread.ofPlatform().daemon().name("sweep-heartbeat").factory());
		try {
			timer.scheduleAtFixedRate(() -> heartbeat(ats, after, regions, unfinished, open, openingsRead),
					this.heartbeat.toNanos(), this.heartbeat.toNanos(), TimeUnit.NANOSECONDS);
			this.paces.get(ats).readEach("The %s sweep".formatted(ats), regions, (slug, region, onRefusal) -> {
				long before = System.nanoTime();
				synchronized (unfinished) {
					open.put(slug, before);
				}
				Reading reading = read(ats, reader, slug, region, onRefusal);
				long readMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
				if (reading instanceof Published board) {
					openingsRead.addAndGet(board.vacancies().size());
				}
				return new TimedReading(reading, readMs);
			}, (slug, timed) -> {
				Reading reading = timed.reading();
				long before = System.nanoTime();
				String outcome;
				int count = 0;
				if (reading instanceof Unreadable) {
					unreadable.incrementAndGet();
					outcome = "unreadable";
				}
				else if (reading instanceof NotFound) {
					VacancyStore.Reconciliation reconciliation = this.vacancies.mirror(ats, slug, List.of());
					// If the company write fails, it stays ACTIVE with no vacancies; the next sweep retries.
					this.companies.recordNotFound(ats, slug);
					deleted.addAndGet(reconciliation.deleted());
					notFound.incrementAndGet();
					outcome = "not_found";
				}
				else {
					List<PublishedVacancy> openings = ((Published) reading).vacancies();
					VacancyStore.Reconciliation reconciliation = this.vacancies.mirror(ats, slug, openings);
					companyName(openings).ifPresent((name) -> this.companies.recordName(ats, slug, name));
					published.addAndGet(openings.size());
					added.addAndGet(reconciliation.added());
					updated.addAndGet(reconciliation.updated());
					deleted.addAndGet(reconciliation.deleted());
					count = openings.size();
					outcome = "published";
				}
				long mirrorMs = reading instanceof Unreadable ? 0
						: TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
				logger.info("SWEEP_BOARD ats=%s slug=%s outcome=%s read_ms=%d mirror_ms=%d vacancies=%d"
					.formatted(ats.name(), slug, outcome, timed.readMs(), mirrorMs, count));
				synchronized (unfinished) {
					open.remove(slug);
					unfinished.remove(slug);
				}
			});
			long wallSeconds = Duration.between(started, this.clock.instant()).toSeconds();
			Report report = new Report(regions.size(), published.get(), added.get(), updated.get(), deleted.get(),
					notFound.get(), unreadable.get(), wallSeconds);
			logger.info(("SWEEP_REPORT ats=%s after=%s limit=%s boards=%d published=%d added=%d updated=%d"
					+ " deleted=%d not_found=%d unreadable=%d wall_s=%d").formatted(ats.name(),
						after == null ? "-" : after, limit == null ? "-" : limit, report.boards(), report.published(),
						report.added(), report.updated(), report.deleted(), report.notFound(), report.unreadable(),
						report.wallSeconds()));
			return report;
		}
		finally {
			timer.shutdownNow();
		}
	}

	/** Logs progress independently of blocked reader and store calls. */
	private void heartbeat(AtsEnum ats, String after, TreeMap<String, RegionEnum> regions,
			ConcurrentSkipListSet<String> unfinished, Map<String, Long> open, AtomicInteger vacancies) {
		synchronized (unfinished) {
			String first = unfinished.isEmpty() ? null : unfinished.first();
			String resume = first == null ? (regions.isEmpty() ? after : regions.lastKey()) : regions.lowerKey(first);
			if (resume == null) {
				resume = after;
			}
			Map.Entry<String, Long> oldest = open.entrySet().stream().min(Map.Entry.comparingByValue()).orElse(null);
			int finished = regions.size() - unfinished.size();
			logger.info(("SWEEP_HEARTBEAT ats=%s finished=%d open=%d left=%d vacancies=%d resume_after=%s"
					+ " longest_open=%s longest_open_s=%d").formatted(ats.name(), finished, open.size(),
						unfinished.size() - open.size(), vacancies.get(), resume == null ? "-" : resume,
						oldest == null ? "-" : oldest.getKey(), oldest == null ? 0
								: TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - oldest.getValue())));
		}
	}

	/** A board's reading and the elapsed time of its reader call. */
	private record TimedReading(Reading reading, long readMs) {
	}

	/**
	 * The name a board's vacancies give their company most often, where its ATS names the company on
	 * a vacancy: a tenant's vacancies can name several of its legal entities.
	 */
	private static Optional<String> companyName(List<PublishedVacancy> openings) {
		return openings.stream()
			.map(PublishedVacancy::companyName)
			.filter((name) -> name != null && !name.isBlank())
			.collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
			.entrySet()
			.stream()
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey);
	}

	/** What the board publishes, or that it was not found, or that it could not be read. */
	private static Reading read(AtsEnum ats, VacancyReaderPort reader, String slug, RegionEnum region,
			Runnable onRefusal) {
		try {
			return new Published(reader.read(slug, region, onRefusal));
		}
		catch (BoardNotFoundException ex) {
			return new NotFound();
		}
		catch (IOException ex) {
			logger.warn("Could not read the openings of the %s board of %s".formatted(ats, slug), ex);
			return new Unreadable();
		}
	}

	/** The three outcomes of asking an ATS for a board's vacancies. */
	private sealed interface Reading permits Published, NotFound, Unreadable {
	}

	/** Every vacancy the board publishes, none on an empty board. */
	private record Published(List<PublishedVacancy> vacancies) implements Reading {
	}

	/** The ATS confirmed that no board is published under the slug. */
	private record NotFound() implements Reading {
	}

	/** The ATS could not be made to say what the board publishes. */
	private record Unreadable() implements Reading {
	}

	/**
	 * What one run found. A board that was not found adds its held vacancies to deleted.
	 * A company whose board could not be read keeps every vacancy it already had, and is counted
	 * only in boards and unreadable.
	 *
	 * @param boards how many boards were asked for, including those not found or unreadable
	 * @param published how many openings those boards published
	 * @param added how many of them were not held yet
	 * @param updated how many held vacancies their boards still publish
	 * @param deleted how many held vacancies their boards no longer publish
	 * @param notFound how many slugs had no board
	 * @param unreadable how many boards the ATS would not say anything about
	 * @param wallSeconds the elapsed wall time of the run in seconds
	 */
	public record Report(int boards, int published, int added, int updated, int deleted, int notFound, int unreadable,
			long wallSeconds) {
	}

}
