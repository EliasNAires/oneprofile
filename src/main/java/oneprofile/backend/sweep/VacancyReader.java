package oneprofile.backend.sweep;

import java.io.IOException;
import java.util.List;
import oneprofile.backend.vacancy.PublishedVacancy;

/** Reads the openings of one board from its ATS, so that a sweep can mirror them. */
public interface VacancyReader {

	/**
	 * Reads every opening a board publishes.
	 * @param slug the slug whose board to read
	 * @return what the board publishes, empty if it publishes nothing
	 * @throws IOException if the board could not be read, which says nothing about its openings
	 */
	List<PublishedVacancy> read(String slug) throws IOException;

}
