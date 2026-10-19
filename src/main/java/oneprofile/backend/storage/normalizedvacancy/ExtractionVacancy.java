package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;

/**
 * One vacancy skill extraction reads: one the title decided in, or one of the pile in a language the
 * body pass reads (ADR-0017), with its title and its description as cleaning cut it.
 *
 * @param normalizedVacancyId the facts derived from it, which what extraction finds belongs to
 * @param vacancyId the vacancy it was derived from
 * @param cleanedTitle its title with what is noise in any title taken out
 * @param descriptionSegments its cleaned description cut into segments, null until cleaning has cut it
 */
public record ExtractionVacancy(long normalizedVacancyId, long vacancyId, String cleanedTitle,
		List<Segment> descriptionSegments) {

	/**
	 * Its segments, none if cleaning has not cut it.
	 * @return the segments of its description
	 */
	public List<Segment> segments() {
		return (this.descriptionSegments != null) ? this.descriptionSegments : List.of();
	}

}
