package oneprofile.backend.storage.normalizedvacancy;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * One vacancy of the engineering subset the duplicate pass reads, without its description.
 * @param vacancyId the vacancy it was derived from
 * @param companyId the company whose board holds it
 * @param slug the company's slug shared across ATSs
 * @param title its raw title as published
 * @param locations its declared location strings with their countries, in publication order
 * @param workModes every work mode it offers, empty if none declared
 * @param firstPublishedAt when its ATS first published it, null where not given
 */
public record DuplicateVacancy(long vacancyId, long companyId, String slug, String title, List<Location> locations,
		Set<WorkModeEnum> workModes, Instant firstPublishedAt) {

	/** Keeps locations and modes independent of their source collections. */
	public DuplicateVacancy {
		locations = List.copyOf(locations);
		workModes = Set.copyOf(workModes);
	}

	/**
	 * One declared location string and the country declared alongside it.
	 * @param location the location string as published
	 * @param country its declared country, null where absent
	 */
	public record Location(String location, String country) {
	}

}
