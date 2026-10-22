package oneprofile.backend.workers.sweep;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.VacancyStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sweeps every board of one ATS that was last found to have openings, and mirrors each one.
 * <p>
 * Only the active boards are asked: the rest have already said they publish nothing. Requests are
 * paced at what each ATS tolerates. A board that could not be read is skipped whole: the company keeps
 * the vacancies it already had, so a board that went down is not mistaken for a board that emptied.
 */
@Component
public class BoardSweepRun {

	private static final Log logger = LogFactory.getLog(BoardSweepRun.class);

	/**
	 * How long one board takes at most on each ATS: each probe's pace, since neither refused a
	 * whole board's descriptions any sooner than a list of titles. Greenhouse served 9.3 whole boards
	 * a second four at a time on 2026-10-21 (#78), Ashby 16 on 2026-10-20 (#20). One after another,
	 * Greenhouse's answers take half a second on average, so its pace rarely holds a run back.
	 */
	private static final Map<AtsEnum, Duration> PACES = Map.of(AtsEnum.GREENHOUSE, Duration.ofMillis(250),
			AtsEnum.ASHBY, Duration.ofMillis(250));

	private final Map<AtsEnum, VacancyReaderPort> readers;

	private final Map<AtsEnum, Duration> paces;

	private final CompanyStore companies;

	private final VacancyStore vacancies;

	/**
	 * Sweeps each ATS's boards at the pace it tolerates.
	 * @param greenhouse reads the openings of Greenhouse boards
	 * @param ashby reads the openings of Ashby boards
	 * @param companies where the boards to sweep are read from
	 * @param vacancies where what the boards publish is mirrored
	 */
	@Autowired
	public BoardSweepRun(GreenhouseVacancyReaderAdapter greenhouse, AshbyVacancyReaderAdapter ashby,
			CompanyStore companies, VacancyStore vacancies) {
		this(Map.of(AtsEnum.GREENHOUSE, greenhouse, AtsEnum.ASHBY, ashby), PACES, companies, vacancies);
	}

	/**
	 * @param readers what reads the openings of each ATS
	 * @param paces how long one board of each ATS takes at most, counted from the start of its request
	 * @param companies where the boards to sweep are read from
	 * @param vacancies where what the boards publish is mirrored
	 */
	BoardSweepRun(Map<AtsEnum, VacancyReaderPort> readers, Map<AtsEnum, Duration> paces, CompanyStore companies,
			VacancyStore vacancies) {
		this.readers = Map.copyOf(readers);
		this.paces = Map.copyOf(paces);
		this.companies = companies;
		this.vacancies = vacancies;
	}

	/**
	 * Sweeps the board of every company of one ATS whose board has openings, in slug order.
	 * @param ats the ATS whose boards to sweep
	 * @return what the run found
	 */
	public Report sweepAll(AtsEnum ats) {
		VacancyReaderPort reader = this.readers.get(ats);
		Duration pace = this.paces.get(ats);
		Set<String> slugs = this.companies.slugsOf(ats, BoardStatusEnum.ACTIVE);
		int published = 0;
		int added = 0;
		int updated = 0;
		int deleted = 0;
		int unreadable = 0;
		long nextRequestAt = System.nanoTime();
		for (String slug : slugs) {
			waitUntil(nextRequestAt);
			nextRequestAt = System.nanoTime() + pace.toNanos();
			List<PublishedVacancy> openings = read(ats, reader, slug);
			if (openings == null) {
				unreadable++;
				continue;
			}
			VacancyStore.Reconciliation reconciliation = this.vacancies.mirror(ats, slug, openings);
			published += openings.size();
			added += reconciliation.added();
			updated += reconciliation.updated();
			deleted += reconciliation.deleted();
		}
		return new Report(slugs.size(), published, added, updated, deleted, unreadable);
	}

	/** What the board publishes, or null if the ATS could not be made to say. */
	private static List<PublishedVacancy> read(AtsEnum ats, VacancyReaderPort reader, String slug) {
		try {
			return reader.read(slug);
		}
		catch (IOException ex) {
			logger.warn("Could not read the openings of the %s board of %s".formatted(ats, slug), ex);
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
			throw new IllegalStateException("Interrupted while pacing a sweep run", ex);
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
