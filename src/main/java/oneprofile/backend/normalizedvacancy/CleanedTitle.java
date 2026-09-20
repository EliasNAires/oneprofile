package oneprofile.backend.normalizedvacancy;

import java.util.Set;

/**
 * A vacancy's title with what is noise in any title taken out, and every seniority level the title
 * named on its way there.
 * <p>
 * The levels are kept as a set rather than collapsed into one, because the title is one source of
 * seniority among several and the step that derives a single level has not run yet.
 *
 * @param title the title once cleaned, which is what classification reads
 * @param titleSeniorities every level the title named, empty if it named none
 */
public record CleanedTitle(String title, Set<SeniorityLevel> titleSeniorities) {

	public CleanedTitle {
		titleSeniorities = Set.copyOf(titleSeniorities);
	}

}
