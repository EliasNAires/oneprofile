package oneprofile.backend.workers.sweep;

import java.io.IOException;
import java.util.List;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.vacancy.PublishedVacancy;

/** Reads the openings of one board from its ATS, so that a sweep can mirror them. */
public interface VacancyReaderPort {

	/**
	 * Reads every opening a board publishes.
	 * @param slug the slug whose board to read
	 * @param region the region its company holds, which picks the API host where an ATS has one per
	 * region
	 * @param onRefusal run each time the ATS refuses a request (429 or 503), retried or not, so that
	 * the run asking slows down
	 * @return what the board publishes, empty if it publishes nothing
	 * @throws IOException if the board could not be read, which says nothing about its openings
	 */
	List<PublishedVacancy> read(String slug, RegionEnum region, Runnable onRefusal) throws IOException;

}
