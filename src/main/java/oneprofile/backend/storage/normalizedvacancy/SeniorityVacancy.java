package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;
import java.util.Set;

/**
 * One vacancy the seniority pass reads: one of the engineering subset, with the levels its title names and
 * its description's segments.
 *
 * @param vacancyId the vacancy it was derived from
 * @param titleSeniorities every level its title names: those cleaning read and those the title breakdown
 * read
 * @param segments its description's segments, empty where cleaning cut none
 */
public record SeniorityVacancy(long vacancyId, Set<SeniorityLevelEnum> titleSeniorities, List<Segment> segments) {

	public SeniorityVacancy {
		titleSeniorities = Set.copyOf(titleSeniorities);
		segments = List.copyOf(segments);
	}

}
