package oneprofile.backend.sweep;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import oneprofile.backend.company.Ats;
import oneprofile.backend.company.BoardStatus;
import oneprofile.backend.company.Companies;
import oneprofile.backend.vacancy.PublishedVacancy;
import oneprofile.backend.vacancy.Vacancies;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Sweeps every board of one ATS that was last found to have openings, and mirrors each one.
 * <p>
 * Only the active boards are asked: the rest have already said they publish nothing. Requests are
 * paced, more slowly than a probe is, because an answer here carries every description of a board
 * rather than a list of titles. A board that could not be read is skipped whole: the company keeps
 * the vacancies it already had, so a board that went down is not mistaken for a board that emptied.
 */
public class BoardSweep {

	private static final Log logger = LogFactory.getLog(BoardSweep.class);

	private final Ats ats;

	private final VacancyReader reader;

	private final Companies companies;

	private final Vacancies vacancies;

	private final Duration pace;

	/**
	 * @param ats the ATS whose boards are swept
	 * @param reader reads the openings of that ATS
	 * @param companies where the boards to sweep are read from
	 * @param vacancies where what the boards publish is mirrored
	 * @param pace how long one board takes at most, counted from the start of its request
	 */
	public BoardSweep(Ats ats, VacancyReader reader, Companies companies, Vacancies vacancies, Duration pace) {
		this.ats = ats;
		this.reader = reader;
		this.companies = companies;
		this.vacancies = vacancies;
		this.pace = pace;
	}

	/**
	 * Sweeps the board of every company of this ATS whose board has openings, in slug order.
	 * @return what the run found
	 */
	public Report sweepAll() {
		Set<String> slugs = this.companies.slugsOf(this.ats, BoardStatus.ACTIVE);
		int published = 0;
		int added = 0;
		int updated = 0;
		int deleted = 0;
		int unreadable = 0;
		long nextRequestAt = System.nanoTime();
		for (String slug : slugs) {
			waitUntil(nextRequestAt);
			nextRequestAt = System.nanoTime() + this.pace.toNanos();
			List<PublishedVacancy> openings = read(slug);
			if (openings == null) {
				unreadable++;
				continue;
			}
			Vacancies.Reconciliation reconciliation = this.vacancies.mirror(this.ats, slug, openings);
			published += openings.size();
			added += reconciliation.added();
			updated += reconciliation.updated();
			deleted += reconciliation.deleted();
		}
		return new Report(slugs.size(), published, added, updated, deleted, unreadable);
	}

	/** What the board publishes, or null if the ATS could not be made to say. */
	private List<PublishedVacancy> read(String slug) {
		try {
			return this.reader.read(slug);
		}
		catch (IOException ex) {
			logger.warn("Could not read the openings of the %s board of %s".formatted(this.ats, slug), ex);
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
