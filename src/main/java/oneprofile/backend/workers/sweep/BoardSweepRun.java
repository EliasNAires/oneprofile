package oneprofile.backend.workers.sweep;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;
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
 * had, so a board that went down is not mistaken for a board that emptied.
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
				workday), PACES, companies, vacancies);
	}

	/**
	 * @param readers what reads the openings of each ATS
	 * @param paces how fast each ATS is asked
	 * @param companies where the boards to sweep are read from
	 * @param vacancies where what the boards publish is mirrored
	 */
	BoardSweepRun(Map<AtsEnum, VacancyReaderPort> readers, Map<AtsEnum, Pace> paces, CompanyStore companies,
			VacancyStore vacancies) {
		this.readers = Map.copyOf(readers);
		this.paces = Map.copyOf(paces);
		this.companies = companies;
		this.vacancies = vacancies;
	}

	/**
	 * Sweeps the board of every company of one ATS whose board has openings, starting them in slug
	 * order.
	 * @param ats the ATS whose boards to sweep
	 * @return what the run found
	 */
	public Report sweepAll(AtsEnum ats) {
		VacancyReaderPort reader = this.readers.get(ats);
		SortedMap<String, RegionEnum> regions = this.companies.regionsOf(ats, BoardStatusEnum.ACTIVE);
		AtomicInteger published = new AtomicInteger();
		AtomicInteger added = new AtomicInteger();
		AtomicInteger updated = new AtomicInteger();
		AtomicInteger deleted = new AtomicInteger();
		AtomicInteger unreadable = new AtomicInteger();
		this.paces.get(ats)
			.readEach("The %s sweep".formatted(ats), regions,
					(slug, region, onRefusal) -> read(ats, reader, slug, region, onRefusal), (slug, openings) -> {
						if (openings == null) {
							unreadable.incrementAndGet();
							return;
						}
						VacancyStore.Reconciliation reconciliation = this.vacancies.mirror(ats, slug, openings);
						published.addAndGet(openings.size());
						added.addAndGet(reconciliation.added());
						updated.addAndGet(reconciliation.updated());
						deleted.addAndGet(reconciliation.deleted());
					});
		return new Report(regions.size(), published.get(), added.get(), updated.get(), deleted.get(),
				unreadable.get());
	}

	/** What the board publishes, or null if the ATS could not be made to say. */
	private static List<PublishedVacancy> read(AtsEnum ats, VacancyReaderPort reader, String slug,
			RegionEnum region, Runnable onRefusal) {
		try {
			return reader.read(slug, region, onRefusal);
		}
		catch (IOException ex) {
			logger.warn("Could not read the openings of the %s board of %s".formatted(ats, slug), ex);
			return null;
		}
	}

	/**
	 * What one run found. A company whose board could not be read keeps every vacancy it already
	 * had, and is counted nowhere but in the boards that were unreadable.
	 *
	 * @param boards how many boards were swept
	 * @param published how many openings those boards published
	 * @param added how many of them were not held yet
	 * @param updated how many held vacancies their boards still publish
	 * @param deleted how many held vacancies their boards no longer publish
	 * @param unreadable how many boards the ATS would not say anything about
	 */
	public record Report(int boards, int published, int added, int updated, int deleted, int unreadable) {
	}

}
