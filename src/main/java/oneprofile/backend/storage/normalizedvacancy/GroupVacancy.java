package oneprofile.backend.storage.normalizedvacancy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import oneprofile.backend.storage.company.AtsEnum;

/**
 * One vacancy of the engineering subset the vacancy-group pass reads, without its description.
 * @param vacancyId the vacancy it was derived from
 * @param companyId the company whose board holds it
 * @param ats the ATS that publishes its board
 * @param slug the board's slug
 * @param cleanedTitle its cleaned title
 * @param normalizedTitles its normalized titles, null before the title breakdown
 * @param managementLevel the rank its title names, null if none
 * @param titleSeniorities every level cleaning and the title breakdown read
 * @param locations its declared locations, in the order its ATS publishes them
 * @param workModes every work mode it offers, empty if none declared
 * @param firstPublishedAt when its ATS first published it, null where not given
 * @param groupId its stored vacancy group, null where none
 */
public record GroupVacancy(long vacancyId, long companyId, AtsEnum ats, String slug, String cleanedTitle,
		List<String> normalizedTitles, ManagementLevelEnum managementLevel, Set<SeniorityLevelEnum> titleSeniorities,
		List<String> locations, Set<WorkModeEnum> workModes, Instant firstPublishedAt, Long groupId) {

	/** Keeps the title evidence, locations and modes as values independent of their source collections. */
	public GroupVacancy {
		normalizedTitles = (normalizedTitles != null) ? List.copyOf(normalizedTitles) : null;
		titleSeniorities = Set.copyOf(titleSeniorities);
		locations = List.copyOf(locations);
		workModes = Set.copyOf(workModes);
	}

}
